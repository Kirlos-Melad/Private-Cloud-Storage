package com.example.privatecloudstorage.model;

import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.example.privatecloudstorage.interfaces.IAction;
import com.example.privatecloudstorage.interfaces.IFileEventListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageMetadata;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;


public class ManagersMediator {
    // Used for debugging
    public static final String TAG = "ManagersMediator";

    private static ManagersMediator mManagersMediator;

    private final ExecutorService EXECUTOR_SERVICE;

    // All the managers
    private final FirebaseDatabaseManager DATABASE_MANAGER;
    private final FirebaseStorageManager STORAGE_MANAGER;
    private final FirebaseAuthenticationManager AUTHENTICATION_MANAGER;
    private final FileManager FILE_MANAGER;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private ManagersMediator(){
        DATABASE_MANAGER = FirebaseDatabaseManager.getInstance();
        STORAGE_MANAGER = FirebaseStorageManager.getInstance();
        AUTHENTICATION_MANAGER = FirebaseAuthenticationManager.getInstance();
        FILE_MANAGER = FileManager.getInstance();

        EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();

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

    /**
     * Retrieve all groups associated with the current user
     *
     * @param action do something upon retrieval
     */
    public void UserGroupsRetriever(IAction action){
        DATABASE_MANAGER.UserGroupsRetriever(action, EXECUTOR_SERVICE);
    }

    /**
     * Get current user
     *
     * @return current user
     */
    public FirebaseUser GetCurrentUser(){
        return AUTHENTICATION_MANAGER.getCurrentUser();
    }

    /* =============================================== File Functions ===============================================*/

    /**
     * Add Event listener to the file manager
     */
    private void AddFileEventListener(){
        FILE_MANAGER.AddEventListener(new IFileEventListener() {
            @Override
            public void onFileAdded(File file,byte mode) {
                if(file.isDirectory())
                    return;

                DATABASE_MANAGER.UserGroupsRetriever(object -> {
                    ArrayList<Group> groups = (ArrayList<Group>) object;
                    String path = file.getAbsolutePath();
                    for(Group group : groups){
                        if (path.contains(group.getId() + " " + group.getName())){
                            FileUploadProcedure(group,file,true,mode);
                            break;
                        }
                    }
                }, EXECUTOR_SERVICE);
            }

            @Override
            public void onFileRemoved(File file) {
                if(file.isDirectory())
                    return;

                DATABASE_MANAGER.UserGroupsRetriever(object -> {
                    ArrayList<Group> groups = (ArrayList<Group>) object;
                    String path = file.getAbsolutePath();
                    for(Group group : groups){
                        if (path.contains(group.getId() + " " + group.getName())){
                            FileRemoveProcedure(group.getId(), file.getName());
                            break;
                        }
                    }
                }, EXECUTOR_SERVICE);
            }

            @Override
            public void onFileChanged(File file) {
                if(file.isDirectory())
                    return;

                DATABASE_MANAGER.UserGroupsRetriever(object -> {
                    ArrayList<Group> groups = (ArrayList<Group>) object;
                    String path = file.getAbsolutePath();
                    for(Group group : groups){
                        if (path.contains(group.getId() + " " + group.getName())){
                            FileUploadProcedure(group,file,false,FILE_MANAGER.NORMAL);
                            break;
                        }
                    }
                }, EXECUTOR_SERVICE);
            }

            @Override
            public void onFileRenamed(File file, String oldName) {
                if(file.isDirectory())
                    return;

                DATABASE_MANAGER.UserGroupsRetriever(object -> {
                    ArrayList<Group> groups = (ArrayList<Group>) object;
                    String path = file.getAbsolutePath();
                    for(Group group : groups){
                        if (path.contains(group.getId() + " " + group.getName())){
                            FileRenameProcedure(group.getId(), file.getName(), oldName);
                            break;
                        }
                    }
                }, EXECUTOR_SERVICE);
            }
        });
    }

    /**
     * Upload file to the storage
     *
     * @param group associated group to the file
     * @param file the file being uploaded
     * @param isNew true if new file else the file is modified
     */
    private void FileUploadProcedure(Group group, File file, boolean isNew,byte mode){
        EXECUTOR_SERVICE.execute(() -> {
            if(isNew){
                DATABASE_MANAGER.GenerateNewFileId(UploadAction(group, file, true,mode), EXECUTOR_SERVICE);
            }
            else{
                DATABASE_MANAGER.FindFileId(group.getId(), file.getName(), UploadAction(group, file, false,mode), EXECUTOR_SERVICE);
            }
        });
    }

    /**
     * Define the action upon uploading the file
     *
     * @param group associated group to the file
     * @param file the file being uploaded
     * @param isNew true if new file else the file is modified
     *
     * @return  the action to be done upon uploading the file
     */
    private IAction UploadAction(Group group,File file, boolean isNew, byte mode){
        return fileId -> {
            EXECUTOR_SERVICE.execute(() -> {
                File encryptedFile;
                try {
                    encryptedFile = FILE_MANAGER.EncryptDecryptFile(file, (String) fileId, group, Cipher.ENCRYPT_MODE);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                // Get encrypted file location in physical storage
                Uri fileUri = Uri.fromFile(encryptedFile);

                if (mode == FILE_MANAGER.NORMAL){
                    // Upload the file to cloud Storage
                    STORAGE_MANAGER.Upload(group.getId(), fileUri, object -> EXECUTOR_SERVICE.execute(() -> {
                        // Extract information
                        StorageMetadata storageMetadata = (StorageMetadata) object;
                        String groupId = group.getId();
                        String fileName = file.getName();

                        // Add the file to Database
                        DATABASE_MANAGER.AddFile(groupId, (String) fileId, fileName, storageMetadata, null, EXECUTOR_SERVICE);

                        // Clear temp directory
                        FILE_MANAGER.DeleteFile(encryptedFile);
                    }), EXECUTOR_SERVICE);
            }
                else{
                    DATABASE_MANAGER.GetMembersIDs(group.getId(), new IAction() {
                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public void onSuccess(Object memIds) {
                            try {
                                ArrayList<String> membersIds = (ArrayList<String>)memIds;
                                int myIndex = membersIds.indexOf(GetCurrentUser().getUid());
                                Collections.swap(membersIds,0,myIndex);
                                ArrayList<File> splitedFiles = FILE_MANAGER.SplitFile(encryptedFile, membersIds);
                                // Clear temp directory
                                FILE_MANAGER.DeleteFile(encryptedFile);

                                for(int i=0;i<splitedFiles.size();i++){
                                    Uri chunkUri = Uri.fromFile(splitedFiles.get(i));
                                    if(i==0){
                                        STORAGE_MANAGER.Upload(group.getId(), chunkUri, object -> EXECUTOR_SERVICE.execute(() -> {
                                            // Extract information
                                            StorageMetadata storageMetadata = (StorageMetadata) object;
                                            String groupId = group.getId();
                                            String fileName = file.getName();

                                            // Add the file to Database
                                            DATABASE_MANAGER.AddFile(groupId, (String) fileId, fileName, storageMetadata, null, EXECUTOR_SERVICE);

                                        }), EXECUTOR_SERVICE);
                                    }
                                    else{

                                        int finalI = i;
                                        STORAGE_MANAGER.Upload(group.getId(),chunkUri, object ->
                                                // Clear temp directory
                                        {FILE_MANAGER.DeleteFile(splitedFiles.get(finalI));},EXECUTOR_SERVICE);
                                    }
                                }

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }, EXECUTOR_SERVICE);
                }
            });

        };
    }

    /**
     * TODO
     *  normal files in a separate directory from the striped files
     */

    /**
     * Download file from storage
     *
     * @param group associated group to the file
     * @param url download link
     * @param fileName the file is saved by this name
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void FileDownloadProcedure(Group group, Uri url, String fileName) {
        EXECUTOR_SERVICE.execute(() -> {
            String path = FILE_MANAGER.GetApplicationDirectory() + File.separator +
                    group.getId() + " " + group.getName();

            File file = new File(path, fileName);
            STORAGE_MANAGER.Download(url, file,
                    object -> {
                        try {
                            FileManager.getInstance().EncryptDecryptFile(file, fileName, group, Cipher.DECRYPT_MODE);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }, EXECUTOR_SERVICE);
        });
    }

    /**
     * Rename the file
     *
     * @param groupId associated group id to the file
     * @param oldName file old name
     * @param newName file new name
     */
    private void FileRenameProcedure(String groupId, String oldName, String newName){
        DATABASE_MANAGER.RenameFile(groupId, oldName, newName, null, EXECUTOR_SERVICE);
    }

    /**
     * Remove file from storage
     *
     * @param groupId associated group id to the file
     * @param fileName file name
     */
    private void FileRemoveProcedure(String groupId, String fileName){
        DATABASE_MANAGER.FindFileId(groupId, fileName, fileId -> {
                    STORAGE_MANAGER.Delete(groupId, (String)fileId, object -> {
                        DATABASE_MANAGER.DeleteFile(groupId, (String)fileId, null, EXECUTOR_SERVICE);
                    }, EXECUTOR_SERVICE);
                }, EXECUTOR_SERVICE);
    }

    /**
     * TODO:
     *  create private functions to extend other procedures
     *  as the behaviour should change depending on the mode
     */
    public void Download(Uri url, File downloadFile, IAction action, ExecutorService executorService) {
        executorService.execute(() -> {
            DatabaseReference dbReference = DATABASE_MANAGER.getmDataBase().getReference("Users/" + GetCurrentUser().getUid() + "/Groups");

            dbReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Group group = dataSnapshot.getValue(Group.class);
                    String groupid = group.getId();

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        });
    }
}