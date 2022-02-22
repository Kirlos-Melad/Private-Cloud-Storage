package com.example.privatecloudstorage;

// Android Libraries

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.FileObserver;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;

/**
 * HomePageActivity is an activity to direct us where we want, Create Group ! JoinGroup ! or View existing group(s)
 */

public class HomePageActivity extends AppCompatActivity {
    private static final String TAG = "HomePageActivity";
    private Button _CreateGroup, _JoinGroup, _CheckGroups;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        // Start monitoring Cloud and Physical storage
        // MUST CALL THIS HERE
        Log.d("TAG", "Home Page: =====================================" + "Current thread " + Thread.currentThread().getId());
        //startService(new Intent(this, FirebaseStorageManager.class));
        //FirebaseDatabaseManager.getInstance().MonitorGroups(getFilesDir());
        FirebaseStorageManager.CreateInstance(getFilesDir(), (FileObserver.CREATE | FileObserver.MOVED_TO));

        _CreateGroup = findViewById(R.id.creatgroup);
        _JoinGroup = findViewById(R.id.joingroup);
        _CheckGroups = findViewById(R.id.chickyourgroup);


        //TODO: Add Log out Button
        //Button to direct to CreateGroupActivity
        _CreateGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(),CreateGroupActivity.class));
            }
        });
        //Button to direct to JoinGroupActivity
        _JoinGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(),JoinGroupActivity.class));
            }
        });
        //Button to direct to GroupListActivity
        _CheckGroups.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(),GroupListActivity.class));
            }
        });
    }
}
