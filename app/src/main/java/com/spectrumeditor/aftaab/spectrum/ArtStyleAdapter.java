package com.spectrumeditor.aftaab.spectrum;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ArtStyleAdapter extends RecyclerView.Adapter<ArtStyleAdapter.ArtListViewHolder>{


    public static class ArtListViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        // each data item is just a string in this case
        TextView styleName;
        ImageView styleImage, styleStatus;
        ItemSelectListener itemSelectListener;

        public ArtListViewHolder(@NonNull View itemView, ItemSelectListener itemSelectListener) {
            super(itemView);
            this.styleName = itemView.findViewById(R.id.style_name);
            this.styleImage = itemView.findViewById(R.id.style_image);
            this.styleStatus = itemView.findViewById(R.id.style_status);
            itemView.setOnClickListener(this);
            this.itemSelectListener = itemSelectListener;
        }

        @Override
        public void onClick(View v) {
            itemSelectListener.onClick(getAdapterPosition());
        }

        public interface ItemSelectListener{
            void onClick(int position);
        }
    }

    private Context context;
    private List<ArtStyle> artStyleList;
    private ArtListViewHolder.ItemSelectListener itemSelectListener;
    public ArtStyleAdapter(Context context, List<ArtStyle> artStyleList, ArtListViewHolder.ItemSelectListener itemSelectListener) {
        this.context = context;
        this.artStyleList = artStyleList;
        this.itemSelectListener = itemSelectListener;
    }

    @NonNull
    @Override
    public ArtListViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view;
        view = LayoutInflater.from(context).inflate(R.layout.arts_item, viewGroup, false);
        return new ArtListViewHolder(view, itemSelectListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ArtListViewHolder viewHolder, int i) {
        ArtStyle artStyle = artStyleList.get(i);
        viewHolder.styleStatus.setImageResource(artStyle.getStatusResource());
        viewHolder.styleImage.setImageResource(artStyle.getStyleResource());
        viewHolder.styleName.setText(artStyle.getStyleName());
    }


    @Override
    public int getItemCount() {
        return artStyleList.size();
    }
}