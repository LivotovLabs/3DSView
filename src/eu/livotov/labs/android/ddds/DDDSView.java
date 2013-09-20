package eu.livotov.labs.android.ddds;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.webkit.*;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * (c) Livotov Labs Ltd. 2012
 * Date: 20/09/2013
 *
 * <h1>Intro</h1>
 *
 * <p>This is the 3DSecure WebView component. It can be used to perform 3D-Secure authorizations when processing internet
 * payments in apps. The technology is also named Verified By Visa and MasterCard Secure Code.</p>
 *
 * <p>The main idea is to route cardholder to the card issuer financial institution where cardholder will be required to
 * answer extra security question or enter one-time sms or token code in order to confirm the card transaction.</p>
 *
 * <h1>How to use</h1>
 *
 * <p>Add DDDSView to your layout, set custom (if required) postback url and authorization results listener via the
 * corresponding setters. Then invoke the <b>authorize(...)</b> method to start 3D-Secure authorization. You will need to
 * provide payment data, which will be issued by your card processor in attempt to make a transaction with the 3DS-capable
 * credit card.</p>
 *
 * <p>Once user completes the authorization, the authorization listener's onAuthorizationCompleted() method will be called
 * with the parameters, came from banking ACS server. Now you can use those parameters in your bckend processing server
 * to finalize the payment.</p>
 */
public class DDDSView extends WebView
{

    /**
     * Pattern to find the MD value in the ACS server post response
     */
    private Pattern mdFinder = Pattern.compile(".*?<input.*name=\\\"MD\\\" value=\\\"(.*?)\\\".*?>", Pattern.DOTALL);

    /**
     * Pattern to find the PaRes value in the ACS server post response
     */
    private Pattern paresFinder = Pattern.compile(".*?<input.*name=\\\"PaRes\\\" value=\\\"(.*?)\\\".*?>", Pattern.DOTALL);

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
    private String postbackUrl = "https://ddds.postback.com";

    /**
     * Callback to send authorization events to
     */
    private DDDSViewAuthorizationListener authorizationListener = null;


    public DDDSView(final Context context)
    {
        super(context);
        initUI();
    }

    public DDDSView(final Context context, final AttributeSet attrs)
    {
        super(context, attrs);
        initUI();
    }

    public DDDSView(final Context context, final AttributeSet attrs, final int defStyle)
    {
        super(context, attrs, defStyle);
        initUI();
    }

    public DDDSView(final Context context, final AttributeSet attrs, final int defStyle, final boolean privateBrowsing)
    {
        super(context, attrs, defStyle, privateBrowsing);
        initUI();
    }

    private void initUI()
    {
        getSettings().setJavaScriptEnabled(true);
        setWebViewClient(new WebViewClient()
        {
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl)
            {
            }


            public void onPageStarted(WebView view, String url, Bitmap icon)
            {
                if (!urlReturned)
                {
                    if (url.startsWith(postbackUrl))
                    {
                        view.loadUrl("javascript:console.log('3DSEC'+document.getElementsByTagName('html')[0].innerHTML);");
                        urlReturned = true;
                    } else
                    {
                        super.onPageStarted(view, url, icon);
                    }
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
            public boolean onConsoleMessage(ConsoleMessage cmsg)
            {
                if (cmsg.message().startsWith("3DSEC"))
                {
                    String msg = cmsg.message().substring(4);
                    completeAuthorization(msg);
                    return true;
                }

                return false;
            }

            public void onProgressChanged(WebView view, int newProgress)
            {
                if (authorizationListener != null)
                {
                    authorizationListener.onAuthorizationWebPageLoadingProgressChanged(newProgress * 100);
                }
            }
        });
    }

    private void completeAuthorization(String html)
    {
        String md = "";
        String pares = "";

        Matcher mdMatcher = mdFinder.matcher(html);
        Matcher parseMatcher = paresFinder.matcher(html);

        if (mdMatcher.find())
        {
            md = mdMatcher.group(1);
        }

        if (parseMatcher.find())
        {
            pares = parseMatcher.group(1);
        }

        if (authorizationListener != null)
        {
            authorizationListener.onAuthorizationCompleted(md, pares);
        }
    }

    /**
     * Checks if debug mode is on. Note, that you must not turn debug mode for production app !
     * @return
     */
    public boolean isDebugMode()
    {
        return debugMode;
    }

    /**
     * Sets the debug mode state. When set to <b>true</b>, ssl errors will be ignored. Do not turn debug mode ON
     * for production environment !
     * @param debugMode
     */
    public void setDebugMode(final boolean debugMode)
    {
        this.debugMode = debugMode;
    }

    /**
     * Sets the callback to receive auhtorization events
     * @param authorizationListener
     */
    public void setAuthorizationListener(final DDDSViewAuthorizationListener authorizationListener)
    {
        this.authorizationListener = authorizationListener;
    }

    /**
     * Starts 3DS authorization
     * @param acsUrl ACS server url, returned by the credit card processing gateway
     * @param md MD parameter, returned by the credit card processing gateway
     * @param paReq PaReq parameter, returned by the credit card processing gateway
     */
    public void authorize(final String acsUrl, final String md, final String paReq)
    {
        authorize(acsUrl, md, paReq, null);
    }

    /**
     * Starts 3DS authorization
     * @param acsUrl ACS server url, returned by the credit card processing gateway
     * @param md MD parameter, returned by the credit card processing gateway
     * @param paReq PaReq parameter, returned by the credit card processing gateway
     * @param  postbackUrl custom postback url for intercepting ACS server result posting. You may use any url you like
     *                     here, if you need, even non existing ones.
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

}
