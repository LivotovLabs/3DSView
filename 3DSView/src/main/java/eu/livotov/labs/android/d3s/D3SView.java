package eu.livotov.labs.android.d3s;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceResponse;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * (c) Livotov Labs Ltd. 2013
 * Alex Askerov, Dmitri Livotov
 * <p/>
 * Date: 20/09/2013
 * <p/>
 * <h1>Intro</h1>
 * <p/>
 * <p>This is the 3DSecure WebView component. It can be used to perform 3D-Secure authorizations when processing internet
 * payments in apps. The technology is also named Verified By Visa and MasterCard Secure Code.</p>
 * <p/>
 * <p>The main idea is to route cardholder to the card issuer financial institution where cardholder will be required to
 * answer extra security question or enter one-time sms or token code in order to confirm the card transaction.</p>
 * <p/>
 * <h1>How to use</h1>
 * <p/>
 * <p>Add DDDSView to your layout, set custom (if required) postback url and authorization results listener via the
 * corresponding setters. Then invoke the <b>authorize(...)</b> method to start 3D-Secure authorization. You will need to
 * provide payment data, which will be issued by your card processor in attempt to make a transaction with the 3DS-capable
 * credit card.</p>
 * <p/>
 * <p>Once user completes the authorization, the authorization listener's onAuthorizationCompleted() method will be called
 * with the parameters, came from banking ACS server. Now you can use those parameters in your bckend processing server
 * to finalize the payment.</p>
 */
public class D3SView extends WebView {

    /**
     * Namespace for JS bridge
     */
    private static String JavaScriptNS = "D3SJS";

    /**
     * Patterns to find the various fields in the ACS server post response
     */
    private static Pattern mdFinder = Pattern.compile(".*?(<input[^<>]* name=\\\"MD\\\"[^<>]*>).*?", Pattern.DOTALL);
    private static Pattern paresFinder = Pattern.compile(".*?(<input[^<>]* name=\\\"PaRes\\\"[^<>]*>).*?", Pattern.DOTALL);
    private static Pattern cresFinder = Pattern.compile(".*?(<input[^<>]* name=\\\"CRes\\\"[^<>]*>).*?", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static Pattern threeDSSessionDataFinder = Pattern.compile(".*?(<input[^<>]* name=\\\"threeDSSessionData\\\"[^<>]*>).*?", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    /**
     * Pattern to find the value from the result of the above searches
     */
    private static final Pattern valuePattern = Pattern.compile(".*? value=\"(.*?)\"", Pattern.DOTALL);

    /**
     * Url that will be used by ACS server for posting result data on authorization completion. We will be monitoring
     * this URL in WebView handler to intercept its loading and grabbing the resulting data from POST message instead.
     */
    private String postbackUrl = "https://www.google.com";

    private AtomicBoolean postbackHandled = new AtomicBoolean(false);

    /**
     * 3-D Secure v2 (AKA Strong Customer Authentication).
     * This is an indicator as to whether the payment is 3-D Secure v1 or v2 is being performed, so the library has a
     * hint to know what field(s) to search for (and avoid unnecessary regular expressions)
     */
    private boolean is3dsV2;

    /**
     * Callback to send authorization events to
     */
    private D3SSViewAuthorizationListener authorizationListener = null;


    public D3SView(final Context context) {
        super(context);
        initUI();
    }

    private void initUI() {
        getSettings().setJavaScriptEnabled(true);
        getSettings().setBuiltInZoomControls(true);
        addJavascriptInterface(new D3SJSInterface(), JavaScriptNS);

        setWebViewClient(new WebViewClient() {

            @Override
            public WebResourceResponse shouldInterceptRequest (WebView view, String url) {
                if (isPostbackUrl(url)) {
                    // Wait for the form data to be processed in the other thread.
                    // 1.5s should be more than enough
                    //
                    // If for whatever reason the form data isn't captured successfully, this carries on and posts to
                    // the callback URL (AKA postback URL)
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }
                return null;
            }

            /*
             * In this lifecycle hook the HTML is available, although all resources (CSS, Images etc) may not be
             * We are merely processing the HTML though, so hooking in here is fine
             */
            @Override
            public void onPageCommitVisible(WebView view, String url) {

                if (!isPostbackUrl(url)) {
                    view.loadUrl(String.format("javascript:window.%s.processHTML(document.getElementsByTagName('html')[0].innerHTML);", JavaScriptNS));
                }

                super.onPageCommitVisible(view, url);
            }

            //
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if (!isPostbackUrl(failingUrl)) {
                    authorizationListener.onAuthorizationWebPageLoadingError(errorCode, description, failingUrl);
                }
            }

            private boolean isPostbackUrl(String url) {
                return url.toLowerCase().startsWith(postbackUrl.toLowerCase());
            }

        });

        setWebChromeClient(new WebChromeClient() {

            public void onProgressChanged(WebView view, int newProgress) {
                if (authorizationListener != null) {
                    authorizationListener.onAuthorizationWebPageLoadingProgressChanged(newProgress);
                }
            }
        });
    }

    public D3SView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        initUI();
    }

