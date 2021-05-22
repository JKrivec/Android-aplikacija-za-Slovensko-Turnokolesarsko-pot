package si.uni_lj.fri.pbd.stkp;

import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class SettingsActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);


        // ================ trash onclick ================
        ImageView settings = findViewById(R.id.settings_trash_btn);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                simulateButtonClick(view);
                deleteSavedPages();
            }
        });
        // ================/ trash onclick ================

    }


    private void deleteSavedPages() {                               // + File.separator + getResources().getString(R.string.saved_page_directory
        int nDeleted = 0;

        String x = getFilesDir().getAbsolutePath();
        File[] y = new File(x).listFiles();
        if (y != null && y.length > 0) {
            for (int i = 0; i < y.length; i++) {
                if (y[i].isDirectory()) {
                    Log.d("xfile", y[i].getName());
                }
            }
        }

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
