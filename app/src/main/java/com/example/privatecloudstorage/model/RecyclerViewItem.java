package com.example.privatecloudstorage.model;

import android.graphics.Color;
import android.net.Uri;
import android.view.View;

import com.example.privatecloudstorage.R;

public class RecyclerViewItem {
    public String mName;
    public String mSubTitle;
    public Uri mImage;
    public View.OnClickListener _onClickListener;
    public View.OnLongClickListener _onLongClickListener;
    public int mNameColor;

    public RecyclerViewItem(String name,String subTitle, Uri image, View.OnClickListener onClickListener,View.OnLongClickListener onLongClickListener) {
        this(name,subTitle, image,  onClickListener, onLongClickListener, R.attr.colorOnPrimary);
    }

    public RecyclerViewItem(String name,String subTitle, Uri image, View.OnClickListener onClickListener,View.OnLongClickListener onLongClickListener,int color){
        this.mName = name;
        this.mSubTitle=subTitle;
        this.mImage = image;
        this._onClickListener = onClickListener;
        this._onLongClickListener = onLongClickListener;
        this.mNameColor = color;
    }
}