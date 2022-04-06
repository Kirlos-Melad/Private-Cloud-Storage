package com.example.privatecloudstorage.controller;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.privatecloudstorage.R;
import com.example.privatecloudstorage.model.FirebaseDatabaseManager;
import com.example.privatecloudstorage.model.Group;
import com.example.privatecloudstorage.model.ManagersMediator;

import java.util.ArrayList;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * GroupListActivity class is to make a dynamic List to view user's Group(s)
 */
public class GroupListActivity extends AppCompatActivity {
    private static final String TAG = "GroupListActivity";
    ListView _ListView;
    ArrayList<String> mItems;
    ArrayAdapter<String> _Adapter;

    private FirebaseDatabaseManager mFirebaseDatabaseManager;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_list);
        _ListView = findViewById(R.id.Listview);
        mItems = new ArrayList<>();
        mFirebaseDatabaseManager = FirebaseDatabaseManager.getInstance();
        //connecting _Adapter with the mItems List
        _Adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, mItems);
        //setting the ListView Adapter with _Adapter
        _ListView.setAdapter(_Adapter);

        //getting user's group(s)
        ManagersMediator.getInstance().UserGroupsRetriever(groups -> {
            for(Group group : (ArrayList<Group>) groups){
                mItems.add(group.getName());
            }
            _ListView.setAdapter(_Adapter);
        });


        _ListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override //on any click (choosing a group) to enter and view group contents
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                ManagersMediator.getInstance().UserGroupsRetriever(groups -> {
                    Group group = ((ArrayList<Group>)groups).get(position);
                    //before go to new activity send group name and id as a parameter
                    Intent intent = new Intent(GroupListActivity.this, GroupContentActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("selectedGroupName", group.getName());
                    bundle.putString("selectedGroupKey", group.getId());

                    intent.putExtras(bundle); //Put Group number to your next Intent
                    startActivity(intent);
                });
            }
        });

        // TODO: Display the QR Code
    }

}