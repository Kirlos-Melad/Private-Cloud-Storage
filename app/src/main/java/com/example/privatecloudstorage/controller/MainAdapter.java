package com.example.privatecloudstorage.controller;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

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

    @NonNull
    @Override
    public Fragment getItem(int position) {
        //return fragment position
        return fragmentArray.get(position);
    }

    @Override
    public int getCount() {
        //return fragment array list size
        return fragmentArray.size();
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return stringArray.get(position);
    }
}
