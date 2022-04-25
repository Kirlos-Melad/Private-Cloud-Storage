package com.example.privatecloudstorage.controller;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.privatecloudstorage.BuildConfig;
import com.example.privatecloudstorage.databinding.ActivityGroupContentBinding;
import com.example.privatecloudstorage.model.FileManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.attribute.FileTime;
import java.util.Objects;

public class GroupContentActivity extends AppCompatActivity {
    private static final String TAG = "GroupContentActivity";
    private ActivityGroupContentBinding _ActivityGroupContentBinding;
    private String mSelectedGroupName;
    private String mSelectedGroupKey;
    private FileManager mFileManager;
    String mFilePath;
    byte mode;
    FileExplorerAdapter mAdapter;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //receive parameters from previous activity(GroupListActivity)
        Bundle bundle = getIntent().getExtras();
        if(bundle == null)
            finish();

        mSelectedGroupName = bundle.getString("selectedGroupName");
        mSelectedGroupKey = bundle.getString("selectedGroupKey");
        mFileManager = FileManager.getInstance();

        mFilePath = getIntent().getStringExtra("path");
        String action =  getIntent().getStringExtra("action");

        super.onCreate(savedInstanceState);
        _ActivityGroupContentBinding = ActivityGroupContentBinding.inflate(getLayoutInflater());
        setContentView(_ActivityGroupContentBinding.getRoot());

        if(mFilePath != null && action != null) {
            initAdapter(mFilePath, action,mode);
            return;
        }

        if(checkPermission()) {
            String path = getFilesDir()+ File.separator + mSelectedGroupKey + " " + mSelectedGroupName;
            initAdapter(path,Intent.ACTION_VIEW,mode);
            _ActivityGroupContentBinding.menu.close(true);
        }
        else requestPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        _ActivityGroupContentBinding.fabShareFile.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View view) {
                _ActivityGroupContentBinding.menu.setVisibility(View.INVISIBLE);
                byte selectedMode = SelectMode();
                if(checkPermission()){
                    String path = Environment.getExternalStorageDirectory().getPath();
                    _ActivityGroupContentBinding.menu.close(true);
                    initAdapter(path,Intent.ACTION_GET_CONTENT,selectedMode);
                }
                else requestPermission();
            }
        });

        _ActivityGroupContentBinding.fabShowQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkPermission()){
                    String path = getFilesDir()+ File.separator + mSelectedGroupKey + " " + mSelectedGroupName
                            + File.separator + mSelectedGroupName +" QR Code.png";
                    ShowQrCode(path);
                    _ActivityGroupContentBinding.menu.close(true);
                }
                else requestPermission();
            }
        });

        _ActivityGroupContentBinding.fabCreateTextFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                _ActivityGroupContentBinding.menu.close(true);
                CreateTxtDialog("Enter File Name :");
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onRestart() {
        super.onRestart();
        try {
            File mSelectedFile = mAdapter.getOpenedFile();
            System.out.println(mSelectedFile);
            if(mSelectedFile != null){
                FileTime lastModificationOnClose = mFileManager.getLastModificationDate(mSelectedFile);
                System.out.println("On closing File : " + lastModificationOnClose.toString());
                mAdapter.InvalidateOpenedFileValue();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void CreateTxtDialog(String msg){
        AlertDialog.Builder dialog = new AlertDialog.Builder(GroupContentActivity.this);
        dialog.setTitle(msg);
        final EditText editText = new EditText(GroupContentActivity.this);
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        dialog.setView(editText);
        dialog.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String name = editText.getText().toString();
                if(TextUtils.isEmpty(name)){
                    Toast.makeText(GroupContentActivity.this,"Name Field cannot be empty",Toast.LENGTH_SHORT).show();
                    return;
                }
                File txtFile = new File(getFilesDir()+ File.separator + mSelectedGroupKey + " " +
                        mSelectedGroupName+ File.separator ,name + ".txt");

                if(txtFile.exists()){
                    ReplaceMsgDialog("Do you want to replace the text file ?",txtFile);
                }
                else
                    try {
                        mFileManager.CreateFile(txtFile);
                        recreate();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        });
        dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        dialog.show();
    }

    private void ReplaceMsgDialog(String msg , File file){
        AlertDialog.Builder replaceDialog = new AlertDialog.Builder(GroupContentActivity.this);
        replaceDialog.setTitle(msg);
        replaceDialog.setPositiveButton("Replace", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                try {
                    mFileManager.CreateFile(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        replaceDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        replaceDialog.show();
    }

    /** Check if permission is granted or not
     * true : if granted
     * false : if not
     * @return true/false
     */
    private boolean checkPermission(){
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(result == PackageManager.PERMISSION_GRANTED)
            return true;
        else
            return false;
    }

    /**
     * request permission if it's not granted
     */
    private void requestPermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE))
            Toast.makeText(this,"Storage Perimission is required, please allow from settings",Toast.LENGTH_SHORT);
        else
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
    }

    private void ShowQrCode(String filePath) {
        File file = new File(filePath);
        Uri uri =  FileProvider.getUriForFile(Objects.requireNonNull(GroupContentActivity.this), BuildConfig.APPLICATION_ID + ".provider",file);
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "image/png");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private byte SelectMode() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(GroupContentActivity.this);
        dialog.setTitle("Select Mode");
        String[] items = {"Normal Mode","Stripping Mode"};
        int checkedItem = 1;
        dialog.setSingleChoiceItems(items, checkedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        mode = mFileManager.NORMAL;
                        Toast.makeText(GroupContentActivity.this, "Clicked on Normal", Toast.LENGTH_LONG).show();
                        break;
                    case 1:
                        mode = mFileManager.STRIP;
                        Toast.makeText(GroupContentActivity.this, "Clicked on Stripping", Toast.LENGTH_LONG).show();
                        break;
                }
            }
        });
        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
        dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                recreate();
            }
        });
        dialog.show();
        return mode;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void initAdapter(String path , String Action,byte mode){
        File file = new File(path);
        File[] filesAndFolders = file.listFiles();
        if(_ActivityGroupContentBinding.nofilesTextview == null || filesAndFolders.length ==0){
            _ActivityGroupContentBinding.nofilesTextview.setVisibility(View.VISIBLE);
            return;
        }
        _ActivityGroupContentBinding.nofilesTextview.setVisibility(View.INVISIBLE);
        mAdapter = new FileExplorerAdapter(GroupContentActivity.this,filesAndFolders,Action,mSelectedGroupName,mSelectedGroupKey,mode);
        _ActivityGroupContentBinding.recyclerView.setLayoutManager(new LinearLayoutManager(GroupContentActivity.this));
        _ActivityGroupContentBinding.recyclerView.setAdapter(mAdapter);
    }
}