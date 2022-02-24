package com.example.privatecloudstorage.model;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.privatecloudstorage.controller.FileManagerListActivity;
import com.example.privatecloudstorage.controller.GroupListActivity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

public class FileManagerAdapter extends RecyclerView.Adapter<FileManagerAdapter.ViewHolder> {
    Context _Context;
    File[] mFilesAndFolders;
    String mAction;
    String mGroupName;
    String mGroupKey;

    /**
     * assign the values sent from fileManagerListActivity to the class memebers
     * @param context
     * @param filesAndFolders List contains files and folders that located at specific path
     * @param action Action that will be performed on the file
     * @param groupName The group that the file will be moved to
     */
    public FileManagerAdapter(Context context, File[] filesAndFolders,String action,String groupName,String selectedGroupKey){
        this._Context = context;
        this.mFilesAndFolders = filesAndFolders;
        this.mAction = action;
        this.mGroupName = groupName;
        this.mGroupKey = selectedGroupKey;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(_Context).inflate(R.layout.recycler_item,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File selectedFile = mFilesAndFolders[position];
        holder.textView.setText(selectedFile.getName());

        if(selectedFile.isDirectory()){
            holder.imageView.setImageResource(R.drawable.ic_baseline_folder_24);
        }else{
            holder.imageView.setImageResource(R.drawable.ic_baseline_insert_drive_file_24);
        }

        holder.itemView.setOnClickListener((new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View view) {
                //open the selected directory
                if(selectedFile.isDirectory()){
                    Intent intent = new Intent(_Context, FileManagerListActivity.class);
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
                        openFile(selectedFile.getAbsolutePath(), mAction);
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
                popupMenu.getMenu().add("Send");
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if(item.getTitle().equals("Send")){
                            //the destination that the file will be moved to
                            File dstFile= new File(_Context.getFilesDir() + File.separator + mGroupKey + " " + mGroupName
                                    + File.separator + Uri.fromFile(selectedFile).getLastPathSegment());
                            //copy the file from original directory to group directory
                            try {
                                //copy the file from original directory to group directory
                                Files.copy(selectedFile.toPath() , dstFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                Toast.makeText(_Context.getApplicationContext(),"Sending...",Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(_Context, GroupListActivity.class);
                                _Context.startActivity(intent);
                            } catch (IOException e) {
                                e.printStackTrace();
                                Toast.makeText(_Context.getApplicationContext(),"The Group is not exist",Toast.LENGTH_LONG).show();
                            }
                        }
                        return true;
                    }
                });
                if(!selectedFile.isDirectory() && mAction.equals(Intent.ACTION_GET_CONTENT)){
                    popupMenu.show();
                }
                return true;
            }
        });
    }

    /**
     * open the file and perform action (VIEW or GET_CONTENT)
     * @param filePath
     * @param action (ACTION_VIEW / ACTION_GET_CONTENT)
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void openFile(String filePath, String action) {
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

    @Override
    public int getItemCount() {
        return mFilesAndFolders.length;
    }

    /**
     * view directory/file icon and its name
     */
    public class ViewHolder extends RecyclerView.ViewHolder{
        TextView textView;
        ImageView imageView;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.file_name_text_view);
            imageView = itemView.findViewById(R.id.icon_view);
        }
    }
}
