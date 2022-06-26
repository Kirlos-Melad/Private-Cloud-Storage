package com.example.privatecloudstorage.controller;


import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import androidx.recyclerview.widget.RecyclerView;

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
            RecyclerViewItem item = new RecyclerViewItem(file.second, null, null, null, v -> {
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
        mAdapter = new ArrayAdapterView(mItems);

        _RecyclerViewBinding.Recyclerview.setAdapter(mAdapter);

    }
}
