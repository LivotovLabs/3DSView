package eu.livotov.labs.android.d3s;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;


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
     * Internal flag for trasking web page url changes in WebView
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
    private String postbackUrl = "https://d3s.postback.com";

    /**
     * Callback to send authorization events to
     */
    private D3SSViewAuthorizationListener authorizationListener = null;


    public D3SView(final Context context)
    {
        super(context);
        initUI();
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

    private void initUI()
    {
        getSettings().setJavaScriptEnabled(true);
        getSettings().setBuiltInZoomControls(true);
        addJavascriptInterface(new D3SJSInterface(), JavaScriptNS);

        setWebViewClient(new WebViewClient()
        {

            public void onPageStarted(WebView view, String url, Bitmap icon)
            {
                if (!urlReturned)
                {
                    if (url.startsWith(postbackUrl))
                    {
                        view.loadUrl(String.format("javascript:window.%s.processHTML(document.getElementsByTagName('html')[0].innerHTML);", JavaScriptNS));
                        urlReturned = true;
                    } else
                    {
                        super.onPageStarted(view, url, icon);
                    }
                }
            }
            
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl)
            {
                if(!failingUrl.startsWith(postbackUrl)) {
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

    private void completeAuthorization(String html)
    {
        String md = "";
        String pares = "";

        Matcher localMatcher1 = mdFinder.matcher(html);
        Matcher localMatcher2 = paresFinder.matcher(html);

        if (localMatcher1.find())
        {
            md = localMatcher1.group(1);
        }

        if (localMatcher2.find())
        {
            pares = localMatcher2.group(1);
        }

        if (!TextUtils.isEmpty(md))
        {
            Matcher valueMatcher = valuePattern.matcher(md);
            if (valueMatcher.find())
            {
                md = valueMatcher.group(1);
            }
        }

        if (!TextUtils.isEmpty(pares))
        {
            Matcher valueMatcher = valuePattern.matcher(pares);
            if (valueMatcher.find())
            {
                pares = valueMatcher.group(1);
            }
        }

        if (authorizationListener != null)
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
     * Sets the callback to receive auhtorization events
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
        if (authorizationListener != null)
        {
            authorizationListener.onAuthorizationStarted(this);
        }

        if (!TextUtils.isEmpty(postbackUrl))
        {
            this.postbackUrl = postbackUrl;
        }

        urlReturned = false;

        List<NameValuePair> params = new LinkedList<NameValuePair>();

        params.add(new BasicNameValuePair("MD", md));
        params.add(new BasicNameValuePair("TermUrl", this.postbackUrl));
        params.add(new BasicNameValuePair("PaReq", paReq));

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try
        {
            new UrlEncodedFormEntity(params, HTTP.UTF_8).writeTo(bos);
        } catch (IOException e)
        {
        }

        postUrl(acsUrl, bos.toByteArray());
    }

    class D3SJSInterface
    {

        D3SJSInterface()
        {
        }

        @android.webkit.JavascriptInterface
        public void processHTML(final String paramString)
        {
            completeAuthorization(paramString);
        }
    }
}
