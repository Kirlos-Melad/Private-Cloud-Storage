package com.example.privatecloudstorage.controller;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.privatecloudstorage.R;
import com.example.privatecloudstorage.model.RecyclerViewItem;

import java.util.ArrayList;

public class ArrayAdapterView extends RecyclerView.Adapter<ArrayAdapterView.ViewHolder> {
    private static final String TAG = "ArrayAdapterView";
    // member / group / file name
    ArrayList<RecyclerViewItem> mItems;
    Context _Context;

    public ArrayAdapterView(ArrayList items, Context context){
        this.mItems = items;
        this._Context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_view_item,parent,false);
        return new ViewHolder(view);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // name
        holder._TextView.setText(mItems.get(position).mName);
        //subtext
        holder._SubTextView.setText(mItems.get(position).mSubTitle);
        // image
        holder._ImageView.setImageURI(mItems.get(position).mImage);
        //color
        TypedValue typedValue = new TypedValue();
        _Context.getTheme().resolveAttribute((mItems.get(position).mNameColor),typedValue,true);
        if(typedValue.isColorType())
            holder._TextView.setTextColor(typedValue.data);
        else
            holder._TextView.setTextColor(mItems.get(position).mNameColor);

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
        TextView _SubTextView;
        ImageView _ImageView;

        public ViewHolder(View itemView) {
            super(itemView);
            _TextView = itemView.findViewById(R.id.item_name);
            _ImageView = itemView.findViewById(R.id.item_image);
            _SubTextView = itemView.findViewById(R.id.sub_text);
        }
    }
}
