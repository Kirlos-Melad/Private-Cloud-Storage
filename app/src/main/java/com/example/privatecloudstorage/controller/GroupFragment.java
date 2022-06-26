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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.privatecloudstorage.R;
import com.example.privatecloudstorage.interfaces.IAction;
import com.example.privatecloudstorage.model.FileManager;
import com.example.privatecloudstorage.model.ManagersMediator;
import com.example.privatecloudstorage.model.RecyclerViewItem;
import com.example.privatecloudstorage.model.User;

import java.io.File;
import java.util.ArrayList;
import java.util.Stack;

public class GroupFragment extends Fragment {
    public static String TAG = "GroupFragment";

    private String mSelectedGroupKey;
    private String mSelectedGroupName;
    private View _Fragment;
    private ArrayAdapterView mAdapter;
    private ArrayList<RecyclerViewItem> mItems;
    private RecyclerView _Recyclerview;

    private  ManagersMediator MANAGER_MEDIATOR;
    private byte mTab;
    private TextView _TextView;
    private Stack<File> mParentFolder;
    private File mOpenedFolder;

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
        _Fragment = inflater.inflate(R.layout.recycler_view, container, false);
        _TextView = _Fragment.findViewById(R.id.RecyclerView_Text);

        _Recyclerview = _Fragment.findViewById(R.id.Recyclerview);
        _Recyclerview.setLayoutManager(new LinearLayoutManager(getContext()));

        return _Fragment;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MANAGER_MEDIATOR = ManagersMediator.getInstance();

        Refresh();

        mItems = new ArrayList<>();
        mParentFolder = new Stack<>();
        mSelectedGroupKey = getArguments().getString("selectedGroupKey");
        mSelectedGroupName = getArguments().getString("selectedGroupName");
        mTab = getArguments().getByte("tab");
        String path = FileManager.getInstance().GetApplicationDirectory() +
                File.separator+mSelectedGroupKey + " "+ mSelectedGroupName;

        mOpenedFolder = new File(path);

        switch (mTab) {
            case GroupSliderActivity.MEMBERS:
                ShowGroupMembers();
                break;
            case GroupSliderActivity.NORMAL_FILES:
                ShowFolderFiles(new File(path, "Normal Files"));
                break;
            case GroupSliderActivity.STRIPPED_FILES:
                ShowFolderFiles(new File(path, "Merged Files"));
                break;
        }
    }

    public void Refresh(){
        // TODO: Listen to changes to refresh
        //  Groups / Members / Files
    }

    /**
     * get user groups and display them on list view
     */
    //
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void ShowGroupMembers() {
        ManagersMediator.getInstance().GroupMembersInformationRetriever(mSelectedGroupKey, membersInfo -> {
            for (User member : (User[]) membersInfo) {
                RecyclerViewItem item = new RecyclerViewItem(member.mName, member.mAbout,null, null, null);
                item.mImage=GetResourceUri(R.drawable.person_24);

                mItems.add(item);
            }
            mAdapter = new ArrayAdapterView(mItems);
            _Recyclerview.setAdapter(mAdapter);

            if (mItems.isEmpty()) {
                _TextView.setText("NO MEMBERS TO SHOW");
                _TextView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void ShowFolderFiles(File folder) {
        if(!mParentFolder.isEmpty()){
            RecyclerViewItem item = new RecyclerViewItem(null,null, null, null, null);
            item.mName="..";
            item.mImage=GetResourceUri(R.drawable.ic_baseline_folder_24);
            item._onClickListener=FileExplorerActivity.FolderOnClickListener(new IAction() {
                @Override
                public void onSuccess(Object object) {
                    File file = mParentFolder.pop();
                    mItems.clear();
                    ShowFolderFiles(file);
                }
            });
            mItems.add(item);
        }

        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            for (File file : files) {
                RecyclerViewItem item = new RecyclerViewItem(null,null, null, null, null);
                item.mName = file.getName();
                if (file.isDirectory()) {
                    item._onClickListener = FileExplorerActivity.FolderOnClickListener(new IAction() {
                        @Override
                        public void onSuccess(Object object) {
                            mParentFolder.push(new File(file.getParent()));
                            mItems.clear();
                            ShowFolderFiles(file);
                        }
                    });
                    //Directory Icon
                    item.mImage=GetResourceUri(R.drawable.ic_baseline_folder_24);
                } else {
                    item._onClickListener = FileExplorerActivity.FileOnClickListener((Activity) getContext(),file);
                    if(file.toString().contains(FileManager.getInstance().GetApplicationDirectory())){
                        item._onLongClickListener = FileExplorerActivity.ApplicationFileOnLongClickListener((Activity) getContext(), file,
                                new IAction() {
                                    @Override
                                    public void onSuccess(Object object) {
                                        mItems.clear();
                                        ShowFolderFiles(folder);
                                    }
                                });
                    }else{
                        item._onLongClickListener = FileExplorerActivity.UserFileOnLongClickListener((Activity) getContext(),file);
                    }
                    item.mImage=GetFileItem(file);
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

    private Uri GetFileItem(File file){
        if (file.toString().contains(".pdf")) {
            //pdf Icon
            return GetResourceUri(R.drawable.ic_baseline_picture_as_pdf_24);
        } else if (file.toString().contains(".jpg") || file.toString().contains(".png")) {
            //Image Icon
            return GetResourceUri(R.drawable.ic_baseline_image_24);
        } else if (file.toString().contains(".mp3")) {
            //Audio Icon
            return GetResourceUri(R.drawable.ic_baseline_audiotrack_24);
        } else if (file.toString().contains(".mp4")) {
            //Video Icon
            return GetResourceUri(R.drawable.ic_baseline_video_library_24);
        } else {
            //File Icon
            return GetResourceUri(R.drawable.ic_baseline_insert_drive_file_24);
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