package com.example.privatecloudstorage;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class HomePageActivity extends AppCompatActivity {
    private Button mCreateGroup, mJoinGroup, mCheckGroups;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        mCreateGroup = findViewById(R.id.creatgroup);
        mJoinGroup = findViewById(R.id.joingroup);
        mCheckGroups = findViewById(R.id.chickyourgroup);

        mCreateGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(),CreateGroupActivity.class));
            }
        });
        mJoinGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(),JoinGroupActivity.class));
            }
        });
        mCheckGroups.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(),MainActivity.class));
            }
        });
    }
}