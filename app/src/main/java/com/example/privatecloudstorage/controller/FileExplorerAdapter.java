package com.example.privatecloudstorage.controller;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Instrumentation;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ActionMenuView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.ActionBarContextView;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.privatecloudstorage.R;
import com.example.privatecloudstorage.databinding.ActivityGroupContentBinding;
import com.example.privatecloudstorage.model.FileManager;
import com.example.privatecloudstorage.BuildConfig;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.Objects;

public class FileExplorerAdapter extends RecyclerView.Adapter<FileExplorerAdapter.ViewHolder> {
    private static final String TAG = "FileExplorerAdapter";
    private ActivityGroupContentBinding _ActivityGroupContentBinding;
    Activity _Context;
    File[] mFilesAndFolders;
    String mAction;
    String mGroupName;
    String mGroupKey;
    FileManager mFileManager;
    private File mOpenedFile;
    byte mMode;

    /**
     * assign the values sent from fileManagerListActivity to the class memebers
     * @param context
     * @param filesAndFolders List contains files and folders that located at specific path
     * @param action Action that will be performed on the file
     * @param groupName The group that the file will be moved to
     */
    public FileExplorerAdapter(Activity context, File[] filesAndFolders,String action,String groupName,String selectedGroupKey, byte mode){
        this._Context = context;
        this.mFilesAndFolders = filesAndFolders;
        this.mAction = action;
        this.mGroupName = groupName;
        this.mGroupKey = selectedGroupKey;
        mFileManager = FileManager.getInstance();
        mOpenedFile = null;
        mMode = mode;
    }

    public File getOpenedFile() {
        return mOpenedFile;
    }

    public void InvalidateOpenedFileValue() {
        mOpenedFile = null;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(_Context).inflate(R.layout.recycler_item,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File selectedFile = mFilesAndFolders[position];

        holder._TextView.setText(selectedFile.getName());

        if(selectedFile.isDirectory()){
            //Directory Icon
            holder._ImageView.setImageResource(R.drawable.ic_baseline_folder_24);
        }
        else{
            if(selectedFile.toString().contains(".pdf")) {
                //pdf Icon
                holder._ImageView.setImageResource(R.drawable.ic_baseline_picture_as_pdf_24);
            }
            else if(selectedFile.toString().contains(".jpg") || selectedFile.toString().contains(".png")) {
                //Image Icon
                holder._ImageView.setImageResource(R.drawable.ic_baseline_image_24);
            }
            else if(selectedFile.toString().contains(".mp3")){
                //Audio Icon
                holder._ImageView.setImageResource(R.drawable.ic_baseline_audiotrack_24);
            }
            else if(selectedFile.toString().contains(".mp4")){
                //Video Icon
                holder._ImageView.setImageResource(R.drawable.ic_baseline_video_library_24);
            }
            else{
                //File Icon
                holder._ImageView.setImageResource(R.drawable.ic_baseline_insert_drive_file_24);
            }
        }

        holder.itemView.setOnClickListener((new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View view) {
                //open the selected directory
                if(selectedFile.isDirectory()){
                    Intent intent = new Intent(_Context, GroupContentActivity.class);
                    String path = selectedFile.getAbsolutePath();
                    intent.putExtra("path",path);
                    intent.putExtra("action",mAction);
                    intent.putExtra("selectedGroupName",mGroupName);
                    intent.putExtra("selectedGroupKey",mGroupKey);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    _Context.startActivity(intent);
                }else{
                    try {
                        //open the selected file
                        OpenFile(selectedFile.getAbsolutePath(), mAction);
                        mOpenedFile = selectedFile;
                    }catch (Exception e){
                        e.printStackTrace();
                        Toast.makeText(_Context.getApplicationContext(),"Cannot open the file",Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }));

        /**
         * Show Menu
         */
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                PopupMenu popupMenu = new PopupMenu(_Context,v);
                if(!selectedFile.isDirectory() && mAction.equals(Intent.ACTION_GET_CONTENT)) {
                    popupMenu.getMenu().add("Send");
                }
                else if(selectedFile.toString().contains(mGroupKey)) {
                    popupMenu.getMenu().add("Rename");
                    popupMenu.getMenu().add("Delete");
                    if(selectedFile.getName().contains(".txt")) {
                        popupMenu.getMenu().add("Edit");
                    }
                }
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if(item.getTitle().equals("Send")){
                            //the destination that the file will be moved to
                            File dstFile= new File(_Context.getFilesDir() + File.separator + mGroupKey + " " + mGroupName
                                    + File.separator + Uri.fromFile(selectedFile).getLastPathSegment());
                            try {
                                //copy the file from original directory to group directory
                                mFileManager.CopyFile(selectedFile.toPath() , dstFile.toPath(),mMode);
                                Toast.makeText(_Context.getApplicationContext(),"Sending...",Toast.LENGTH_LONG).show();
                            } catch (IOException e) {
                                e.printStackTrace();
                                Toast.makeText(_Context.getApplicationContext(),"The Group is not exist",Toast.LENGTH_LONG).show();
                            }
                        }
                        if(item.getTitle().equals("Rename")){
                            AlertDialog.Builder renameDialog = new AlertDialog.Builder(_Context);
                            renameDialog.setTitle("Rename File :");
                            final EditText fileNameEditText = new EditText(_Context);
                            fileNameEditText.setInputType(InputType.TYPE_CLASS_TEXT);
                            fileNameEditText.setText(selectedFile.toString().
                                    substring((selectedFile.getPath().lastIndexOf(File.separator))+1,selectedFile.toString().length()));
                            fileNameEditText.setSelectAllOnFocus(true);
                            renameDialog.setView(fileNameEditText);
                            renameDialog.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    String newName = fileNameEditText.getText().toString();
                                    mFileManager.RenameFile(selectedFile ,newName);
                                    _Context.recreate();
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
                        if(item.getTitle().equals("Delete")){
                            mFileManager.DeleteFile(selectedFile);
                            _Context.recreate();
                        }
                        if(item.getTitle().equals("Edit")){
                            AlertDialog.Builder editDialog = new AlertDialog.Builder(_Context);
                            final EditText editText = new EditText(_Context);
                            editText.setInputType(InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE);
                            editText.setSingleLine(false);
                            try {
                                byte[] b = Files.readAllBytes(selectedFile.toPath());
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
                                        Files.write(selectedFile.toPath(),bytes);
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

                        return true;
                    }
                });
                popupMenu.show();
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return mFilesAndFolders.length;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void OpenFile(String filePath, String action) throws IOException {
        if(action.equals(Intent.ACTION_GET_CONTENT))
            return;
        File file = new File(filePath);
        FileTime lastModifiedOnOpen = mFileManager.getLastModificationDate(file);
        System.out.println("On opening File : "+lastModifiedOnOpen.toString());
        Uri uri =  FileProvider.getUriForFile(Objects.requireNonNull(_Context.getApplicationContext()), BuildConfig.APPLICATION_ID + ".provider",file);
        String mime = _Context.getContentResolver().getType(uri);
        Intent intent = new Intent();
        intent.setAction(action);
        intent.setDataAndType(uri, mime);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        _Context.startActivityForResult(intent,0);
    }

    /**
     * view directory/file icon and its name
     */
    public class ViewHolder extends RecyclerView.ViewHolder{
        private TextView _TextView;
        private ImageView _ImageView;

        public ViewHolder(View itemView) {
            super(itemView);
            _TextView = itemView.findViewById(R.id.file_name_text_view);
            _ImageView = itemView.findViewById(R.id.icon_view);
        }
    }
}

