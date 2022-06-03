package com.example.privatecloudstorage.model;

import android.net.Uri;
import android.view.View;

public class RecyclerViewItem {
    public String mName;
    public String mSubTitle;
    public Uri mImage;
    public View.OnClickListener _onClickListener;
    public View.OnLongClickListener _onLongClickListener;

    public RecyclerViewItem(String name,String subTitle, Uri image, View.OnClickListener onClickListener,View.OnLongClickListener onLongClickListener) {
        this.mName = name;
        this.mSubTitle=subTitle;
        this.mImage = image;
        this._onClickListener = onClickListener;
        this._onLongClickListener = onLongClickListener;
    }
}
