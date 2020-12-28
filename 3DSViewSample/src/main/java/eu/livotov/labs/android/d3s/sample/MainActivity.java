package eu.livotov.labs.android.d3s.sample;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import eu.livotov.labs.android.d3s.D3SSViewAuthorizationListener;
import eu.livotov.labs.android.d3s.D3SView;
import eu.livotov.labs.android.d3s.sample.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        // Set a listener that defines what you wish to happen upon certain callbacks.
        binding.d3sView.setAuthorizationListener(new D3SSViewAuthorizationListener() {
            @Override
            public void onAuthorizationCompleted(String md, String paRes) {
                Log.i("D3SSViewAuthorizationListener", "Authorization completed.");
            }

            @Override
            public void onAuthorizationCompleted3dsV2(String cres, String threeDSSessionData) {
                Log.i("D3SSViewAuthorizationListener", "Authorization completed 3dsV2.");
            }

            @Override
            public void onAuthorizationStarted(D3SView view) {
                Log.i("D3SSViewAuthorizationListener", "Authorization started.");
            }

            @Override
            public void onAuthorizationWebPageLoadingProgressChanged(int progress) {
                Log.i("D3SSViewAuthorizationListener", String.format("Web page loading progress: %d.", progress));
            }

            @Override
            public void onAuthorizationWebPageLoadingError(int errorCode, String description, String failingUrl) {
                Log.e("D3SSViewAuthorizationListener", "Web page loading error.");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Get your parameters from your backend and then call authorize to begin.
        binding.d3sView.authorize("acsUrl", "md", "paReq");
    }
}