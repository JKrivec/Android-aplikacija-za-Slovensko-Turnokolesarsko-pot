package si.uni_lj.fri.pbd.stkp;

import android.content.Context;
import android.os.Bundle;


import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ScrollingActivity extends AppCompatActivity {

    boolean drawInternal;
    String downloadsPath;
    RecyclerView recyclerView;
    List<Etapa> etapeList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // get view from layout
        setContentView(R.layout.activity_scrolling);
        recyclerView = findViewById(R.id.recyclerView);
        drawInternal = getIntent().getBooleanExtra("drawInternal", false);
        if (drawInternal) {
            Context appContext = getApplicationContext();
            downloadsPath = appContext.getFilesDir().getAbsolutePath() + File.separator + appContext.getResources().getString(R.string.download_directory);
        }
        fillEtapeList();
        setRecyclerView();

    }

    private void setRecyclerView() {
        EtapaRecyclerViewAdapter etapaAdapter = new EtapaRecyclerViewAdapter(this.etapeList);
        this.recyclerView.setAdapter(etapaAdapter);
        this.recyclerView.setHasFixedSize(false);
    }

    private void fillEtapeList() {
        String json = null;
        this.etapeList = new ArrayList<Etapa>();
        try {
            InputStream inputStream;
            if (drawInternal) {
                inputStream = new FileInputStream(downloadsPath + File.separator + "etape.json");
            } else {
                inputStream = getAssets().open("etape.json");
            }
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            json = new String(buffer, "UTF-8");
            JSONArray jsonArr = new JSONArray(json);

            for (int i = 0; i < jsonArr.length(); i++) {
                JSONObject jsonEl = jsonArr.getJSONObject(i);
                Etapa etapa = new Etapa(jsonEl.getString("name"), jsonEl.getString("desc"), jsonEl.getString("href"), jsonEl.getString("category"), jsonEl.getString("file"));
                this.etapeList.add(etapa);
            }

        } catch (IOException | JSONException ex) {
            Toast.makeText(this, "Problem pri branju podatkov etap iz .json datoteke.", Toast.LENGTH_LONG).show();
            ex.printStackTrace();
        }
    }

}
