package com.example.privatecloudstorage.controller;


import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import android.text.InputType;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.example.privatecloudstorage.R;
import com.example.privatecloudstorage.databinding.ActivityGroupContentBinding;
import com.example.privatecloudstorage.databinding.ActivityGroupSliderBinding;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

//replace fragment activity with AppCompatActivity

/**
 * mange tab layout
 */
public class GroupSliderActivity extends AppCompatActivity {
    public static final byte MEMBERS=0x01;
    public static final byte NORMAL_FILES=0x02;
    public static final byte STRIPPED_FILES=0x03;

    ArrayList<String> titels=new ArrayList<>();
    private ActivityGroupSliderBinding _ActivityGroupSliderBinding;
    //getSupportActionBar().setTitle(mSelectedGroupName);

    /**
     * The number of pages (wizard steps) to show in this demo.
     */
    private static final int NUM_PAGES = 3;

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private ViewPager2 viewPager;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private FragmentStateAdapter pagerAdapter;
    private String mSelectedGroupName;
    private String mSelectedGroupKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _ActivityGroupSliderBinding = ActivityGroupSliderBinding.inflate(getLayoutInflater());
        setContentView(_ActivityGroupSliderBinding.getRoot());
        TabLayout tabLayout;

        Bundle bundle = getIntent().getExtras();
        if(bundle == null)
            finish();
        mSelectedGroupName = bundle.getString("selectedGroupName");
        mSelectedGroupKey = bundle.getString("selectedGroupKey");
        getSupportActionBar().setTitle(mSelectedGroupName);



        viewPager = findViewById(R.id.pager);
        pagerAdapter = new ScreenSlidePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        tabLayout= findViewById(R.id.tab_Layout);
        titels.add("MEMBERS");
        titels.add("NORMAL FILES");
        titels.add("STRIPPED FILES");

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    tab.setText(titels.get(position));
                }
        ).attach();

    }

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else {
            // Otherwise, select the previous step.
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
        }
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
    /**
     * A simple pager adapter that represents 3 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private class ScreenSlidePagerAdapter extends FragmentStateAdapter {
        public ScreenSlidePagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @Override
        public Fragment createFragment(int position) {
            Bundle bundle = new Bundle();
            bundle.putString("selectedGroupName", mSelectedGroupName);
            bundle.putString("selectedGroupKey", mSelectedGroupKey);

            GroupFragment groupFragment=new GroupFragment();
            switch (position){
                case 0:
                    bundle.putByte("tab", MEMBERS);
                    break;
                case 1:
                    bundle.putByte("tab", NORMAL_FILES);
                    break;
                case 2:
                    bundle.putByte("tab", STRIPPED_FILES);
                    break;
                default:
                    throw new ArrayIndexOutOfBoundsException(String.format("fragment position %s out of bounds", position));

            }
            groupFragment.setArguments(bundle);

            return groupFragment;
        }

        @Override
        public int getItemCount() {
            return NUM_PAGES;
        }




    }
}