package com.example.privatecloudstorage.controller;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.privatecloudstorage.R;
import com.example.privatecloudstorage.model.FirebaseDatabaseManager;
import com.example.privatecloudstorage.model.Group;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.material.navigation.NavigationView;

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

    DrawerLayout drawerLayout;
    NavigationView navigationView;
    ActionBarDrawerToggle actionBarDrawerToggle;

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
                        Group groupInformation = (Group) o;
                        mItems.add(groupInformation.getName());
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
                                    Group groupInformation = (Group) o;
                                    //before go to new activity send group name and id as a parameter
                                    Intent intent = new Intent(GroupListActivity.this, HomeActivity.class);
                                    Bundle bundle = new Bundle();
                                    bundle.putString("selectedGroupName", groupInformation.getName());
                                    bundle.putString("selectedGroupKey", groupInformation.getId());

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

        drawerLayout=findViewById(R.id.drawer_layout);
        navigationView=findViewById(R.id.navgetion);
        actionBarDrawerToggle=new ActionBarDrawerToggle(this,drawerLayout,R.string.menu_open,R.string.menu_close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);

        //whether the drawerlayout is in open or closed state
        actionBarDrawerToggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.creat:
                        //Log.i(TAG, "first item clicked: ");
                        startActivity(new Intent(GroupListActivity.this,CreateGroupActivity.class));
                        drawerLayout.closeDrawer(GravityCompat.START);
                        break;
                    case R.id.join:
                        //Log.i(TAG, "second item clicked: ");
                        startActivity(new Intent(GroupListActivity.this,JoinGroupActivity.class));
                        drawerLayout.closeDrawer(GravityCompat.START);
                        break;
                    case R.id.profile:
                        startActivity(new Intent(GroupListActivity.this,ProfileActivity.class));
                        drawerLayout.closeDrawer(GravityCompat.START);
                        break;

                }
                return true;
            }
        });
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(actionBarDrawerToggle.onOptionsItemSelected(item))
            return true;
        return super.onOptionsItemSelected(item);
    }
}