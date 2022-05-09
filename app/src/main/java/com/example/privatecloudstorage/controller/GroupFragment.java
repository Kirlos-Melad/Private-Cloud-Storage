package com.example.privatecloudstorage.controller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
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
import com.example.privatecloudstorage.interfaces.IAction;
import com.example.privatecloudstorage.model.FileManager;
import com.example.privatecloudstorage.model.ManagersMediator;
import com.example.privatecloudstorage.model.RecyclerViewItem;

import java.io.File;
import java.util.ArrayList;

import me.dm7.barcodescanner.core.ViewFinderView;

public class GroupFragment extends Fragment {

    private String mSelectedGroupKey;
    private String mSelectedGroupName;
    private View _Fragment;
    private ArrayAdapterView mAdapter;
    private ArrayList<RecyclerViewItem> mItems;
    private byte mTab;
    private RecyclerView _Recyclerview;
    private TextView _TextView;
    //RecyclerView.LayoutManager _layoutManager;

    /**
     * initialize view
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return fragment group layout
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        _Fragment = inflater.inflate(R.layout.fragment_group, container, false);
        _TextView = _Fragment.findViewById(R.id.fragment_text);

        _Recyclerview = _Fragment.findViewById(R.id.recycler_view);
        _Recyclerview.setLayoutManager(new LinearLayoutManager(getContext()));

        return _Fragment;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mItems = new ArrayList<>();
        mSelectedGroupKey = getArguments().getString("selectedGroupKey");
        mSelectedGroupName = getArguments().getString("selectedGroupName");
        mTab = getArguments().getByte("tab");
        File path;
        switch (mTab) {
            case GroupSliderActivity.MEMBERS:
                ShowGroupMembers();
                break;
            //TODO:show group content activity inside fragment
            case GroupSliderActivity.NORMAL_FILES:
                ShowFolderFiles(new File(FileManager.getInstance().GetApplicationDirectory()+
                        File.separator+mSelectedGroupKey+" "+mSelectedGroupName+
                        File.separator+"Normal Files"));
                break;
            case GroupSliderActivity.STRIPPED_FILES:
                ShowFolderFiles(new File(FileManager.getInstance().GetApplicationDirectory()+
                        File.separator+mSelectedGroupKey+" "+mSelectedGroupName+
                        File.separator+"Merged Files"));
                break;
        }
    }

    /**
     * display fragment
     *
     * @param savedInstanceState
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    /**
     * get user groups and display them on list view
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void ShowGroupMembers() {

        //TODO : Members doesn't work
        ManagersMediator.getInstance().GroupMembersRetriever(mSelectedGroupKey, users -> {
            for (String member : (ArrayList<String>) users) {
                mItems.add(new RecyclerViewItem(member, null, null, null));
            }
            mAdapter = new ArrayAdapterView(mItems);
            _Recyclerview.setAdapter(mAdapter);

            //_ActivityGroupContentBinding.recyclerView.setLayoutManager(new LinearLayoutManager(GroupContentActivity.this));
            // _ActivityGroupContentBinding.recyclerView2.setAdapter(adapter);
            if (mItems.isEmpty()) {
                _TextView.setText("NO MEMBERS TO SHOW");
                _TextView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void ShowFolderFiles(File folder) {
        if (folder.isFile()) {
        } else if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            for (File file : files) {
                RecyclerViewItem item = new RecyclerViewItem(null, null, null, null);
                item.mName = file.getName();
                if (file.isDirectory()) {
                    item._onClickListener = FileExplorerActivity.FolderOnClickListener(new IAction() {
                        @Override
                        public void onSuccess(Object object) {
                            //TODO : use stack to do back
                            mItems.clear();
                            ShowFolderFiles(file);
                        }
                    });
                    //Directory Icon
                    item.mImage=GetResourceUri(R.drawable.ic_baseline_folder_24);
                } else {
                    item._onClickListener = FileExplorerActivity.FileOnClickListener((Activity) getContext(),file);
                    if(file.toString().contains(FileManager.getInstance().GetApplicationDirectory())){
                        item._onLongClickListener = FileExplorerActivity.ApplicationFileOnLongClickListener((Activity) getContext(),file);
                    }else{
                        item._onLongClickListener = FileExplorerActivity.UserFileOnLongClickListener((Activity) getContext(),file);
                    }
                    if (file.toString().contains(".pdf")) {
                        //pdf Icon
                        item.mImage=GetResourceUri(R.drawable.ic_baseline_picture_as_pdf_24);
                    } else if (file.toString().contains(".jpg") || file.toString().contains(".png")) {
                        //Image Icon
                        item.mImage=GetResourceUri(R.drawable.ic_baseline_image_24);
                    } else if (file.toString().contains(".mp3")) {
                        //Audio Icon
                        item.mImage=GetResourceUri(R.drawable.ic_baseline_audiotrack_24);
                    } else if (file.toString().contains(".mp4")) {
                        //Video Icon
                        item.mImage=GetResourceUri(R.drawable.ic_baseline_video_library_24);
                    } else {
                        //File Icon
                        item.mImage=GetResourceUri(R.drawable.ic_baseline_insert_drive_file_24);
                    }
                }
                mItems.add(item);
            }

            mAdapter = new ArrayAdapterView(mItems);
            _Recyclerview.setAdapter(mAdapter);

            if (mItems.isEmpty()) {
                _TextView.setText("NO FILES TO SHOW");
                _TextView.setVisibility(View.VISIBLE);
            }
        }
    }

    private Uri GetResourceUri(int resourceId){
        Resources resources = getContext().getResources();
        Uri uri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(resources.getResourcePackageName(resourceId))
                .appendPath(resources.getResourceTypeName(resourceId))
                .appendPath(resources.getResourceEntryName(resourceId))
                .build();

        return uri;
    }
}