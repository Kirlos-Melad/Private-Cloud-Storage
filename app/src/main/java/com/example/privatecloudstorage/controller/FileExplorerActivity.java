package com.example.privatecloudstorage.controller;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.example.privatecloudstorage.BuildConfig;
import com.example.privatecloudstorage.R;
import com.example.privatecloudstorage.databinding.ActivityFileExplorerBinding;
import com.example.privatecloudstorage.interfaces.IAction;
import com.example.privatecloudstorage.model.FileManager;
import com.example.privatecloudstorage.model.FirebaseDatabaseManager;
import com.example.privatecloudstorage.model.RecyclerViewItem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Stack;

public class FileExplorerActivity extends AppCompatActivity {

    private ActivityFileExplorerBinding _ActivityFileExplorerBinding;
    private ArrayList<RecyclerViewItem> mItems;
    private Stack<File> mParentFolder;
    private ArrayAdapterView mAdapter;
    private static String mSelectedGroupName;
    private static String mSelectedGroupKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _ActivityFileExplorerBinding = ActivityFileExplorerBinding.inflate(getLayoutInflater());
        setContentView(_ActivityFileExplorerBinding.getRoot());

        mItems = new ArrayList<>();
        mParentFolder = new Stack<>();
        _ActivityFileExplorerBinding.filesView.setLayoutManager(new LinearLayoutManager(this));

        Bundle bundle = getIntent().getExtras();
        if(bundle == null)
            finish();
        mSelectedGroupName = bundle.getString("selectedGroupName");
        mSelectedGroupKey = bundle.getString("selectedGroupKey");

