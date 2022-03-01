package com.example.privatecloudstorage.controller;

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
import java.util.Objects;

public class GroupContentActivity extends AppCompatActivity {
    private static final String TAG = "GroupContentActivity";
    private ActivityGroupContentBinding _ActivityGroupContentBinding;
    private String mSelectedGroupName;
    private String mSelectedGroupKey;
    private FileManager mFileManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //receive parameters from previous activity(GroupListActivity)
        Bundle bundle = getIntent().getExtras();
        if(bundle == null)
            finish();

        mSelectedGroupName = bundle.getString("selectedGroupName");
        mSelectedGroupKey = bundle.getString("selectedGroupKey");
        mFileManager = FileManager.getInstance();

        super.onCreate(savedInstanceState);
        _ActivityGroupContentBinding = ActivityGroupContentBinding.inflate(getLayoutInflater());
        setContentView(_ActivityGroupContentBinding.getRoot());

        _ActivityGroupContentBinding.fabShareFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkPermission()){
                    Intent intent = new Intent(GroupContentActivity.this, FileExplorerListActivity.class);
                    String path = Environment.getExternalStorageDirectory().getPath();
                    intent.putExtra("path",path);
                    intent.putExtra("action",Intent.ACTION_GET_CONTENT);
                    intent.putExtra("selectedGroupName", mSelectedGroupName);
                    intent.putExtra("selectedGroupKey", mSelectedGroupKey);
                    startActivity(intent);
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
                }
                else requestPermission();
            }
        });

        _ActivityGroupContentBinding.fabCreateTextFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog("Enter File Name :",false);
            }
        });

        _ActivityGroupContentBinding.fabCreateFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog("Enter Folder Name :",true);
            }
        });

        _ActivityGroupContentBinding.fabRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recreate();
            }
        });

        if(checkPermission()) {
            String path = getFilesDir()+ File.separator + mSelectedGroupKey + " " + mSelectedGroupName;
            initAdapter(path,Intent.ACTION_VIEW);
        }
        else requestPermission();
    }

    private void showDialog(String msg , boolean isDir){
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
                if(name.contains(".")){
                    Toast.makeText(GroupContentActivity.this,"Name Field cannot contain dot",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(isDir){
                    //create dir
                    File directory = new File(getFilesDir()+ File.separator + mSelectedGroupKey + " " +
                            mSelectedGroupName+ File.separator ,name);
                    mFileManager.CreateDirectory(directory);
                }
                else{
                    //create txt file
                    File txtFile = new File(getFilesDir()+ File.separator + mSelectedGroupKey + " " +
                            mSelectedGroupName+ File.separator ,name + ".txt");
                    mFileManager.CreateFile(txtFile);
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

    public void initAdapter(String path , String Action){
        File file = new File(path);
        File[] filesAndFolders = file.listFiles();
        if(_ActivityGroupContentBinding.nofilesTextview == null || filesAndFolders.length ==0){
            _ActivityGroupContentBinding.nofilesTextview.setVisibility(View.VISIBLE);
            return;
        }
        _ActivityGroupContentBinding.nofilesTextview.setVisibility(View.INVISIBLE);
        FileExplorerAdapter adapter = new FileExplorerAdapter(GroupContentActivity.this,filesAndFolders,Action,mSelectedGroupName,mSelectedGroupKey);
        _ActivityGroupContentBinding.recyclerView.setLayoutManager(new LinearLayoutManager(GroupContentActivity.this));
        _ActivityGroupContentBinding.recyclerView.setAdapter(adapter);
    }
}
