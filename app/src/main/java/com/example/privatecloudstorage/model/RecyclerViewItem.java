package com.example.privatecloudstorage.model;

import android.net.Uri;
import android.view.View;

public class RecyclerViewItem {
    public String mName;
    public Uri mImage;
    public View.OnClickListener _onClickListener;


    public RecyclerViewItem(String name, Uri image, View.OnClickListener onClickListener) {
        this.mName = name;
        this.mImage = image;
        this._onClickListener = onClickListener;
    }
}
