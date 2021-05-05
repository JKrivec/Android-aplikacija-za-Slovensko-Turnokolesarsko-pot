package si.uni_lj.fri.pbd.stkp;


import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuView;
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
        LinearLayout linearLayout;
        RelativeLayout expandable;


        public EtapaHolder(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.etapa_name);
            desc = itemView.findViewById(R.id.etapa_desc);
            category = itemView.findViewById(R.id.etapa_category);
            linearLayout = itemView.findViewById(R.id.linear_layout);
            expandable = itemView.findViewById(R.id.expandable);

            linearLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Etapa etapa = etapaList.get(getAbsoluteAdapterPosition());
                    etapa.setExpanded(!etapa.isExpanded());
                    notifyItemChanged(getAbsoluteAdapterPosition());
                    Log.d("list", "clicked?? bro pls");
                }
            });

        }
    }
}
