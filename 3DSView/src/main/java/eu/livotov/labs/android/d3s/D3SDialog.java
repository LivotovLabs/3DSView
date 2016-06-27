package eu.livotov.labs.android.d3s;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ProgressBar;

/**
 * (c) Livotov Labs Ltd. 2012
 * Date: 26/09/2014
 */
public class D3SDialog extends DialogFragment implements D3SSViewAuthorizationListener
{

    private D3SView authenticator;
    private ProgressBar progressBar;
    private String acs, md, pareq, postback, stackedModePostbackUrl;

    private D3SDialogListener authorizationListener;

    private Handler handler;

    public static D3SDialog newInstance(final String acsUrl, final String md, final String paReq, D3SDialogListener listener)
    {
        return newInstance(acsUrl, md, paReq, null, listener);
    }

    public static D3SDialog newInstance(final String acsUrl, final String md, final String paReq, final String postbackUrl, D3SDialogListener listener)
    {
        D3SDialog dialog = new D3SDialog();

        dialog.acs = acsUrl;
        dialog.md = md;
        dialog.pareq = paReq;
        dialog.postback = postbackUrl;
        dialog.authorizationListener = listener;

        return dialog;
    }

    public static D3SDialog newInstance(final String acsUrl, final String md, final String paReq, final String acsPostbackUrl, final String stackedModePostbackUrl, D3SDialogListener listener)
    {
        D3SDialog dialog = new D3SDialog();

        dialog.acs = acsUrl;
        dialog.md = md;
        dialog.pareq = paReq;
        dialog.postback = acsPostbackUrl;
        dialog.stackedModePostbackUrl = stackedModePostbackUrl;
        dialog.authorizationListener = listener;

        return dialog;
    }

    public void showDialogAndAuthenticate(FragmentActivity activity)
    {
        if (activity.getCurrentFocus() != null)
        {
            activity.getCurrentFocus().clearFocus();
        }

        try
        {
            activity.getSupportFragmentManager().executePendingTransactions();
        }
        catch (Throwable err)
        {
            err.printStackTrace();
        }

        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
        ft.add(this, "d3sdialog");
        ft.commitAllowingStateLoss();
    }

    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState)
    {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setBackgroundDrawable(null);

        View v = inflater.inflate(R.layout.dialog_3ds, container, false);

        progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
        authenticator = (D3SView) v.findViewById(R.id.authenticator);
        authenticator.setAuthorizationListener(this);

        getDialog().setCanceledOnTouchOutside(false);
        getDialog().setCancelable(true);

        handler = new Handler();

        return v;
    }

    public void onViewCreated(final View view, final Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        if (!TextUtils.isEmpty(stackedModePostbackUrl))
        {
            authenticator.setStackedMode(stackedModePostbackUrl);
        }

        if (TextUtils.isEmpty(postback))
        {
            authenticator.authorize(acs, md, pareq);
        }
        else
        {
            authenticator.authorize(acs, md, pareq, postback);
        }
    }

    public void onAuthorizationCompleted(final String md, final String paRes)
    {
        handler.post(new Runnable()
        {
            public void run()
            {
                dismiss();
                if (authorizationListener != null)
                {
                    authorizationListener.onAuthorizationCompleted(md, paRes);
                }
            }
        });
    }

    public void onAuthorizationStarted(final D3SView view)
    {
        handler.post(new Runnable()
        {
            public void run()
            {
                progressBar.setVisibility(View.VISIBLE);
            }
        });

    }

    public void onAuthorizationWebPageLoadingProgressChanged(final int progress)
    {
        handler.post(new Runnable()
        {
            public void run()
            {
                progressBar.setVisibility(progress > 0 && progress < 100 ? View.VISIBLE : View.GONE);
            }
        });

    }

    public void onAuthorizationWebPageLoadingError(final int errorCode, final String description, final String failingUrl)
    {
        handler.post(new Runnable()
        {
            public void run()
            {
                dismiss();
                if (authorizationListener != null)
                {
                    authorizationListener.onAuthorizationFailed(errorCode, description, failingUrl);
                }
            }
        });
    }

    @Override
    public void onAuthorizationCompletedInStackedMode(final String finalizationUrl)
    {
        handler.post(new Runnable()
        {
            public void run()
            {
                dismiss();
                if (authorizationListener != null)
                {
                    authorizationListener.onAuthorizationCompletedInStackedMode(finalizationUrl);
                }
            }
        });
    }

    public interface D3SDialogListener
    {
        void onAuthorizationCompleted(final String md, final String paRes);

        void onAuthorizationFailed(final int code, final String message, final String failedUrl);

        void onAuthorizationCompletedInStackedMode(String finalizationUrl);
    }
}
