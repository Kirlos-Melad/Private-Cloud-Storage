package com.example.privatecloudstorage.controller;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.privatecloudstorage.R;
import com.example.privatecloudstorage.model.ManagersMediator;
import com.example.privatecloudstorage.model.RecyclerViewItem;

import java.io.File;
import java.util.ArrayList;

public class GroupFragment extends Fragment {

    private String mSelectedGroupKey;
    private String mSelectedGroupName;
    private View _View;
    private ArrayAdapterView mAdapter;
    private ArrayList<RecyclerViewItem> mItems;
    private byte mTab;
    //RecyclerView.LayoutManager _layoutManager;

    /**
     * initialize view
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return fragment group layout
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        _View = inflater.inflate(R.layout.fragment_group, container, false);
        
        return _View;
    }


    /**
     * display fragment
     * @param savedInstanceState
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mItems = new ArrayList<>();
        mSelectedGroupKey = getArguments().getString("selectedGroupKey");
        mSelectedGroupName = getArguments().getString("selectedGroupName");
        mTab = getArguments().getByte("tab");
        File path;
        switch (mTab){
            case GroupSliderActivity.MEMBERS:
                ShowGroupMembers();
                break;
                //TODO:show group content activity inside fragment
            /*case GroupSliderActivity.NORMAL_FILES:
                 path = new File(FileManager.getInstance().GetApplicationDirectory(),
                        mSelectedGroupKey+" "+mSelectedGroupName);
                ShowFolderFiles(path);
                break;*/
           /* case GroupSliderActivity.STRIPPED_FILES:
                 path = new File(FileManager.getInstance().GetApplicationDirectory(),
                        mSelectedGroupKey+" "+mSelectedGroupName);
                ShowFolderFiles(path);
                break;*/
        }
    }

    /**
     * get user groups and display them on list view
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void ShowGroupMembers(){
        ManagersMediator.getInstance().GroupMembersRetriever(mSelectedGroupKey, users -> {
            for(String member : (ArrayList<String>) users){
                mItems.add(new RecyclerViewItem(member,null,null));
            }
            mAdapter = new ArrayAdapterView(mItems);
            //_ActivityGroupContentBinding.recyclerView.setLayoutManager(new LinearLayoutManager(GroupContentActivity.this));
           // _ActivityGroupContentBinding.recyclerView2.setAdapter(adapter);
            /*if(mItems.isEmpty())
                  textView.setVisibility(View.VISIBLE);
            _ListView.setAdapter(mAdapter);*/
        });
    }
    private void ShowFolderFiles(){
    }
}
