package com.example.privatecloudstorage.controller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.content.Context;
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

import me.dm7.barcodescanner.core.ViewFinderView;

public class GroupFragment extends Fragment {

    private TextView textView;
    private String mSelectedGroupKey;
    private String mSelectedGroupName;
    private View view;
    private ArrayAdapter<String> _Adapter;
    private ListView listView;
    private ArrayList<String> mItems;
    private byte mTab;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_group, container, false);
        mItems = new ArrayList<>();
        _Adapter = new ArrayAdapter<>(getActivity().getApplicationContext(), android.R.layout.simple_list_item_1, mItems);
        listView = view.findViewById(R.id.fragment_list_view);
        listView.setAdapter(_Adapter);
        textView = view.findViewById(R.id.fragment_text);
        textView.setText("Empty");

        return view;
    }

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
                 path=new File(FileManager.getInstance().GetApplicationDirectory(),
                        mSelectedGroupKey+" "+mSelectedGroupName);
                ShowFolderFiles(path);
                break;
            case GroupSliderActivity.STRIPPED_FILES:
                 path = new File(FileManager.getInstance().GetApplicationDirectory(),
                        mSelectedGroupKey+" "+mSelectedGroupName);
                ShowFolderFiles(path);
                break;*/

        }

    }


    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void ShowGroupMembers(){
        ManagersMediator.getInstance().GroupMembersRetriever(mSelectedGroupKey, users -> {
            for(String member : (ArrayList<String>) users){
                mItems.add(member);
            }
            if(mItems.isEmpty())
                  textView.setVisibility(View.VISIBLE);
            listView.setAdapter(_Adapter);
        });
    }
    private void ShowFolderFiles(File path){


    }
}
