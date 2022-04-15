package com.example.privatecloudstorage.controller;

// Android Libraries
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.privatecloudstorage.R;
import com.example.privatecloudstorage.databinding.ActivityHomePageBinding;
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
        FirebaseStorageManager.CreateInstance(getFilesDir());

        //TODO: Add Log out Button
        //Button to direct to CreateGroupActivity
        _ActivityHomePageBinding.creatgroup.setOnClickListener(v ->
                startActivity(new Intent(this,HomeActivity.class)));
        //Button to direct to JoinGroupActivity
        _ActivityHomePageBinding.joingroup.setOnClickListener(v ->
                startActivity(new Intent(this,JoinGroupActivity.class)));
        //Button to direct to GroupListActivity
        _ActivityHomePageBinding.chickyourgroup.setOnClickListener(v ->
                startActivity(new Intent(this,GroupListActivity.class)));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.setting_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item1:
                Toast.makeText(this, "Item 1 selected", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.item2:
                Toast.makeText(this, "Item 2 selected", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.item3:
                Toast.makeText(this, "Item 3 selected", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
