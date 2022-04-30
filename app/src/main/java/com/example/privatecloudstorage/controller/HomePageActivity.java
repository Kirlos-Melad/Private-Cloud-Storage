package com.example.privatecloudstorage.controller;

// Android Libraries
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.privatecloudstorage.databinding.ActivityHomePageBinding;
import com.example.privatecloudstorage.model.FileManager;
import com.example.privatecloudstorage.model.FirebaseStorageManager;

/**
 * HomePageActivity is an activity to direct us where we want, Create Group ! JoinGroup ! or View existing group(s)
 */

public class HomePageActivity extends AppCompatActivity {
    private static final String TAG = "HomePageActivity";
    private ActivityHomePageBinding _ActivityHomePageBinding;
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _ActivityHomePageBinding = ActivityHomePageBinding.inflate(getLayoutInflater());
        setContentView(_ActivityHomePageBinding.getRoot());

        // Start monitoring Cloud and Physical storage
        // MUST CALL THIS HERE
        FileManager.createInstance(getFilesDir());
        FirebaseStorageManager.getInstance();

        //TODO: Add Log out Button
        //Button to direct to CreateGroupActivity
        _ActivityHomePageBinding.creatgroup.setOnClickListener(v ->
                startActivity(new Intent(this,CreateGroupActivity.class)));
        //Button to direct to JoinGroupActivity
        _ActivityHomePageBinding.joingroup.setOnClickListener(v ->
                startActivity(new Intent(this,JoinGroupActivity.class)));
        //Button to direct to GroupListActivity
        _ActivityHomePageBinding.chickyourgroup.setOnClickListener(v ->
                startActivity(new Intent(this,GroupListActivity.class)));

    }
}
