package com.example.privatecloudstorage.controller;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Bundle;
import android.view.View;

import com.example.privatecloudstorage.databinding.ActivityFileManagerListBinding;

import java.io.File;

public class FileExplorerListActivity extends AppCompatActivity {
    private static final String TAG = "FileManagerListActivity";
    private ActivityFileManagerListBinding _ActivityFileManagerListBinding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _ActivityFileManagerListBinding = ActivityFileManagerListBinding.inflate(getLayoutInflater());
        setContentView(_ActivityFileManagerListBinding.getRoot());

        //receive parameters from GroupContentActivity
        String path = getIntent().getStringExtra("path");
        String action =  getIntent().getStringExtra("action");
        String selectedGroupName = getIntent().getStringExtra("selectedGroupName");
        String selectedGroupKey = getIntent().getStringExtra("selectedGroupKey");

        File file = new File(path);
        File[] filesAndFolders = file.listFiles();


        if(_ActivityFileManagerListBinding.nofilesTextview ==null || filesAndFolders.length ==0){
            _ActivityFileManagerListBinding.nofilesTextview.setVisibility(View.VISIBLE);
            return;
        }
        _ActivityFileManagerListBinding.nofilesTextview.setVisibility(View.INVISIBLE);

        _ActivityFileManagerListBinding.recyclerView.setLayoutManager(new LinearLayoutManager((this)));
        _ActivityFileManagerListBinding.recyclerView.setAdapter(new FileExplorerAdapter(FileExplorerListActivity.this,filesAndFolders,action,selectedGroupName,selectedGroupKey));
    }
}