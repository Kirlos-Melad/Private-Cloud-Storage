package com.example.privatecloudstorage;

// Android Libraries
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.FileObserver;
import android.view.View;
import android.widget.Button;

import java.io.File;

public class HomePageActivity extends AppCompatActivity {
    private Button _CreateGroup, _JoinGroup, _CheckGroups;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        // Start monitoring Cloud and Physical storage
        // MUST CALL THIS HERE
        FirebaseDatabaseManager.getInstance().MonitorGroups(getFilesDir());
        FirebaseStorageManager.getInstance(getFilesDir(),(FileObserver.CREATE | FileObserver.MOVED_TO | FileObserver.CLOSE_WRITE | FileObserver.MODIFY)).startWatching();

        _CreateGroup = findViewById(R.id.creatgroup);
        _JoinGroup = findViewById(R.id.joingroup);
        _CheckGroups = findViewById(R.id.chickyourgroup);

        //TODO: Add Log out Button

        _CreateGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(),CreateGroupActivity.class));
            }
        });
        _JoinGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(),JoinGroupActivity.class));
            }
        });
        _CheckGroups.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(),GroupListActivity.class));
            }
        });
    }
}
