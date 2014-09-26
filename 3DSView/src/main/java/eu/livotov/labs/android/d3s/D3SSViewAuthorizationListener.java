package eu.livotov.labs.android.d3s;

/**
 * (c) Livotov Labs Ltd. 2013
 * Alex Askerov, Dmitri Livotov
 *
 * Date: 20/09/2013
 * <p/>
 * Callback interface to receive authorization events
 */
public interface D3SSViewAuthorizationListener
{

    /**
     * Called when remote banking ACS server finishes 3DS authorization. Now you may pass the returned
     * MD and PaRes parameters to your credit card processing gateway for finalizing the transaction.
     *
     * @param md    MD parameter, sent by ACS server
     * @param paRes paRes parameter, sent by ACS server
     */
    void onAuthorizationCompleted(final String md, final String paRes);

    /**
     * Called when authorization process is started and web page from ACS server is being loaded.
     * For isntace, you may display progress now, etc...
     *
     * @param view reference for the DDDSView instance
     */
    void onAuthorizationStarted(D3SView view);

    /**
     * Called to update the ACS web page loading progress.
     *
     * @param progress current loading progress from 0 to 100.
     */
    void onAuthorizationWebPageLoadingProgressChanged(int progress);

    /**
     * Called if a loading error occurs
     * @param errorCode
     * @param description
     * @param failingUrl
     */
    void onAuthorizationWebPageLoadingError(int errorCode, String description, String failingUrl);
}
