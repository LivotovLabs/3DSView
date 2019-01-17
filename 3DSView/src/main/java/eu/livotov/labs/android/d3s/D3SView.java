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
public class D3SView extends WebView
{

    /**
     * Namespace for JS bridge
     */
    private static String JavaScriptNS = "D3SJS";

    /**
     * Pattern to find the MD value in the ACS server post response
     */
    private static Pattern mdFinder = Pattern.compile(".*?(<input[^<>]* name=\\\"MD\\\"[^<>]*>).*?", 32);

    /**
     * Pattern to find the PaRes value in the ACS server post response
     */
    private static Pattern paresFinder = Pattern.compile(".*?(<input[^<>]* name=\\\"PaRes\\\"[^<>]*>).*?", 32);

    private static Pattern valuePattern = Pattern.compile(".*? value=\\\"(.*?)\\\"", 32);

    /**
     * Internal flag for tracking web page url changes in WebView
     */
    private boolean urlReturned = false;

    /**
     * When set to <b>true</b>, SSL certificate errors and self-signed certificates errors will be ignored.
     */
    private boolean debugMode = false;

    /**
     * Url that will be used by ACS server for posting result data on authorization completion. We will be monitoring
     * this URL in WebView handler to intercept its loading and grabbing the resulting data from POST message instead.
     */
    private String postbackUrl = "https://www.google.com";

    /**
     * In simple mode we do not try to inject JS code and parse ACS server POST data, we just intercept postback url and call the simplified result listener instead. This is used
     * for some stacked providers where we need to pass extra provider's postback url to an acs server and then wait for it to complete.
     */
    private String stackedModePostbackUrl;

    private AtomicBoolean postbackHandled = new AtomicBoolean(false);

    /**
     * Callback to send authorization events to
     */
    private D3SSViewAuthorizationListener authorizationListener = null;


    public D3SView(final Context context)
    {
        super(context);
        initUI();
    }

    private void initUI()
    {
        getSettings().setJavaScriptEnabled(true);
        getSettings().setBuiltInZoomControls(true);
        addJavascriptInterface(new D3SJSInterface(), JavaScriptNS);

        setWebViewClient(new WebViewClient()
        {

            public boolean shouldOverrideUrlLoading(final WebView view, final String url)
            {
                final boolean stackedMode = !TextUtils.isEmpty(stackedModePostbackUrl);

                if (!postbackHandled.get() && (!stackedMode && url.toLowerCase().contains(postbackUrl.toLowerCase()) || (stackedMode
                        && url.toLowerCase().contains(stackedModePostbackUrl.toLowerCase()))))
                {
                    if (!TextUtils.isEmpty(stackedModePostbackUrl))
                    {
                        if (postbackHandled.compareAndSet(false, true)) {
                            authorizationListener.onAuthorizationCompletedInStackedMode(url);
                        }
                    }
                    else
                    {
                        view.loadUrl(String.format("javascript:window.%s.processHTML(document.getElementsByTagName('html')[0].innerHTML);", JavaScriptNS));
                    }
                    return true;
                }
                else
                {
                    return super.shouldOverrideUrlLoading(view, url);
                }
            }

            public void onPageStarted(WebView view, String url, Bitmap icon)
            {
                final boolean stackedMode = !TextUtils.isEmpty(stackedModePostbackUrl);

                if (!urlReturned && !postbackHandled.get())
                {
                    if ((!stackedMode && url.toLowerCase().contains(postbackUrl.toLowerCase())) || (stackedMode && url.toLowerCase().contains(stackedModePostbackUrl.toLowerCase())))
                    {
                        if (!TextUtils.isEmpty(stackedModePostbackUrl))
                        {
                            if (postbackHandled.compareAndSet(false, true)) {
                                authorizationListener.onAuthorizationCompletedInStackedMode(url);
                            }
                        }
                        else
                        {
                            view.loadUrl(String.format("javascript:window.%s.processHTML(document.getElementsByTagName('html')[0].innerHTML);", JavaScriptNS));
                        }
                        urlReturned = true;
                    }
                    else
                    {
                        super.onPageStarted(view, url, icon);
                    }
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (url.toLowerCase().contains(postbackUrl.toLowerCase())) {
                    return;
                }

                	view.loadUrl(String.format("javascript:window.%s.processHTML(document.getElementsByTagName('html')[0].innerHTML);", JavaScriptNS));
            }
;
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl)
            {
                if (!failingUrl.startsWith(postbackUrl))
                {
                    authorizationListener.onAuthorizationWebPageLoadingError(errorCode, description, failingUrl);
                }
            }

            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error)
            {
                if (debugMode)
                {
                    handler.proceed();
                }
            }


        });

