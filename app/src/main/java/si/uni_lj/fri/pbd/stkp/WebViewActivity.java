package si.uni_lj.fri.pbd.stkp;

import android.net.http.*;

import android.os.Bundle;
import android.util.Log;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


public class WebViewActivity extends AppCompatActivity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // get view from layout
        setContentView(R.layout.webview);

        this.webView = (WebView) findViewById(R.id.webview);
        webView.setWebViewClient(new WebViewClient());
        // get url from the intent
        String url = getIntent().getStringExtra("url");
        Log.d("web", "hello? url is " + url);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.loadUrl(url);

        Log.d("web", "current url is " + webView.getUrl());
    }


    @Override
    public void onBackPressed() {
        if (this.webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
