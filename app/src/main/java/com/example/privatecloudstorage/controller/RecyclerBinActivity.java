package com.example.privatecloudstorage.controller;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
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
        LayoutInflater inflater = getLayoutInflater();
        _RecyclerViewBinding = RecyclerViewBinding.inflate(inflater);
        setContentView(_RecyclerViewBinding.getRoot());

        Bundle bundle = getIntent().getExtras();
        if(bundle == null)
            finish();
        mGroupKey = bundle.getString("Key");
        ManagersMediator.getInstance().RecycledFilesRetriever(mGroupKey,recycledFiles->{

            ArrayList<Pair<String,String>>recycledFilesArray=(ArrayList<Pair<String, String>>) recycledFiles;
            if(!recycledFilesArray.isEmpty()){
                _RecyclerViewBinding.RecyclerViewText.setVisibility(View.GONE);
                ShowRecycledFiles(recycledFilesArray);

            }else{
                _RecyclerViewBinding.RecyclerViewText.setText("NO FILES TO SHOW");
                _RecyclerViewBinding.RecyclerViewText.setVisibility(View.VISIBLE);
            }
        });
    }
    private void ShowRecycledFiles(ArrayList<Pair<String,String>>recycledFilesArray){
        for(Pair<String,String> file :  recycledFilesArray){
            RecyclerViewItem item = new RecyclerViewItem(file.second, null, null, null, new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    PopupMenu popupMenu = new PopupMenu(getApplicationContext(),v);
                    popupMenu.getMenu().add("Restore");
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @RequiresApi(api = Build.VERSION_CODES.Q)
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            if(item.getTitle().equals("Restore")){
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
                        }
                    });
                    popupMenu.show();
                    return true;
                }
            });
            mItems.add(item);
        }
        mAdapter = new ArrayAdapterView(mItems);
        _Recyclerview.setAdapter(mAdapter);
    }
}