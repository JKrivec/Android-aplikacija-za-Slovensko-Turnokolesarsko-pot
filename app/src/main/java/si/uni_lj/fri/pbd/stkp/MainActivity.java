package si.uni_lj.fri.pbd.stkp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {


    String downloadsPath;
    boolean drawInternal;

    /**
     * Define button animations
     */
    private AlphaAnimation fadeIn = new AlphaAnimation(1F, 0.2F);
    private AlphaAnimation fadeOut = new AlphaAnimation(0.2f, 1F);

    private void simulateButtonClick(View view) {
        fadeIn.setDuration(200);
        fadeOut.setDuration(200);
        view.startAnimation(fadeIn);
        view.startAnimation(fadeOut);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**
         * Set up button actions
         */
        // ================ etape onclick ================
        ImageView seznamEtap = findViewById(R.id.etapeImageView);
            seznamEtap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                simulateButtonClick(view);

                Intent intent = new Intent(view.getContext(), ScrollingActivity.class);
                drawInternal = checkIfFilesPresentInternal();
                intent.putExtra("drawInternal", drawInternal);
                view.getContext().startActivity(intent);

            }
        });

        // ================ map onclick ================
        ImageView zemljevid = findViewById(R.id.mapImageView);
        zemljevid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                simulateButtonClick(view);

                Intent intent = new Intent(view.getContext(), MapsActivity.class);
                String[] fileNamesToDraw = getAllGpxFileNames();
                if (fileNamesToDraw == null) {
                    return;
                }
                intent.putExtra("fileNamesToDraw", fileNamesToDraw);
                intent.putExtra("drawInternal", drawInternal);
                view.getContext().startActivity(intent);

            }
        });

        // ================ settings onclick ================
        ImageView settings = findViewById(R.id.settingsImageView);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                simulateButtonClick(view);
                Intent intent = new Intent(view.getContext(), SettingsActivity.class);
                view.getContext().startActivity(intent);
            }
        });

        // ================ web page onclick ================
        ImageView webpage = findViewById(R.id.spletnaStranImageView);
        webpage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                simulateButtonClick(view);
                Intent intent = new Intent(view.getContext(), WebViewActivity.class);
                intent.putExtra("url", getResources().getString(R.string.stkp_url));
                view.getContext().startActivity(intent);
            }
        });






        downloadsPath = getFilesDir().getAbsolutePath() + File.separator + getResources().getString(R.string.download_directory);
    }

    /**
     *
     * Return all the names of .gpx files
     * (internal storage if update was downloaded or assets otherwise)
     */
    private String[] getAllGpxFileNames() {
        ArrayList<String> gpxFilesArrayList = new ArrayList<>();
        try {
            String[] allFiles;
            drawInternal = checkIfFilesPresentInternal();
            if (drawInternal) {
                allFiles = new File(downloadsPath).list();
            } else {
                allFiles = this.getAssets().list("");
            }

            for (int i = 0; i < allFiles.length; i++) {
                if (allFiles[i].endsWith(".gpx")) {
                    gpxFilesArrayList.add(allFiles[i]);
                }
            }
            String[] gpxFiles = new String[gpxFilesArrayList.size()];
            for (int i = 0; i < gpxFilesArrayList.size(); i++) {
                gpxFiles[i] = gpxFilesArrayList.get(i);
            }
            return gpxFiles;
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(),"Napaka pri branju direktorija \"assets\"",Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return null;
        }

    }

    /**
     * Check if there are files downloaded from the backend saved in internal storage
     * Check /downloads directory
     */
    private boolean checkIfFilesPresentInternal() {
        File[] downloads = new File(downloadsPath).listFiles();
        if (downloads != null && downloads.length > 0) {
            return true;
        }

        return false;
    }
}
