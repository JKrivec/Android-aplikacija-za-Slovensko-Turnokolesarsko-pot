package si.uni_lj.fri.pbd.stkp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

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


        // ================ seznam etap onclick ================
        ImageView seznamEtap = findViewById(R.id.etapeImageView);
            seznamEtap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                simulateButtonClick(view);

                Intent intent = new Intent(view.getContext(), ScrollingActivity.class);
                view.getContext().startActivity(intent);

            }
        });
        // ================/ seznam etap onclick ================

        // ================ zemljevid onclick ================
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
                //Log.d("files", "filenamestodraw: " + Arrays.toString(fileNamesToDraw));
                intent.putExtra("fileNamesToDraw", fileNamesToDraw);
                view.getContext().startActivity(intent);

            }
        });
        // ================/ zemljevid onclick ================//

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
        // ================/ settings onclick ================

    }

    private String[] getAllGpxFileNames() {
        ArrayList<String> gpxFilesArrayList = new ArrayList<String>();
        try {
            String[] allFiles = this.getAssets().list("");
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


}
