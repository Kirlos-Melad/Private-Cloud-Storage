package com.example.privatecloudstorage.controller;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.privatecloudstorage.R;
import com.example.privatecloudstorage.model.RecyclerViewItem;

import java.util.ArrayList;

public class ArrayAdapterView extends RecyclerView.Adapter<ArrayAdapterView.ViewHolder> {
    private static final String TAG = "ArrayAdapterView";
    // member / group / file name
    ArrayList<RecyclerViewItem> mItems;

    public ArrayAdapterView(ArrayList names){
        this.mItems = names;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_view_item,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // name
        holder._TextView.setText(mItems.get(position).mName);
        // image
        holder._ImageView.setImageURI(mItems.get(position).mImage);

        holder.itemView.setOnClickListener(mItems.get(position)._onClickListener);
        holder.itemView.setOnLongClickListener(mItems.get(position)._onLongClickListener);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    /**
     * view directory/file icon and its name
     */
    public class ViewHolder extends RecyclerView.ViewHolder{
        TextView _TextView;
        ImageView _ImageView;

        public ViewHolder(View itemView) {
            super(itemView);
            _TextView = itemView.findViewById(R.id.item_name);
            _ImageView = itemView.findViewById(R.id.item_image);
        }
    }
}