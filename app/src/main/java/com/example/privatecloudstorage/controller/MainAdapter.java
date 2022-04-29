package com.example.privatecloudstorage.controller;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;

public class MainAdapter extends FragmentPagerAdapter {
    ArrayList<Fragment> fragmentArray=new ArrayList<>();
    ArrayList<String> stringArray=new ArrayList<>();

    public void addFragment(Fragment fragment,String s){
        fragmentArray.add(fragment);
        stringArray.add(s);
    }
    public MainAdapter(@NonNull FragmentManager supportFragmentManager,int behavior) {
        super(supportFragmentManager,behavior);
    }
    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return stringArray.get(position);
    }

    @Override
    public int getCount() {
        //return fragment array list size
        return fragmentArray.size();
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        //return fragment position
        return fragmentArray.get(position);
    }

}
