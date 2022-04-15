package com.example.privatecloudstorage;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.ArrayList;

public class MainAdapter extends FragmentPagerAdapter {
    ArrayList<Fragment> fragmentarray=new ArrayList<>();
    ArrayList<String>stringarray=new ArrayList<>();
    //int[]images={R.drawable.group_add,R.drawable.add_check};

    public void addFragment(Fragment fragment,String s){
        fragmentarray.add(fragment);
        stringarray.add(s);
    }
    public MainAdapter(@NonNull FragmentManager supportFragmentManager,int behavior) {
        super(supportFragmentManager,behavior);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        //return fragment podition
        return fragmentarray.get(position);
    }

    @Override
    public int getCount() {
        //return fragment array list size
        return fragmentarray.size();
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return stringarray.get(position);
    }
}
