package com.example.privatecloudstorage.controller;

import android.content.Intent;
import android.net.Uri;


import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.os.Build;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;
import androidx.annotation.RequiresApi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.privatecloudstorage.R;
import com.example.privatecloudstorage.databinding.ActivityGroupListBinding;
import com.example.privatecloudstorage.databinding.ActivitySignInBinding;
import com.example.privatecloudstorage.model.FileManager;
import com.example.privatecloudstorage.model.FirebaseAuthenticationManager;
import com.example.privatecloudstorage.model.FirebaseDatabaseManager;
import com.example.privatecloudstorage.model.FirebaseStorageManager;
import com.example.privatecloudstorage.model.Group;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.example.privatecloudstorage.model.ManagersMediator;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * GroupListActivity class is to make a dynamic List to view user's Group(s)
 */
public class GroupListActivity extends AppCompatActivity {
    private static final String TAG = "GroupListActivity";
    ArrayList<String> mItems;
    ArrayAdapter<String> _Adapter;
    ActionBarDrawerToggle actionBarDrawerToggle;
    FirebaseAuthenticationManager firebaseAuthenticationManager;
    TextView HeaderName ;
    TextView HeaderEmail ;
    CircleImageView profile;

    private @NonNull
    ActivityGroupListBinding _ActivityGroupListBinding;
    private FirebaseDatabaseManager mFirebaseDatabaseManager;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _ActivityGroupListBinding = ActivityGroupListBinding.inflate(getLayoutInflater());
        setContentView(_ActivityGroupListBinding.getRoot());

        mFirebaseDatabaseManager = FirebaseDatabaseManager.getInstance();
        firebaseAuthenticationManager=FirebaseAuthenticationManager.getInstance();

        // Start monitoring Cloud and Physical storage
        // MUST CALL THIS HERE
        FileManager.createInstance(getFilesDir());
        FirebaseStorageManager.getInstance();

        //To access navigation header
        View view=_ActivityGroupListBinding.navgetion.getHeaderView(0);
        HeaderName = (TextView) view.findViewById(R.id.nav_hedear_user_name);
        HeaderEmail = (TextView) view.findViewById(R.id.nav_hedear_user_email);
        profile= (CircleImageView)view.findViewById(R.id.img_second);

        profile.setImageURI(firebaseAuthenticationManager.getUserImage());
        HeaderName.setText(firebaseAuthenticationManager.getCurrentUser().getDisplayName());
        HeaderEmail.setText(firebaseAuthenticationManager.getCurrentUser().getEmail());


        mItems = new ArrayList<>();
        //connecting _Adapter with the mItems List
        _Adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, mItems);
        //setting the ListView Adapter with _Adapter
        _ActivityGroupListBinding.Listview.setAdapter(_Adapter);

        //getting user's group(s)

        

        ManagersMediator.getInstance().UserGroupsRetriever(groups -> {
            for(Group group : (ArrayList<Group>) groups){
                mItems.add(group.getName());
            }
            _ActivityGroupListBinding.Listview.setAdapter(_Adapter);
        });


        _ActivityGroupListBinding.Listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                 @Override //on any click (choosing a group) to enter and view group contents
                 public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                     ManagersMediator.getInstance().UserGroupsRetriever(groups -> {
                         Group group = ((ArrayList<Group>) groups).get(position);
                         //before go to new activity send group name and id as a parameter
                         Intent intent = new Intent(GroupListActivity.this, HomeActivity.class);
                         Bundle bundle = new Bundle();
                         bundle.putString("selectedGroupName", group.getName());
                         bundle.putString("selectedGroupKey", group.getId());

                         intent.putExtras(bundle); //Put Group number to your next Intent
                         startActivity(intent);
                     });
                 }
        });

        //Manage navigation bar----------------------------------------------------------
        actionBarDrawerToggle = new ActionBarDrawerToggle(this,_ActivityGroupListBinding.drawerLayout,R.string.menu_open,R.string.menu_close);
        _ActivityGroupListBinding.drawerLayout.addDrawerListener(actionBarDrawerToggle);

        //whether the drawerlayout is in open or closed state
        actionBarDrawerToggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        _ActivityGroupListBinding.navgetion.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.creat:
                        //Log.i(TAG, "first item clicked: ");
                        startActivity(new Intent(GroupListActivity.this,CreateGroupActivity.class));
                        _ActivityGroupListBinding.drawerLayout.closeDrawer(GravityCompat.START);
                        break;
                    case R.id.join:
                        //Log.i(TAG, "second item clicked: ");
                        startActivity(new Intent(GroupListActivity.this,JoinGroupActivity.class));
                        _ActivityGroupListBinding.drawerLayout.closeDrawer(GravityCompat.START);
                        break;
                    case R.id.profile:
                        startActivity(new Intent(GroupListActivity.this,ProfileActivity.class));
                        _ActivityGroupListBinding.drawerLayout.closeDrawer(GravityCompat.START);
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

