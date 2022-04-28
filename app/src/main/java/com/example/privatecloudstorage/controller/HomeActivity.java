package com.example.privatecloudstorage.controller;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.privatecloudstorage.R;
import com.example.privatecloudstorage.databinding.ActivityHomeBinding;
import com.example.privatecloudstorage.databinding.ActivitySignInBinding;
import com.google.android.material.tabs.TabLayout;

public class HomeActivity extends AppCompatActivity {
    public static final String TAG = "HomeActivity";
    private String mSelectedGroupName;
    private String mSelectedGroupKey;
    private @NonNull
    ActivityHomeBinding _ActivityHomeBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _ActivityHomeBinding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(_ActivityHomeBinding.getRoot());

        //put the group name in action bar from Group list activity
        Bundle bundle = getIntent().getExtras();
        if(bundle == null)
            finish();
        mSelectedGroupName = bundle.getString("selectedGroupName");
        mSelectedGroupKey = bundle.getString("selectedGroupKey");
        getSupportActionBar().setTitle(mSelectedGroupName);

        _ActivityHomeBinding.tabLayout.setupWithViewPager(_ActivityHomeBinding.viewPager);

        MainAdapter mainAdapter=new MainAdapter(getSupportFragmentManager(),FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        mainAdapter.addFragment(new Fragment1(),"NORMAL FILES");
        mainAdapter.addFragment(new Fragment2(),"STREPPED FILES");


        //TODO:use old Bundle if valid to be used more than one time
        Bundle bundle1 = new Bundle();
        bundle1.putString("selectedGroupKey1",mSelectedGroupKey);
       // set Fragment class Arguments
        Fragment3 fragobj = new Fragment3();
        fragobj.setArguments(bundle1);
        //getSupportFragmentManager().beginTransaction()
          //      .replace(R.id.fragment_id, fragobj, TAG).commit();

        mainAdapter.addFragment(fragobj,"MEMBERS");


        _ActivityHomeBinding.viewPager.setAdapter(mainAdapter);

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