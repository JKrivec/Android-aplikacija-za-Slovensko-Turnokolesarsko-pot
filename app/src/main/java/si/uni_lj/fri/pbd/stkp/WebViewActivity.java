package si.uni_lj.fri.pbd.stkp;

import android.content.res.ColorStateList;
import android.graphics.Color;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;


public class WebViewActivity extends AppCompatActivity {
    private WebView webView;
    private ImageButton saveButton;
    private ImageButton loadButton;
    private String currentUrl;
    private boolean displayingSavedPage = false;
    private boolean displayingSavedPageOriginalUrl = false;
    private boolean displayingSavedPageBack = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // get view from layout
        setContentView(R.layout.webview);
        // Lock marker btn
        saveButton = findViewById(si.uni_lj.fri.pbd.stkp.R.id.save_webview_btn);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                simulateButtonClick(v);



                String filename = fileNameFromUrl(webView.getUrl());
                if (checkIfUrlIsFile(webView.getUrl())) {
                    filename = webView.getUrl().split("file://")[1];
                }

                // Check if page is already saved
                File existCheckFile = new File(filename);
                if (existCheckFile.exists()) {
                    // delete and remove the tint
                    existCheckFile.delete();
                    v.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#17000000")));
                    Toast.makeText(getApplicationContext(), R.string.page_delete_success, Toast.LENGTH_LONG).show();
                } else {
                    // Create a directory for saved web pages if it does not exist yet
                    new File(filename.substring(0,filename.lastIndexOf("/"))).mkdirs();
                    webView.saveWebArchive(filename);
                    Log.d("xfile", "saved: " + filename);

                    saveButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#778BC34A")));
                    Toast.makeText(getApplicationContext(), R.string.page_save_success, Toast.LENGTH_LONG).show();
                }
            }
        });

        this.webView = (WebView) findViewById(R.id.webview);
        webView.setWebViewClient(new WebViewClient() {
            // For compatibility reasons, both 2 override both
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return loadUrl(url);
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                return loadUrl(url);
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d("xfile", "url loaded: " + url);
            }
        });
        // get url from the intent and try to load it
        String url = getIntent().getStringExtra("url");
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        loadUrl(url);

    }

    // Checks if the url user navigated to can be converted into the file that is saved
    //
    private boolean loadUrl(String url){

        // Check if the "url" is actually a saved file
            Log.d("xfile", "webviewUrl: " + webView.getUrl());
            Log.d("xfile", "url to be: " + url);
        if (url.startsWith("cid")) {
            Log.d("xfile", "displayingSavedPage yessir");
            saveButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#778BC34A")));
            return false;
        } else {
            saveButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#17000000")));

        }




        // If "url" is an actual web address (and not pointing to a saved file),
        // check if it's saved.
        if (!checkIfUrlIsFile(url) && checkIfUrlSaved(url)) {
            Log.d("xfile", "opening saved");

            // Colour the save button green
            saveButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#778BC34A")));
            //File file = new File(fileNameFromUrl(url));
            // Load saved web page into the webView
            webView.loadUrl("file://"+fileNameFromUrl(url));
            displayingSavedPage = true;
            // Override url loading of the original page because we will load the saved one
            return true;
        } else {
            webView.loadUrl(url);
            saveButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#17000000")));
            return false;
        }
    }
    
    private String fileNameFromUrl(String url){
        String base = getFilesDir().getAbsolutePath() + File.separator +getResources().getString(R.string.saved_page_directory) + File.separator;
        return base + url.replaceAll("[.,*&\\/:=?]", "_") + ".mhtml";
    }

    private boolean checkIfUrlIsFile(String url) {
        return url.startsWith("file://");
    }

    private boolean checkIfUrlSaved(String url) {
        return new File(fileNameFromUrl(url)).exists();
    }



    // Prevent going back to the last activity if user navigates
    // the webView further than the original page
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    // =================== button click animation ===================
    private AlphaAnimation fadeIn = new AlphaAnimation(1F, 0.2F);
    private AlphaAnimation fadeOut = new AlphaAnimation(0.2f, 1F);

    private void simulateButtonClick(View view) {
        fadeIn.setDuration(200);
        fadeOut.setDuration(200);
        view.startAnimation(fadeIn);
        view.startAnimation(fadeOut);
    }
    // ===================/ button click animation ===================
}