        setWebChromeClient(new WebChromeClient()
        {

            public void onProgressChanged(WebView view, int newProgress)
            {
                if (authorizationListener != null)
                {
                    authorizationListener.onAuthorizationWebPageLoadingProgressChanged(newProgress);
                }
            }
        });
    }

    public D3SView(final Context context, final AttributeSet attrs)
    {
        super(context, attrs);
        initUI();
    }

    public D3SView(final Context context, final AttributeSet attrs, final int defStyle)
    {
        super(context, attrs, defStyle);
        initUI();
    }

    public D3SView(final Context context, final AttributeSet attrs, final int defStyle, final boolean privateBrowsing)
    {
        super(context, attrs, defStyle);
        initUI();
    }

    public void setStackedMode(String stackedModePostbackUrl)
    {
        this.stackedModePostbackUrl = stackedModePostbackUrl;
    }

    private void completeAuthorizationIfPossible(String html)
    {
    		// If the postback has already been handled, stop now
        if (postbackHandled.get()) {
            return;
        }

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
        if (paresValueMatcher.find()){
            pares = paresValueMatcher.group(1);
        } else {
    			return; // Not Found
        }
        
        // If we get to this point, we've definitely got values for both the MD and PaRes

        // The postbackHandled check is just to ensure we've not already called back. 
        // We don't want onAuthorizationCompleted to be called twice.
        if (postbackHandled.compareAndSet(false, true) && authorizationListener != null)
        {
            authorizationListener.onAuthorizationCompleted(md, pares);
        }
    }

    /**
     * Checks if debug mode is on. Note, that you must not turn debug mode for production app !
     *
     * @return
     */
    public boolean isDebugMode()
    {
        return debugMode;
    }

    /**
     * Sets the debug mode state. When set to <b>true</b>, ssl errors will be ignored. Do not turn debug mode ON
     * for production environment !
     *
     * @param debugMode
     */
    public void setDebugMode(final boolean debugMode)
    {
        this.debugMode = debugMode;
    }

    /**
     * Sets the callback to receive authorization events
     *
     * @param authorizationListener
     */
    public void setAuthorizationListener(final D3SSViewAuthorizationListener authorizationListener)
    {
        this.authorizationListener = authorizationListener;
    }

    /**
     * Starts 3DS authorization
     *
     * @param acsUrl ACS server url, returned by the credit card processing gateway
     * @param md     MD parameter, returned by the credit card processing gateway
     * @param paReq  PaReq parameter, returned by the credit card processing gateway
     */
    public void authorize(final String acsUrl, final String md, final String paReq)
    {
        authorize(acsUrl, md, paReq, null);
    }

    /**
     * Starts 3DS authorization
     *
     * @param acsUrl      ACS server url, returned by the credit card processing gateway
     * @param md          MD parameter, returned by the credit card processing gateway
     * @param paReq       PaReq parameter, returned by the credit card processing gateway
     * @param postbackUrl custom postback url for intercepting ACS server result posting. You may use any url you like
     *                    here, if you need, even non existing ones.
     */
    public void authorize(final String acsUrl, final String md, final String paReq, final String postbackUrl)
    {
        urlReturned = false;
        postbackHandled.set(false);

        if (authorizationListener != null)
        {
            authorizationListener.onAuthorizationStarted(this);
        }

        if (!TextUtils.isEmpty(postbackUrl))
        {
            this.postbackUrl = postbackUrl;
        }

        String postParams;
        try
        {
            postParams = String.format(Locale.US, "MD=%1$s&TermUrl=%2$s&PaReq=%3$s", URLEncoder.encode(md, "UTF-8"), URLEncoder.encode(this.postbackUrl, "UTF-8"), URLEncoder.encode(paReq, "UTF-8"));
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }

        postUrl(acsUrl, postParams.getBytes());
    }

    class D3SJSInterface
    {

        D3SJSInterface()
        {
        }

        @android.webkit.JavascriptInterface
        public void processHTML(final String paramString)
        {
            completeAuthorizationIfPossible(paramString);
        }
    }
}
