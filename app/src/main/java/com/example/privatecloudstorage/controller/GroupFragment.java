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

import com.example.privatecloudstorage.R;
import com.example.privatecloudstorage.model.FileManager;
import com.example.privatecloudstorage.model.ManagersMediator;

import java.io.File;
import java.util.ArrayList;

public class GroupFragment extends Fragment {

    private TextView textView;
    private String mSelectedGroupKey;
    private String mSelectedGroupName;
    private View _View;
    private ArrayAdapter<String> _Adapter;
    private ListView _ListView;
    private ArrayList<String> mItems;
    private byte mTab;

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
        mItems = new ArrayList<>();
        _Adapter = new ArrayAdapter<>(getActivity().getApplicationContext(), android.R.layout.simple_list_item_1, mItems);
        _ListView = _View.findViewById(R.id.fragment_list_view);
        _ListView.setAdapter(_Adapter);
        textView = _View.findViewById(R.id.fragment_text);
        textView.setText("Empty");

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
                mItems.add(member);
            }
            if(mItems.isEmpty())
                  textView.setVisibility(View.VISIBLE);
            _ListView.setAdapter(_Adapter);
        });
    }
    private void ShowFolderFiles(){
    }
}
