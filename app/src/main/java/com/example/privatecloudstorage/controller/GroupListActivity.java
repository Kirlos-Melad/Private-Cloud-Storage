package com.example.privatecloudstorage.controller;

import android.content.ContentResolver;
import android.content.Intent;


import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.os.Build;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.privatecloudstorage.R;
import com.example.privatecloudstorage.databinding.ActivityGroupListBinding;
import com.example.privatecloudstorage.interfaces.IAction;
import com.example.privatecloudstorage.model.FileManager;
import com.example.privatecloudstorage.model.FirebaseAuthenticationManager;
import com.example.privatecloudstorage.model.FirebaseStorageManager;
import com.example.privatecloudstorage.model.Group;
import com.example.privatecloudstorage.model.ManagersMediator;
import com.example.privatecloudstorage.model.RecyclerViewItem;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * GroupListActivity class is to make a dynamic List to view user's Group(s)
 */
public class GroupListActivity extends AppCompatActivity {

    private static final String TAG = "GroupListActivity";

    private ArrayAdapterView mAdapter;
    private ArrayList<RecyclerViewItem> mItems;

    ActionBarDrawerToggle _ActionBarDrawerToggle;
    FirebaseAuthenticationManager mFirebaseAuthenticationManager;
    TextView _HeaderName;
    TextView _HeaderEmail;
    CircleImageView _Profile;
    RecyclerView recyclerView;


    private @NonNull
    ActivityGroupListBinding _ActivityGroupListBinding;
    /**
     * handle  user groups
     * @param savedInstanceState
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _ActivityGroupListBinding = ActivityGroupListBinding.inflate(getLayoutInflater());
        setContentView(_ActivityGroupListBinding.getRoot());

        mFirebaseAuthenticationManager =FirebaseAuthenticationManager.getInstance();
        recyclerView=_ActivityGroupListBinding.recyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Start monitoring Cloud and Physical storage
        // MUST CALL THIS HERE
        FileManager.createInstance(getFilesDir());
        FirebaseStorageManager.getInstance();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ManagersMediator.getInstance().StartMonitoring();

        //To access navigation header
        View view =_ActivityGroupListBinding.navgetion.getHeaderView(0);
        _HeaderName = (TextView) view.findViewById(R.id.nav_hedear_user_name);
        _HeaderEmail = (TextView) view.findViewById(R.id.nav_hedear_user_email);
        /* TODO: retrive user info
         *  if there is no photo url == NoPhoto
         */
        _Profile = (CircleImageView)view.findViewById(R.id.img_second);

        _Profile.setImageURI(mFirebaseAuthenticationManager.getUserImage());
        _HeaderName.setText(mFirebaseAuthenticationManager.getCurrentUser().getDisplayName());
        _HeaderEmail.setText(mFirebaseAuthenticationManager.getCurrentUser().getEmail());


        mItems = new ArrayList<>();
       // mItems[0]=new ArrayList<>();
        //getting user's group(s)
        ManagersMediator.getInstance().UserGroupsRetriever(groups -> {
            for(Group group : (ArrayList<Group>) groups){
                ManagersMediator.getInstance().UserSingleGroupRetriever(group.getId(),g->{
                    RecyclerViewItem item = new RecyclerViewItem(((Group)g).getName(), ((Group)g).getDescription(),null, null, null);
                    item.mImage = GetResourceUri(R.drawable.ic_group);
                    mItems.add(item);
                    item._onClickListener=FileExplorerActivity.FolderOnClickListener(new IAction() {
                        @Override
                        public void onSuccess(Object object) {
                            Intent intent = new Intent(GroupListActivity.this, GroupSliderActivity.class);
                            Bundle bundle = new Bundle();
                            bundle.putString("selectedGroupName", ((Group)g).getName());
                            bundle.putString("selectedGroupKey", ((Group)g).getId());
                            bundle.putString("selectedGroupDescription",((Group)g).getDescription());

                            intent.putExtras(bundle);//Put Group number to your next Intent
                            startActivity(intent);
                        }
                    });
                    mAdapter = new ArrayAdapterView(mItems, this);
                    recyclerView.setAdapter( mAdapter);

                    if (mItems.isEmpty()) {
                        _ActivityGroupListBinding.ViewText.setText("NO GROUPS TO SHOW");
                        _ActivityGroupListBinding.ViewText.setVisibility(View.VISIBLE);
                    }
                });

            }


        });
        /**
         * retrieve group key and name and move to GroupSliderActivity
         */


        //Manage navigation bar----------------------------------------------------------
        _ActionBarDrawerToggle = new ActionBarDrawerToggle(this,_ActivityGroupListBinding.drawerLayout,R.string.menu_open,R.string.menu_close);
        _ActivityGroupListBinding.drawerLayout.addDrawerListener(_ActionBarDrawerToggle);

        //whether the drawerlayout is in open or closed state
        _ActionBarDrawerToggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        /**
         * handle user selection from navigation bar to move to another activity
         */
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
                        ManagersMediator.getInstance().GetUserProfileData(userData->{
                            HashMap<String, String> user_data = (HashMap<String, String>) userData;

                            Intent intent = new Intent(GroupListActivity.this, ProfileActivity.class);
                            Bundle bundle = new Bundle();

                            bundle.putString("Uri", user_data.get("ProfilePicture"));
                            bundle.putString("Description", user_data.get("About"));
                            bundle.putString("Name", user_data.get("Name"));
                            Log.d(TAG, "onNavigationItemSelected: ------------------------- " + user_data.get("Name"));
                            bundle.putString("Caller", "User");

                            intent.putExtras(bundle);//Put Group number to your next Intent
                            startActivity(intent);
                            finish();
                        });
                        recreate();
                        _ActivityGroupListBinding.drawerLayout.closeDrawer(GravityCompat.START);
                        break;
                    case R.id.logout:
                        mFirebaseAuthenticationManager.Logout();
                        startActivity(new Intent(GroupListActivity.this,SignInActivity.class));
                        _ActivityGroupListBinding.drawerLayout.closeDrawer(GravityCompat.START);
                }
                return true;
            }
        });

    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(_ActionBarDrawerToggle.onOptionsItemSelected(item))
            return true;
        return super.onOptionsItemSelected(item);
    }
    private Uri GetResourceUri(int resourceId){
        Resources resources =getApplicationContext().getResources();
        Uri uri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(resources.getResourcePackageName(resourceId))
                .appendPath(resources.getResourceTypeName(resourceId))
                .appendPath(resources.getResourceEntryName(resourceId))
                .build();

        return uri;
    }
}