        String path = Environment.getExternalStorageDirectory().getPath();
        ShowFileExplorer(new File(path));

    }

    private Uri GetResourceUri(int resourceId){
        Resources resources = getResources();
        Uri uri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(resources.getResourcePackageName(resourceId))
                .appendPath(resources.getResourceTypeName(resourceId))
                .appendPath(resources.getResourceEntryName(resourceId))
                .build();

        return uri;
    }

    private void ShowFileExplorer(File FileExplorerPath) {
        if(!mParentFolder.isEmpty()){
            RecyclerViewItem item = new RecyclerViewItem(null,null, null, null, null);
            item.mName="..";
            item.mImage = GetResourceUri(R.drawable.ic_baseline_folder_24);
            item._onClickListener=FileExplorerActivity.FolderOnClickListener(new IAction() {
                @Override
                public void onSuccess(Object object) {
                    File file = mParentFolder.pop();
                    mItems.clear();
                    ShowFileExplorer(file);
                }
            });
            mItems.add(item);
        }

        if (FileExplorerPath.isDirectory()) {
            File[] files = FileExplorerPath.listFiles();
            for (File file : files) {
                RecyclerViewItem item = new RecyclerViewItem(null,null, null, null, null);
                item.mName = file.getName();
                if (file.isDirectory()) {
                    item._onClickListener = FileExplorerActivity.FolderOnClickListener(new IAction() {
                        @Override
                        public void onSuccess(Object object) {
                            mParentFolder.push(new File(file.getParent()));
                            mItems.clear();
                            ShowFileExplorer(file);
                        }
                    });
                    //Directory Icon
                    item.mImage=GetResourceUri(R.drawable.ic_baseline_folder_24);
                } else {
                    item._onClickListener = FileExplorerActivity.FileOnClickListener(this,file);
                    if(file.toString().contains(FileManager.getInstance().GetApplicationDirectory())){
                        item._onLongClickListener = FileExplorerActivity.ApplicationFileOnLongClickListener(this, file,
                                new IAction() {
                                    @Override
                                    public void onSuccess(Object object) {
                                        mItems.clear();
                                        ShowFileExplorer(FileExplorerPath);
                                    }
                                });
                    }else{
                        item._onLongClickListener = FileExplorerActivity.UserFileOnLongClickListener(this,file);
                    }
                    item.mImage=GetFileItem(file);
                }
                mItems.add(item);
            }

            mAdapter = new ArrayAdapterView(mItems, this);
            _ActivityFileExplorerBinding.filesView.setAdapter(mAdapter);

            if (mItems.isEmpty()) {
                _ActivityFileExplorerBinding.nofilesText.setText("NO FILES TO SHOW");
                _ActivityFileExplorerBinding.nofilesText.setVisibility(View.VISIBLE);
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

    public static View.OnClickListener FileOnClickListener(Context context, File file){

        return new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View view) {
                try {
                    //open the selected file
                    Uri uri =  FileProvider.getUriForFile(Objects.requireNonNull(context), BuildConfig.APPLICATION_ID + ".provider",file);
                    String mime = context.getContentResolver().getType(uri);
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, mime);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }catch (Exception e){
                    e.printStackTrace();
                    Toast.makeText(context.getApplicationContext(),"Cannot open the file",Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    public static View.OnClickListener FolderOnClickListener(IAction action){
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                action.onSuccess(null);
            }
        };
    }

    public static View.OnLongClickListener ApplicationFileOnLongClickListener(Activity activity, File file, IAction action){
        Log.d("=======================", activity.getLocalClassName().toString());
        return new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                PopupMenu popupMenu = new PopupMenu(activity,v);
                popupMenu.getMenu().add("Rename");
                popupMenu.getMenu().add("Delete");
                popupMenu.getMenu().add("Download Old Versions");
                if(file.getName().contains(".txt"))
                    popupMenu.getMenu().add("Edit");

                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.Q)
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if(item.getTitle().equals("Rename")){
                            AlertDialog.Builder renameDialog = new AlertDialog.Builder(activity);
                            renameDialog.setTitle("Rename File :");
                            final EditText fileNameEditText = new EditText(activity);
                            fileNameEditText.setInputType(InputType.TYPE_CLASS_TEXT);
                            fileNameEditText.setText(file.toString().
                                    substring((file.getPath().lastIndexOf(File.separator))+1,file.toString().length()));
                            fileNameEditText.setSelectAllOnFocus(true);
                            renameDialog.setView(fileNameEditText);
                            renameDialog.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    String newName = fileNameEditText.getText().toString();
                                    FileManager.getInstance().RenameFile(file ,newName,
                                            file.getParentFile().getName().equals("Normal Files") ? FileManager.NORMAL:FileManager.STRIP);
                                    action.onSuccess(null);
                                }
                            });
                            renameDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.cancel();
                                }
                            });
                            renameDialog.show();
                        }
                        else if(item.getTitle().equals("Delete")){
                            FileManager.getInstance().DeleteFile(file,
                                    file.getParentFile().getName().equals("Normal Files") ? FileManager.NORMAL:FileManager.STRIP);
                            action.onSuccess(null);
                        }
                        else if(item.getTitle().equals("Edit")){
                            //TODO : sync with firebase ---------------------------------------
                            AlertDialog.Builder editDialog = new AlertDialog.Builder(activity);
                            final EditText editText = new EditText(activity);
                            editText.setInputType(InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE);
                            editText.setSingleLine(false);
                            try {
                                byte[] b = Files.readAllBytes(file.toPath());
                                String content = new String(b);
                                editText.setText(content);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            editDialog.setView(editText);
                            editDialog.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    try {
                                        byte[] bytes = editText.getText().toString().getBytes();
                                        Files.write(file.toPath(),bytes);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                            editDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.cancel();
                                }
                            });
                            editDialog.show();
                        }

                        else if(item.getTitle().equals("Download Old Versions")){
                            new FileVersionDownloaderBox(file, activity).ShowDialog();
                        }
                        return true;
                    }
                });
                popupMenu.show();
                return true;
            }
        };
    }

    private static void SelectMode(Activity activity, IAction action) {
        final byte[] mode = {FileManager.NORMAL};
        AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
        dialog.setTitle("Select Mode");
        String[] items = {"Normal Mode","Striping Mode"};
        int checkedItem=0;

        dialog.setSingleChoiceItems(items, checkedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        mode[0] = FileManager.NORMAL;
                        Toast.makeText(activity, "Clicked on Normal", Toast.LENGTH_LONG).show();
                        break;
                    case 1:
                        mode[0] = FileManager.STRIP;
                        Toast.makeText(activity, "Clicked on Striping", Toast.LENGTH_LONG).show();
                        break;
                }
            }
        });
        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                action.onSuccess(mode[0]);
            }
        });
        dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                activity.recreate();
            }
        });
        dialog.show();
    }

    public static View.OnLongClickListener UserFileOnLongClickListener(Context context, File file){

        return new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                PopupMenu popupMenu = new PopupMenu(context,view);
                popupMenu.getMenu().add("Share");
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        //the destination that the file will be moved to
                        File normalDst = new File(context.getFilesDir() + File.separator + mSelectedGroupKey + " "
                                +mSelectedGroupName + File.separator + "Normal Files" + File.separator + Uri.fromFile(file).getLastPathSegment() );

                        File stripDst = new File(context.getFilesDir() + File.separator + mSelectedGroupKey + " "
                                +mSelectedGroupName + File.separator + "Stripped Files" + File.separator + Uri.fromFile(file).getLastPathSegment());

                        SelectMode((Activity) context,new IAction()  {
                            //copy the file from original directory to group directory
                            @RequiresApi(api = Build.VERSION_CODES.Q)
                            @Override
                            public void onSuccess(Object object) {
                                byte mode = (byte) object;
                                if (mode == FileManager.NORMAL) {
                                    try {
                                        FileManager.getInstance().CopyFile(file.toPath(), normalDst.toPath(),FileManager.NORMAL);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        Toast.makeText(context.getApplicationContext(), "The Group is not exist", Toast.LENGTH_LONG).show();
                                    }
                                }
                                if (mode == FileManager.STRIP) {
                                    try {
                                        FileManager.getInstance().CopyFile(file.toPath(), stripDst.toPath(),FileManager.STRIP);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        Toast.makeText(context.getApplicationContext(), "The Group is not exist", Toast.LENGTH_LONG).show();
                                    }
                                }
                            }
                        });
                        return true;
                    }

                });
                popupMenu.show();
                return true;
            };
        };
    }

}