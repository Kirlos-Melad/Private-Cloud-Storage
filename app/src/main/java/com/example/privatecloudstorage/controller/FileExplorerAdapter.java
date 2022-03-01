package com.example.privatecloudstorage.controller;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.privatecloudstorage.BuildConfig;
import com.example.privatecloudstorage.R;
import com.example.privatecloudstorage.model.FileManager;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class FileExplorerAdapter extends RecyclerView.Adapter<FileExplorerAdapter.ViewHolder> {
    private static final String TAG = "FileExplorerAdapter";
    Context _Context;
    File[] mFilesAndFolders;
    String mAction;
    String mGroupName;
    String mGroupKey;
    FileManager mFileManager;

    /**
     * assign the values sent from fileManagerListActivity to the class memebers
     * @param context
     * @param filesAndFolders List contains files and folders that located at specific path
     * @param action Action that will be performed on the file
     * @param groupName The group that the file will be moved to
     */
    public FileExplorerAdapter(Context context, File[] filesAndFolders,String action,String groupName,String selectedGroupKey){
        this._Context = context;
        this.mFilesAndFolders = filesAndFolders;
        this.mAction = action;
        this.mGroupName = groupName;
        this.mGroupKey = selectedGroupKey;
        mFileManager = FileManager.getInstance();
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
                    Intent intent = new Intent(_Context, FileExplorerListActivity.class);
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
                        //openFile(selectedFile.getAbsolutePath(), mAction);
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
                else {
                    popupMenu.getMenu().add("Rename");
                    popupMenu.getMenu().add("Delete");
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
                                mFileManager.CopyFile(selectedFile.toPath() , dstFile.toPath());
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
                            renameDialog.setView(fileNameEditText);
                            renameDialog.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    String newName = fileNameEditText.getText().toString();
                                    //ToDo:rename file
                                    mFileManager.RenameFile(newName ,selectedFile , mGroupKey, mGroupName);
                                }
                            });
                            System.out.println(selectedFile);
                            renameDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.cancel();
                                }
                            });
                            renameDialog.show();
                        }
                        if(item.getTitle().equals("Delete")){
                            //TODO:delete from group folder
                            mFileManager.DeleteFile(selectedFile);
                            //TODO:delete from Firebase
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

    public void OpenFile(String filePath, String action) {
        File file = new File(filePath);
        Uri uri =  FileProvider.getUriForFile(Objects.requireNonNull(_Context.getApplicationContext()), BuildConfig.APPLICATION_ID + ".provider",file);
        String mime = _Context.getContentResolver().getType(uri);
        Intent intent = new Intent();
        intent.setAction(action);
        intent.setDataAndType(uri, mime);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if(action.equals(Intent.ACTION_GET_CONTENT))
            return;
        _Context.startActivity(intent);
    }

    /**
     * view directory/file icon and its name
     */
    public class ViewHolder extends RecyclerView.ViewHolder{
        TextView _TextView;
        ImageView _ImageView;

        public ViewHolder(View itemView) {
            super(itemView);
            _TextView = itemView.findViewById(R.id.file_name_text_view);
            _ImageView = itemView.findViewById(R.id.icon_view);
        }
    }
}