    public D3SView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        initUI();
    }

    public D3SView(final Context context, final AttributeSet attrs, final int defStyle, final boolean privateBrowsing) {
        super(context, attrs, defStyle);
        initUI();
    }

    private void completeAuthorizationIfPossible(final String html) {

        // Process HTML in a thread to improve performance
        Runnable runnable = () -> {
            // If the postback has already been handled, stop now
            if (postbackHandled.get()) {
                return;
            }

            if(is3dsV2){
                match3DSV2Parameters(html);
            } else {
                match3DSV1Parameters(html);
            }

        };
        Thread thread = new Thread(runnable);
        thread.start();

    }

    private void match3DSV2Parameters(String html) {

        // Try and find the CRes in the supplied html

        String cres = "";
        String threeDSSessionData = null;

        Matcher cresMatcher = cresFinder.matcher(html);
        if (cresMatcher.find()) {
            cres = cresMatcher.group(1);
        } else {
            return; // Not Found
        }

        Matcher cresValueMatcher = valuePattern.matcher(cres);
        if (cresValueMatcher.find()) {
            cres = cresValueMatcher.group(1);
        } else {
            return; // Not Found
        }

        Matcher threeDSSessionDataMatcher = threeDSSessionDataFinder.matcher(html);
        if (threeDSSessionDataMatcher.find()) {
            String fieldResult = threeDSSessionDataMatcher.group(1);
            if(fieldResult != null) {
                Matcher threeDSSessionDataValueMatcher = valuePattern.matcher(fieldResult);
                if (threeDSSessionDataValueMatcher.find()) {
                    threeDSSessionData = threeDSSessionDataValueMatcher.group(1);
                }
            }
        }

        if (postbackHandled.compareAndSet(false, true) && authorizationListener != null) {
            authorizationListener.onAuthorizationCompleted3dsV2(cres, threeDSSessionData);
        }
    }

    private void match3DSV1Parameters(String html) {

        // Try and find the MD and PaRes form elements in the supplied html
        String md = "";
        String pares = "";

        Matcher mdMatcher = mdFinder.matcher(html);
        if (mdMatcher.find()) {
            md = mdMatcher.group(1);
        } else {
            return; // Not Found
        }

        Matcher paresMatcher = paresFinder.matcher(html);
        if (paresMatcher.find()) {
            pares = paresMatcher.group(1);
        } else {
            return; // Not Found
        }

        // Now extract the values from the previously captured form elements
        Matcher mdValueMatcher = valuePattern.matcher(md);
        if (mdValueMatcher.find()) {
            md = mdValueMatcher.group(1);
        } else {
            return; // Not Found
        }

        Matcher paresValueMatcher = valuePattern.matcher(pares);
        if (paresValueMatcher.find()) {
            pares = paresValueMatcher.group(1);
        } else {
            return; // Not Found
        }

        // If we get to this point, we've definitely got values for both the MD and PaRes

        // The postbackHandled check is just to ensure we've not already called back.
        // We don't want onAuthorizationCompleted to be called twice.
        if (postbackHandled.compareAndSet(false, true) && authorizationListener != null) {
            authorizationListener.onAuthorizationCompleted(md, pares);
        }
    }

    /**
     * Sets the callback to receive authorization events
     *
     * @param authorizationListener
     */
    public void setAuthorizationListener(final D3SSViewAuthorizationListener authorizationListener) {
        this.authorizationListener = authorizationListener;
    }

    /**
     * Starts 3DS v1 authorization
     *
     * @param acsUrl ACS server url, returned by the credit card processing gateway
     * @param md     MD parameter, returned by the credit card processing gateway
     * @param paReq  PaReq parameter, returned by the credit card processing gateway
     */
    public void authorize(final String acsUrl, final String md, final String paReq) {
        authorize(acsUrl, null, md, paReq, null, null);
    }

    /**
     * Starts 3-D Secure v2 authentication
     * @param acsUrl ACS server url - supplied by the payment gateway
     * @param creq - CReq to post to the ACS
     * @param threeDSSessionData - Session data to pass to the ACS. This will be reflected back in the callback
     * @param postbackUrl - the URL to wait for, so the CRes can be extracted
     */
    public void authorize(final String acsUrl, final String creq, final String threeDSSessionData, final String postbackUrl) {
        authorize(acsUrl, creq, null, null,  threeDSSessionData, postbackUrl);
    }

    /**
     * Starts 3DS authorization
     *
     * @param acsUrl      ACS server url, returned by the credit card processing gateway
     * @param creq        CReq parameter (replaces MD and PaReq).
     * @param md          MD parameter, returned by the credit card processing gateway
     * @param paReq       PaReq parameter, returned by the credit card processing gateway
     * @param postbackUrl custom postback url for intercepting ACS server result posting. You may use any url you like
     *                    here, if you need, even non existing ones.
     */
    public void authorize(final String acsUrl, final String creq, final String md, final String paReq, final String threeDSSessionData, final String postbackUrl) {
        postbackHandled.set(false);

        if (authorizationListener != null) {
            authorizationListener.onAuthorizationStarted(this);
        }

        if (!TextUtils.isEmpty(postbackUrl)) {
            this.postbackUrl = postbackUrl;
        }

        String postParams;
        try {
            if(creq != null){
                // 3-D Secure v2
                is3dsV2 = true;
                postParams = String.format(Locale.US, "creq=%1$s&threeDSSessionData=%2$s", URLEncoder.encode(creq, "UTF-8"), URLEncoder.encode(threeDSSessionData, "UTF-8"));
            } else {
                // 3-D Secure v1
                postParams = String.format(Locale.US, "MD=%1$s&TermUrl=%2$s&PaReq=%3$s", URLEncoder.encode(md, "UTF-8"), URLEncoder.encode(this.postbackUrl, "UTF-8"), URLEncoder.encode(paReq, "UTF-8"));
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        postUrl(acsUrl, postParams.getBytes());
    }

    class D3SJSInterface {

        D3SJSInterface() {
        }

        @android.webkit.JavascriptInterface
        public void processHTML(final String html) {
            completeAuthorizationIfPossible(html);
        }
    }
}