package com.example.privatecloudstorage.model;

import android.net.Uri;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.example.privatecloudstorage.interfaces.IAction;
import com.example.privatecloudstorage.interfaces.IFileEventListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.StorageMetadata;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;


public class ManagersMediator {
    // Used for debugging
    public static final String TAG = "ManagersMediator";

    private static ManagersMediator mManagersMediator;

    private ExecutorService mExecutorService;

    // All the managers
    private FirebaseDatabaseManager mFirebaseDatabaseManager;
    private FirebaseStorageManager mFirebaseStorageManager;
    private FirebaseUser mFirebaseUser;
    private FileManager mFileManager;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private ManagersMediator(){
        mFirebaseDatabaseManager = FirebaseDatabaseManager.getInstance();
        mFirebaseStorageManager = FirebaseStorageManager.getInstance();
        mFirebaseUser = FirebaseAuthenticationManager.getInstance().getCurrentUser();
        mFileManager = FileManager.getInstance();

        mExecutorService = Executors.newSingleThreadExecutor();

        // Listen to directory changes
        AddFileEventListener();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static ManagersMediator getInstance(){
        if(mManagersMediator == null)
            mManagersMediator = new ManagersMediator();

        return mManagersMediator;
    }

    /* =============================================== User Functions ===============================================*/

    public void UserGroupsRetriever(IAction action){
        mFirebaseDatabaseManager.UserGroupsRetriever(action, mExecutorService);
    }

    /* =============================================== File Functions ===============================================*/

    /**
     * Add Event listener to the file manager
     */
    private void AddFileEventListener(){
        mFileManager.AddEventListener(new IFileEventListener() {
            @Override
            public void onFileAdded(File file) {
                if(file.isDirectory())
                    return;

                mFirebaseDatabaseManager.UserGroupsRetriever(object -> {
                    ArrayList<Group> groups = (ArrayList<Group>) object;
                    String path = file.getAbsolutePath();
                    for(Group group : groups){
                        if (path.contains(group.getId() + " " + group.getName())){
                            FileUploadProcedure(group, file, true);
                            break;
                        }
                    }
                }, mExecutorService);
            }

            @Override
            public void onFileRemoved(File file) {
                if(file.isDirectory())
                    return;

                mFirebaseDatabaseManager.UserGroupsRetriever(object -> {
                    ArrayList<Group> groups = (ArrayList<Group>) object;
                    String path = file.getAbsolutePath();
                    for(Group group : groups){
                        if (path.contains(group.getId() + " " + group.getName())){
                            FileRemoveProcedure(group.getId(), file.getName());
                            break;
                        }
                    }
                }, mExecutorService);
            }

            @Override
            public void onFileChanged(File file) {
                if(file.isDirectory())
                    return;

                mFirebaseDatabaseManager.UserGroupsRetriever(object -> {
                    ArrayList<Group> groups = (ArrayList<Group>) object;
                    String path = file.getAbsolutePath();
                    for(Group group : groups){
                        if (path.contains(group.getId() + " " + group.getName())){
                            FileUploadProcedure(group, file, false);
                            break;
                        }
                    }
                }, mExecutorService);
            }

            @Override
            public void onFileRenamed(File file, String oldName) {
                if(file.isDirectory())
                    return;

                mFirebaseDatabaseManager.UserGroupsRetriever(object -> {
                    ArrayList<Group> groups = (ArrayList<Group>) object;
                    String path = file.getAbsolutePath();
                    for(Group group : groups){
                        if (path.contains(group.getId() + " " + group.getName())){
                            FileRenameProcedure(group.getId(), file.getName(), oldName);
                            break;
                        }
                    }
                }, mExecutorService);
            }
        });
    }

    private void FileUploadProcedure(Group group, File file, boolean isNew){
        mExecutorService.execute(() -> {
            if(isNew){
                mFirebaseDatabaseManager.GenerateNewFileId(UploadAction(group, file, true), mExecutorService);
            }
            else{
                mFirebaseDatabaseManager.FindFileId(group.getId(), file.getName(), UploadAction(group, file, false), mExecutorService);
            }
        });
    }

    private IAction UploadAction(Group group, File file, boolean isNew){
        return fileId -> {
            File encryptedFile;
            try {
                encryptedFile = mFileManager.EncryptDecryptFile(file, (String)fileId, group, Cipher.ENCRYPT_MODE);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            // Get encrypted file location in physical storage
            Uri fileUri = Uri.fromFile(encryptedFile);

            // Upload the file to cloud Storage
            mFirebaseStorageManager.Upload(group.getId(), fileUri, object -> mExecutorService.execute(() -> {
                // Extract information
                StorageMetadata storageMetadata = (StorageMetadata) object;
                String groupId = group.getId();
                String fileName = file.getName();

                // Add the file to Database
                mFirebaseDatabaseManager.AddFile(groupId, (String)fileId, fileName, storageMetadata, null, mExecutorService);
            }), mExecutorService);

            // Clear temp directory
            mFileManager.DeleteFile(encryptedFile);
        };
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void FileDownloadProcedure(Group group, Uri url, String fileName) {
        String path = mFileManager.GetApplicationDirectory() + File.separator +
                group.getId() + " " + group.getName();

        File file = new File(path, fileName);
        mFirebaseStorageManager.Download(url, file,
                object -> {
                    try {
                        FileManager.getInstance().EncryptDecryptFile(file, fileName, group, Cipher.DECRYPT_MODE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }, mExecutorService);
    }

    private void FileRenameProcedure(String groupId, String oldName, String newName){
        mFirebaseDatabaseManager.RenameFile(groupId, oldName, newName, null, mExecutorService);
    }

    private void FileRemoveProcedure(String groupId, String fileName){
        mFirebaseDatabaseManager.FindFileId(groupId, fileName, fileId -> {
                    mFirebaseStorageManager.Delete(groupId, (String)fileId, object -> {
                        mFirebaseDatabaseManager.DeleteFile(groupId, (String)fileId, null, mExecutorService);
                    }, mExecutorService);
                }, mExecutorService);
    }
}
