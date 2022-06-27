package com.example.privatecloudstorage.controller;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.RequiresApi;

import com.example.privatecloudstorage.R;
import com.example.privatecloudstorage.model.Group;
import com.example.privatecloudstorage.model.ManagersMediator;
import com.example.privatecloudstorage.model.UserFile;
import com.example.privatecloudstorage.model.UserFileVersion;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class FileVersionDownloaderBox {

    private UserFile mUserFile;
    private Context _Context;
    private Dialog _Dialog;
    private Spinner mDropdownMenu;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public FileVersionDownloaderBox(File file, Context context){
        _Context = context;
        mUserFile = new UserFile();
        _Dialog = CreateDialog(file);
    }

    public void ShowDialog(){
        _Dialog.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private View InitializeView(File file) {
        LayoutInflater inflater = ((Activity)_Context).getLayoutInflater();

        View view = inflater.inflate(R.layout.activity_file_version_downloader_box,null);
        mDropdownMenu = (Spinner)  view.findViewById(R.id.spinner);

        ManagersMediator.getInstance().FileInformationRetriever(
                file.getParentFile().getParentFile().getName().split(" ")[0],
                file.getName(),
                userFile -> {
                    ArrayList<String> dates = new ArrayList<>();

                    mUserFile.Url = ((UserFile)userFile).Url;
                    mUserFile.Id = ((UserFile)userFile).Id;
                    mUserFile.mode = ((UserFile)userFile).mode;
                    mUserFile.VersionInformation = (ArrayList<UserFileVersion>) ((UserFile)userFile).VersionInformation.clone();
                    for(UserFileVersion userFileVersion : ((UserFile)userFile).VersionInformation){
                        if(userFileVersion.date != null)
                            dates.add(userFileVersion.date);
                    }

                    Collections.reverse(dates);
                    mDropdownMenu.setAdapter(new ArrayAdapter<>(_Context, R.layout.support_simple_spinner_dropdown_item,
                            dates));
                }
                );

        return view;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private Dialog CreateDialog(File file) {
        AlertDialog.Builder builder=new AlertDialog.Builder(_Context);

        builder.setView(InitializeView(file)).setTitle(file.getName())
                .setPositiveButton("Download", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // position = version reversed
                        int position = mDropdownMenu.getSelectedItemPosition();
                        int version = mDropdownMenu.getCount() - position - 1;
                        String cloudPath = mUserFile.Url + "/" + version + "/" + mUserFile.Id;
                        File physicalPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                                mUserFile.VersionInformation.get(version).Name);
                        Group group = new Group(
                                file.getParentFile().getParentFile().getName().split(" ")[0],
                                file.getParentFile().getParentFile().getName().split(" ")[1],
                                "", "","");

                        ManagersMediator.getInstance().CustomDownload(group, cloudPath, physicalPath, null);

                        Log.d("TAG", "onClick: ------------" + physicalPath.getPath());
                        Log.d("TAG", "onClick: -----------" + physicalPath.exists());
                    }
                })
                .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });


        return builder.create();
    }
}