package com.example.privatecloudstorage;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.io.File;

public class FileManagerListActivity extends AppCompatActivity {
    RecyclerView mRecyclerView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_manager_list);

        mRecyclerView = findViewById(R.id.recycler_view);
        TextView noFilesText = findViewById(R.id.nofiles_textview);

        //receive parameters from GroupContentActivity
        String path = getIntent().getStringExtra("path");
        String action =  getIntent().getStringExtra("action");
        String selectedGroupName = getIntent().getStringExtra("selectedGroupName");
        String selectedGroupKey = getIntent().getStringExtra("selectedGroupKey");

        File file = new File(path);

        File[] filesAndFolders = file.listFiles();

        if(filesAndFolders==null || filesAndFolders.length ==0){
            noFilesText.setVisibility(View.VISIBLE);
            return;
        }
        noFilesText.setVisibility(View.INVISIBLE);

        mRecyclerView.setLayoutManager(new LinearLayoutManager((this)));
        mRecyclerView.setAdapter(new FileManagerAdapter(FileManagerListActivity.this,filesAndFolders,action,selectedGroupName,selectedGroupKey));
    }
}