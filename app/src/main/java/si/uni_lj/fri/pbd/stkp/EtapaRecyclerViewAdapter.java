package si.uni_lj.fri.pbd.stkp;


import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EtapaRecyclerViewAdapter extends RecyclerView.Adapter<EtapaRecyclerViewAdapter.EtapaHolder> {

    List<Etapa> etapaList;

    public EtapaRecyclerViewAdapter(List<Etapa> etapaList) {
        this.etapaList = etapaList;
    }

    @NonNull
    @Override
    public EtapaHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.etapa, parent, false);
        return new EtapaHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EtapaHolder holder, int position) {
        Etapa etapa = this.etapaList.get(position);
        holder.name.setText(etapa.getName());
        holder.desc.setText(etapa.getDesc());
        holder.category.setText(etapa.getCategory());
        // Set visibility
        int visibility;
        if (etapa.isExpanded()) {
            visibility = View.VISIBLE;
        } else {
            visibility = View.GONE;
        }
        holder.expandable.setVisibility(visibility);

    }

    @Override
    public int getItemCount() {
        return this.etapaList.size();
    }

    public class EtapaHolder extends RecyclerView.ViewHolder {

        TextView name, desc, category;
        ImageView etapaMapBtn, etapaWebsiteBtn;
        LinearLayout linearLayout;
        RelativeLayout expandable;


        public EtapaHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.etapa_name);
            desc = itemView.findViewById(R.id.etapa_desc);
            category = itemView.findViewById(R.id.etapa_category);
            etapaMapBtn = itemView.findViewById(R.id.etapa_map);
            etapaWebsiteBtn = itemView.findViewById(R.id.etapa_website);
            linearLayout = itemView.findViewById(R.id.linear_layout);
            expandable = itemView.findViewById(R.id.expandable);

            linearLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Etapa etapa = etapaList.get(getAbsoluteAdapterPosition());
                    etapa.setExpanded(!etapa.isExpanded());
                    notifyItemChanged(getAbsoluteAdapterPosition());
                }
            });

            etapaMapBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    simulateButtonClick(v);
                    Etapa etapa = etapaList.get(getAbsoluteAdapterPosition());
                    Intent intent = new Intent(v.getContext(), MapsActivity.class);
                    String[] fileNamesToDraw = {etapa.getGpxFileName()};
                    intent.putExtra("fileNamesToDraw", fileNamesToDraw);
                    v.getContext().startActivity(intent);
                }
            });

            etapaWebsiteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    simulateButtonClick(v);
                    Etapa etapa = etapaList.get(getAbsoluteAdapterPosition());
                    Intent intent = new Intent(v.getContext(), WebViewActivity.class);
                    String url = etapa.getLink();
                    intent.putExtra("url", url);
                    v.getContext().startActivity(intent);
                }
            });
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
