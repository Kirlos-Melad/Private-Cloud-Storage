package com.example.privatecloudstorage.controller;


import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import androidx.recyclerview.widget.RecyclerView;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import com.example.privatecloudstorage.databinding.ActivityProfileBinding;
import com.example.privatecloudstorage.databinding.RecyclerViewBinding;
import com.example.privatecloudstorage.model.ManagersMediator;
import com.example.privatecloudstorage.model.RecyclerViewItem;
import com.example.privatecloudstorage.model.User;

import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.example.privatecloudstorage.R;
import com.example.privatecloudstorage.databinding.ActivityCreateGroupBinding;
import com.example.privatecloudstorage.databinding.RecyclerViewBinding;
import com.example.privatecloudstorage.model.ManagersMediator;
import com.example.privatecloudstorage.model.RecyclerViewItem;


import java.io.File;
import java.util.ArrayList;

public class RecyclerBinActivity extends AppCompatActivity {
    private @NonNull
    RecyclerViewBinding _RecyclerViewBinding;
    private String mGroupKey;
    private ArrayAdapterView mAdapter;
    private ArrayList<RecyclerViewItem> mItems;
    private RecyclerView _Recyclerview;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        _RecyclerViewBinding = RecyclerViewBinding.inflate(getLayoutInflater());
        setContentView(_RecyclerViewBinding.getRoot());

        _RecyclerViewBinding.Recyclerview.setLayoutManager(new LinearLayoutManager(this));


        Bundle bundle = getIntent().getExtras();
        if(bundle == null)
            finish();
        mGroupKey = bundle.getString("Key");
        ManagersMediator.getInstance().RecycledFilesRetriever(mGroupKey,recycledFiles->{

            ArrayList<Pair<String,String>>recycledFilesArray=(ArrayList<Pair<String, String>>) recycledFiles;
            if(!recycledFilesArray.isEmpty()){

                mItems = new ArrayList<>();

                _RecyclerViewBinding.RecyclerViewText.setVisibility(View.GONE);
                ShowRecycledFiles(recycledFilesArray);

            }else{
                _RecyclerViewBinding.RecyclerViewText.setText("NO FILES TO SHOW");
                _RecyclerViewBinding.RecyclerViewText.setVisibility(View.VISIBLE);
            }
        });
    }



    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void ShowRecycledFiles(ArrayList<Pair<String,String>>recycledFilesArray){
        for(Pair<String,String> file :  recycledFilesArray){
            RecyclerViewItem item = new RecyclerViewItem(file.second, null, GetFileItem(file.second), null, v -> {

                PopupMenu popupMenu = new PopupMenu(getApplicationContext(),v);
                popupMenu.getMenu().add("Restore");
                popupMenu.setOnMenuItemClickListener(item1 -> {
                    if(item1.getTitle().equals("Restore")){
                        ManagersMediator.getInstance().RestoreRecycledFile(mGroupKey,file.first,object->{
                            int index=-1;
                            for(int i=0;i<recycledFilesArray.size(); i++){
                                if(file.first.equals(recycledFilesArray.get(i).first)){
                                    index=i;
                                }
                            }
                            recycledFilesArray.remove(index);
                            ShowRecycledFiles(recycledFilesArray);
                        });
                    }
                    return true;
                });
                popupMenu.show();
                return true;

            });
            mItems.add(item);
        }
        mAdapter = new ArrayAdapterView(mItems, getApplicationContext());

        _RecyclerViewBinding.Recyclerview.setAdapter(mAdapter);

    }
    private Uri GetFileItem(String file){
         if (file.contains(".pdf")) {
        //pdf Icon
        return GetResourceUri(R.drawable.ic_baseline_picture_as_pdf_24);
    } else if (file.contains(".jpg") || file.contains(".png")) {
        //Image Icon
        return GetResourceUri(R.drawable.ic_baseline_image_24);
    } else if (file.contains(".mp3")) {
        //Audio Icon
        return GetResourceUri(R.drawable.ic_baseline_audiotrack_24);
    } else if (file.contains(".mp4")) {
        //Video Icon
        return GetResourceUri(R.drawable.ic_baseline_video_library_24);
    } else {
        //File Icon
        return GetResourceUri(R.drawable.ic_baseline_insert_drive_file_24);
    }
}

    private Uri GetResourceUri(int resourceId){
        Resources resources = getApplicationContext().getResources();
        Uri uri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(resources.getResourcePackageName(resourceId))
                .appendPath(resources.getResourceTypeName(resourceId))
                .appendPath(resources.getResourceEntryName(resourceId))
                .build();

        return uri;
    }
}
