package com.example.privatecloudstorage.controller;

import static android.content.Intent.getIntent;

//import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.example.privatecloudstorage.R;
import android.content.Context;
import android.widget.ListView;

import com.example.privatecloudstorage.model.FirebaseAuthenticationManager;
import com.example.privatecloudstorage.model.FirebaseDatabaseManager;
import com.example.privatecloudstorage.model.Group;
import com.example.privatecloudstorage.model.ManagersMediator;

import java.util.ArrayList;

import me.dm7.barcodescanner.core.ViewFinderView;

public class Fragment3 extends Fragment {
    ArrayList<String> mUsers;
    ArrayAdapter<String> _Adapter;
    ListView listView;
    private String mSelectedGroupKey;

    private FirebaseDatabaseManager mFirebaseDatabaseManager;
    FirebaseAuthenticationManager firebaseAuthenticationManager;
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_3, container, false);

        mFirebaseDatabaseManager = FirebaseDatabaseManager.getInstance();
        firebaseAuthenticationManager=FirebaseAuthenticationManager.getInstance();

        if(getArguments()!=null) {
            mSelectedGroupKey = getArguments().getString("selectedGroupKey1");
        }

        mUsers = new ArrayList<>();
        //connecting _Adapter with the mItems List
        _Adapter = new ArrayAdapter<String>(getActivity(),
                R.layout.fragment_3, R.id.user_list_view, mUsers);
        //setting the ListView Adapter with _Adapter
        listView = view.findViewById(R.id.user_list_view);
        listView.setAdapter(_Adapter);

        ManagersMediator.getInstance().GroupMembersRetriever(mSelectedGroupKey,users -> {
            for(String member : (ArrayList<String>) users){
                mUsers.add(member);
            }
            listView.setAdapter(_Adapter);
        });

        // Inflate the layout for this fragment
        return view;
    }
}