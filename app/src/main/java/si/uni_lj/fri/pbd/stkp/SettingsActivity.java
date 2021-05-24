package si.uni_lj.fri.pbd.stkp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SettingsActivity extends AppCompatActivity {

    OkHttpClient httpClient;
    String apiBaseUrl;
    String apiLatestDateEndpoint;
    String apiDownloadZipEndpoint;
    ProgressBar dateCheckLoader;
    ImageButton dateCheckBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // ================ buttons setup ================
        ImageView settings = findViewById(R.id.settings_trash_btn);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                simulateButtonClick(view);
                deleteSavedPages();
            }
        });

        dateCheckBtn = findViewById(R.id.settings_date_check_btn);
        dateCheckBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                simulateButtonClick(view);
                getServerLatestDate();
            }
        });

        dateCheckLoader = findViewById(R.id.settings_date_check_loader);
        // ================/ buttons setup ================




        // Get strings to create url's for backend API calls
        apiBaseUrl = getResources().getString(R.string.api_base_url);
        apiLatestDateEndpoint = getResources().getString(R.string.api_latest_date_endpoint);
        apiDownloadZipEndpoint = getResources().getString(R.string.api_download_zip_endpoint);

        httpClient = new OkHttpClient();

    }

    private String getDateFromJsonResponse(String response) {
        String date = null;
        try {
            JSONObject jsonResponse = new JSONObject(response);
            date = (String) jsonResponse.get("date");
            if (date.equals("null")) {
                date = null;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return date;
    }

    private String getLocalLatestDate() {
        String localDate = null;
        try {
            InputStream dateFileStream = getAssets().open("latest_date.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(dateFileStream));
            localDate = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.something_went_wrong, Toast.LENGTH_SHORT).show();
        }
        if (localDate == null) {
            Toast.makeText(this, R.string.something_went_wrong, Toast.LENGTH_SHORT).show();
        }
        return localDate;
    }

    private void serverDateOnCallback(String apiDate) {
        apiDate = getDateFromJsonResponse(apiDate);
        dateCheckLoader.setVisibility(View.GONE);
        Log.d("settings", "serverDateOnCallback apiDate: " + apiDate);
        String localDate = getLocalLatestDate();
        if (localDate == null || apiDate == null) return;
        boolean upToDate = compareApiDateWithLocalDate(apiDate, localDate);
        if (upToDate) {
            dateCheckBtn.setVisibility(View.VISIBLE);
            Toast.makeText(this, "up to date bro!!", Toast.LENGTH_SHORT).show();

        } else {
            Log.d("settings", "serverDateOnCallback: not up to date lolz");
        }

    }
    // Returns true if up to date, false if not
    private boolean compareApiDateWithLocalDate(String apiDate, String localDate) {
        return createIntFromDateString(apiDate) <= createIntFromDateString(localDate);

    }

    private int createIntFromDateString(String date) {
        String[] splitDate = date.split("-");
        int returnVal = Integer.parseInt(splitDate[2]) * 10000 + Integer.parseInt(splitDate[1]) * 100 + Integer.parseInt(splitDate[2]);
        Log.d("settings", "Date integer: " + String.valueOf(returnVal));
        return returnVal;
    }


    private void getServerLatestDate(){
        dateCheckBtn.setVisibility(View.GONE);
        dateCheckLoader.setVisibility(View.VISIBLE);

        final Request dateRequest = new Request.Builder().url(apiBaseUrl + apiLatestDateEndpoint).build();
        Log.d("settings", "dateRequest , " + dateRequest);

        httpClient.newCall(dateRequest).enqueue(new Callback() {
             @Override
             public void onFailure(@NotNull Call call, @NotNull IOException e) {
                 Log.d("settings", "onFailure: " + e.getMessage());
                 e.printStackTrace();
                 SettingsActivity.this.runOnUiThread(new Runnable() {
                     @Override
                     public void run() {
                         Toast.makeText(getApplicationContext(), R.string.problem_server_connection, Toast.LENGTH_SHORT).show();

                     }
                 });
             }

             @Override
             public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                 Log.d("settings", "onResponse, " + response);
                if (response.isSuccessful()) {
                    final String responseString = response.body().string();
                    SettingsActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            serverDateOnCallback(responseString);

                        }
                    });
                }
             }
         });
    }


    private void deleteSavedPages() {
        int nDeleted = 0;
        String savedPagesDirPath = getFilesDir().getAbsolutePath() + File.separator + getResources().getString(R.string.saved_page_directory);
        File[] allFiles = new File(savedPagesDirPath).listFiles();

        if (allFiles != null && allFiles.length > 0) {
            for (int i = 0; i < allFiles.length; i++) {
                allFiles[i].delete();
                nDeleted++;
            }
            Toast.makeText(this, getResources().getString(R.string.files_delete_success) + " " +nDeleted, Toast.LENGTH_LONG).show();

        } else {
            Toast.makeText(this,getResources().getString(R.string.no_files), Toast.LENGTH_LONG).show();
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
