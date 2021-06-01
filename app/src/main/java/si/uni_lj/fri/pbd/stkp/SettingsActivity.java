package si.uni_lj.fri.pbd.stkp;


import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;

public class SettingsActivity extends AppCompatActivity {

    OkHttpClient httpClient;
    String apiBaseUrl;
    String apiLatestDateEndpoint;
    String apiDownloadZipEndpoint;
    ProgressBar dateCheckLoader;
    ImageButton dateCheckBtn;
    TextView dateCheckText;
    String serverDate;
    Boolean filesPresentInternal;
    RelativeLayout deleteDownloadsLayout;
    String downloadsPath;

    int dateCheckState = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        downloadsPath = getFilesDir().getAbsolutePath() + File.separator + getResources().getString(R.string.download_directory);
        // ================ buttons setup ================
        ImageButton settings = findViewById(R.id.settings_trash_btn);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                simulateButtonClick(view);
                deleteSavedPages();
            }
        });

        ImageButton deleteDownloadsBtn = findViewById(R.id.settings_delete_downloads_btn);
        deleteDownloadsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                simulateButtonClick(view);
                deleteDownloads();
            }
        });

        dateCheckBtn = findViewById(R.id.settings_date_check_btn);
        dateCheckBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                simulateButtonClick(view);
                if (dateCheckState == 0) {
                    getServerLatestDate();
                } else if (dateCheckState == 2){
                    getNewServerFiles();
                }
            }
        });

        dateCheckLoader = findViewById(R.id.settings_date_check_loader);
        dateCheckText = findViewById(R.id.settings_date_check_text);
        // ================/ buttons setup ================
        filesPresentInternal = checkIfFilesPresentInternal();
        deleteDownloadsLayout = findViewById(R.id.settings_delete_downloads);
        if (!filesPresentInternal) {
            deleteDownloadsLayout.setVisibility(View.GONE);
        }



        // Get strings to create url's for backend API calls
        apiBaseUrl = getResources().getString(R.string.api_base_url);
        apiLatestDateEndpoint = getResources().getString(R.string.api_latest_date_endpoint);
        apiDownloadZipEndpoint = getResources().getString(R.string.api_download_zip_endpoint);

        httpClient = new OkHttpClient();
    }

    // Check if there are files downloaded from the backend saved in internal storage
    // Check /downloads
    private boolean checkIfFilesPresentInternal() {
        File[] downloads = new File(downloadsPath).listFiles();
        for (int i = 0; i < downloads.length; i++) {
            Log.d("settings", downloads[i].getName());
        }
        return downloads != null && downloads.length > 0;
    }


    private void getNewServerFiles() {
        dateCheckBtn.setVisibility(View.GONE);
        dateCheckLoader.setVisibility(View.VISIBLE);

        final Request downloadRequest = new Request.Builder().url(apiBaseUrl + apiDownloadZipEndpoint).build();
        Log.d("settings", "dateRequest , " + downloadRequest);

        httpClient.newCall(downloadRequest).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                SettingsActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), R.string.problem_server_download, Toast.LENGTH_SHORT).show();

                    }
                });
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String fileName = downloadsPath + File.separator + "latest_files.zip";
                // Create a directory for saved web pages if it does not exist yet
                new File(downloadsPath).mkdirs();
                File downloadFile = new File(fileName);
                BufferedSink sink = Okio.buffer(Okio.sink(downloadFile));
                sink.writeAll(response.body().source());
                sink.close();


                // TODO: check if you got the file, then delete the old ones

                Log.d("download", "unzipping");

                // Unzipping
                try {
                    FileInputStream fileInputStream = new FileInputStream(downloadFile);
                    ZipInputStream zipInputStream = new ZipInputStream(fileInputStream);
                    ZipEntry zipEntry;
                    while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                        Log.d("download", "unzipping: " + zipEntry.getName());

                        FileOutputStream fout = new FileOutputStream(downloadsPath + File.separator + zipEntry.getName());
                        BufferedOutputStream bufout = new BufferedOutputStream(fout);
                        byte[] buffer = new byte[1024];
                        int read = 0;
                        while ((read = zipInputStream.read(buffer)) != -1) {
                            bufout.write(buffer, 0, read);
                        }

                        zipInputStream.closeEntry();
                        bufout.close();
                        fout.close();


                    }
                    zipInputStream.close();
                    // Create a latest_date.txt in the storage and save the date we updated to.
                    Log.d("settings", "creating latest_date.txt");

                    FileOutputStream fout = new FileOutputStream(downloadsPath + File.separator + "latest_date.txt");
                    fout.write(serverDate.getBytes());
                    fout.close();
                    // delete the ZIP, we won't need it anymore
                    downloadFile.delete();
                    filesPresentInternal = true;

                } catch (Exception e) {
                    Log.d("download", "zip err: " + e.getMessage());
                }
                SettingsActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dateCheckLoader.setVisibility(View.GONE);
                        dateCheckBtn.setVisibility(View.VISIBLE);
                        dateCheckBtn.setImageResource(R.drawable.check_solid);
                        dateCheckBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#778BC34A")));
                        dateCheckText.setText(getResources().getText(R.string.files_up_to_date));
                        dateCheckState = 1;
                        deleteDownloadsLayout.setVisibility(View.VISIBLE);
                        Toast.makeText(getApplicationContext(), R.string.files_update_success, Toast.LENGTH_SHORT).show();

                    }
                });
            }
        });

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
            InputStream inputStream;
            if (filesPresentInternal) {
                File dateFile = new File(downloadsPath+ File.separator +"latest_date.txt");
                inputStream = new FileInputStream(dateFile);
            } else {
                inputStream = getAssets().open("latest_date.txt");
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
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
        serverDate = apiDate;
        dateCheckLoader.setVisibility(View.GONE);
        Log.d("settings", "serverDateOnCallback apiDate: " + apiDate);
        String localDate = getLocalLatestDate();
        if (localDate == null || apiDate == null) return;
        boolean upToDate = compareApiDateWithLocalDate(apiDate, localDate);
        dateCheckBtn.setVisibility(View.VISIBLE);
        if (upToDate) {
            dateCheckBtn.setImageResource(R.drawable.check_solid);
            dateCheckBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#778BC34A")));
            dateCheckText.setText(getResources().getText(R.string.files_up_to_date));
            dateCheckState = 1;
            Toast.makeText(this, "up to date bro!!", Toast.LENGTH_SHORT).show();

        } else {
            dateCheckBtn.setImageResource(R.drawable.cloud_download_alt_solid);
            dateCheckBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#BEC3A54A")));
            dateCheckText.setText(getResources().getText(R.string.files_possible_download));
            dateCheckState = 2;
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

    private void deleteDownloads() {
        int nDeleted = 0;
        File[] allFiles = new File(downloadsPath).listFiles();
        if (allFiles != null && allFiles.length > 0) {
            for (int i = 0; i < allFiles.length; i++) {
                allFiles[i].delete();
                nDeleted++;
            }
            Toast.makeText(this, getResources().getString(R.string.files_delete_success) + " " +nDeleted, Toast.LENGTH_LONG).show();
        }
        simulateFadeOut(deleteDownloadsLayout);
        filesPresentInternal = false;
        dateCheckState = 0;
        dateCheckBtn.setImageResource(R.drawable.question_solid);
        dateCheckBtn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#5C5C5C")));
        dateCheckText.setText(getResources().getText(R.string.check_for_new_files));
        deleteDownloadsLayout.setVisibility(View.GONE);
    }



    // =================== button click animation ===================
    private AlphaAnimation fadeIn = new AlphaAnimation(1F, 0.2F);
    private AlphaAnimation fadeOut = new AlphaAnimation(0.2f, 1F);
    private AlphaAnimation fadeOutSlow = new AlphaAnimation(0f, 1f);

    private void simulateButtonClick(View view) {
        fadeIn.setDuration(200);
        fadeOut.setDuration(200);
        view.startAnimation(fadeIn);
        view.startAnimation(fadeOut);
    }


    private void simulateFadeOut(View view) {
        fadeOut.setDuration(1000);
        view.startAnimation(fadeOutSlow);
    }
    // ===================/ button click animation ===================
}
