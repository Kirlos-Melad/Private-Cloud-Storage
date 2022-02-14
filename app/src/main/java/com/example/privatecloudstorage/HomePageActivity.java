package com.example.privatecloudstorage;

// Android Libraries
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class HomePageActivity extends AppCompatActivity {
    private Button _CreateGroup, _JoinGroup, _CheckGroups;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

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
                startActivity(new Intent(getApplicationContext(),MainActivity.class));
            }
        });
    }
}