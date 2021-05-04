package si.uni_lj.fri.pbd.stkp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

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

                //Intent intent = new Intent(view.getContext(), MapsActivity.class);
                //view.getContext().startActivity(intent);

            }
        });
        // ================/ seznam etap onclick ================

        // ================ zemljevid onclick ================
        ImageView zemljevid = findViewById(R.id.zemljevidImageView);
        zemljevid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                simulateButtonClick(view);

                Intent intent = new Intent(view.getContext(), MapsActivity.class);
                String[] fileNamesToDraw = {"01.gpx", "02.gpx", "03.gpx", "KT.gpx"};
                intent.putExtra("fileNamesToDraw", fileNamesToDraw);
                view.getContext().startActivity(intent);

            }
        });
        // ================/ zemljevid onclick ================

    }


}
