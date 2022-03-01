package com.example.privatecloudstorage.model;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FileManager {
    // Custom Event Listener
    public interface EventListener {
        void onEvent(int event, File file);
    }

    private static FileManager mFileManager;
    private File mParentDirectory;
    private EventListener mEventListener;
    public static final int CREATE=1;
    public static final int DELETE=2;
    public static final int RENAME=3;

    public static FileManager getInstance(){
        return mFileManager;
    }

    public static void CreateInstance(File parentDirectory,EventListener eventListener){
        if(mFileManager == null)
            mFileManager  = new FileManager(parentDirectory,eventListener);
    }

    private FileManager(File parentDirectory,EventListener eventListener) {
        this.mParentDirectory = parentDirectory;
        mEventListener=eventListener;
    }

    public void RenameFile(String newName , File oldFile , String groupKey , String groupName){
        String extension = oldFile.toString().substring(oldFile.getPath().lastIndexOf("."),oldFile.toString().length());
        File newFile = new File(mParentDirectory+ File.separator
                + groupKey + " " + groupName+ File.separator +newName + extension);
        oldFile.renameTo(newFile);
        mEventListener.onEvent(RENAME,newFile);
    }

    public void CreateFile(File file){
        if(!file.exists()) {
            try {
                file.createNewFile();
                mEventListener.onEvent(CREATE,file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    public String getManagedDirectory(){
        return mParentDirectory.toString();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void CopyFile(Path src, Path dst) throws IOException {
        Files.copy(src , dst,StandardCopyOption.REPLACE_EXISTING);
        mEventListener.onEvent(CREATE,new File(dst.toString()));
    }

    public void CreateDirectory(File directory){
        if(!directory.exists())
            directory.mkdir();
    }

    public void DeleteFile(File file){
        file.delete();
        mEventListener.onEvent(DELETE,file);

    }
}
