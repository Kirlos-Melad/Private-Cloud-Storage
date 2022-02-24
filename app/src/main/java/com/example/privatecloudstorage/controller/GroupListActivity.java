package com.example.privatecloudstorage.controller;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.privatecloudstorage.R;
import com.example.privatecloudstorage.model.FirebaseDatabaseManager;

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
        mFirebaseDatabaseManager.getUserGroupsObservable()
                .subscribe(new Observer() {
                    Disposable disposable = null;

                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.d("TAG", "onEvent USER INNER: =====================================" + "Current thread " + Thread.currentThread().getId());
                        disposable = d;
                    }

                    @Override
                    public void onNext(@NonNull Object o) {
                        Pair<String, String> groupInformation = (Pair<String, String>) o;
                        mItems.add(groupInformation.second);
                        _ListView.setAdapter(_Adapter);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {
                        disposable.dispose();
                    }
                });


        _ListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override //on any click (choosing a group) to enter and view group contents
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                mFirebaseDatabaseManager.getUserGroupsObservable()
                        .subscribe(new Observer() {
                            Disposable disposable = null;
                            int index = -1;

                            @Override
                            public void onSubscribe(Disposable d) {
                                Log.d("TAG", "onEvent USER INNER: =====================================" + "Current thread " + Thread.currentThread().getId());
                                disposable = d;
                            }

                            @Override
                            public void onNext(@NonNull Object o) {
                                index++;
                                if (index == position) {
                                    Pair<String, String> groupInformation = (Pair<String, String>) o;
                                    //before go to new activity send group name and id as a parameter
                                    Intent intent = new Intent(GroupListActivity.this, GroupContentActivity.class);
                                    Bundle bundle = new Bundle();
                                    bundle.putString("selectedGroupName", groupInformation.second);
                                    bundle.putString("selectedGroupKey", groupInformation.first);

                                    intent.putExtras(bundle); //Put Group number to your next Intent
                                    startActivity(intent);
                                }
                            }

                            @Override
                            public void onError(Throwable e) {
                            }

                            @Override
                            public void onComplete() {
                                disposable.dispose();
                            }
                        });

            }
        });

        // TODO: Display the QR Code
    }

}