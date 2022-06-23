package com.example.privatecloudstorage.model;

import android.net.Uri;
import android.os.Build;
import android.util.Log;

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

    private final ExecutorService EXECUTOR_SERVICE;

    // All the managers
    private final FirebaseDatabaseManager DATABASE_MANAGER;
    private final FirebaseStorageManager STORAGE_MANAGER;
    private final FirebaseAuthenticationManager AUTHENTICATION_MANAGER;
    private final FileManager FILE_MANAGER;

    private boolean mMonitoringStarted;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private ManagersMediator(){
        DATABASE_MANAGER = FirebaseDatabaseManager.getInstance();
        STORAGE_MANAGER = FirebaseStorageManager.getInstance();
        AUTHENTICATION_MANAGER = FirebaseAuthenticationManager.getInstance();
        FILE_MANAGER = FileManager.getInstance();

        EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();
        mMonitoringStarted = false;

        // Listen to directory changes
        //AddFileEventListener();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static ManagersMediator getInstance(){
        if(mManagersMediator == null)
            mManagersMediator = new ManagersMediator();

        return mManagersMediator;
    }

    /* =============================================== User Functions ===============================================*/

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public boolean JoinGroup(Group group){
        if (DATABASE_MANAGER.JoinGroup(group)) {
            boolean isCreated = group.CreateGroup(true);
            if(!isCreated)
                return false;

            return true;
        }

        return false;
    }

    /**
     * Retrieve all groups associated with the current user
     *
     * @param action do something upon retrieval
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void UserGroupsRetriever(IAction action){
        DATABASE_MANAGER.UserGroupsRetriever(action, EXECUTOR_SERVICE);
    }
    public void GroupMembersInformationRetriever(String groupId , IAction action){
        DATABASE_MANAGER.GroupMembersInformationRetriever(groupId,action, EXECUTOR_SERVICE);
    }

    /**
     * Get current user
     *
     * @return current user
     */
    public FirebaseUser GetCurrentUser(){
        return AUTHENTICATION_MANAGER.getCurrentUser();
    }

    public void SignUp(String userName, String email, String password, IAction action){
        boolean signedUp = AUTHENTICATION_MANAGER.SignUp(email, password, userName);
        if (signedUp) {
            // log out and wait for the user to verify his email to login again
            DATABASE_MANAGER.AddUser(AUTHENTICATION_MANAGER.getCurrentUser().getUid(), userName, email);
            AUTHENTICATION_MANAGER.Logout();
            action.onSuccess(null);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void SetUserProfilePicture(Uri physicalPath){
        AUTHENTICATION_MANAGER.UpdateUserProfileImage(physicalPath, object ->
                STORAGE_MANAGER.UploadUserFile(physicalPath, cloudPath ->{
                    DATABASE_MANAGER.SetUserProfilePicture((String) cloudPath);
                }, EXECUTOR_SERVICE)
        );
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void SetUserName(String name){
        AUTHENTICATION_MANAGER.UpdateUserProfileName(name);
        DATABASE_MANAGER.SetUserName(name);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void SetUserAbout(String about){
        DATABASE_MANAGER.SetUserAbout(about);
    }



    //+==================================================================================
    /*@RequiresApi(api = Build.VERSION_CODES.Q)
    public String GetUserAbout(String userId){
       String subTitle= DATABASE_MANAGER.GetUserAbout(userId);
       return subTitle;
    }*/
    //+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++


    public void FileInformationRetriever(String groupId, String fileName, IAction action){
        DATABASE_MANAGER.FindFileId(groupId, fileName, fileId -> {
            DATABASE_MANAGER.GetSharedFileVersions((String)fileId, action, EXECUTOR_SERVICE);
        }, EXECUTOR_SERVICE);
    }



    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void StartMonitoring(){
        if(!mMonitoringStarted){
            DATABASE_MANAGER.MonitorUserConnection();
            DATABASE_MANAGER.MonitorGroups();
            AddFileEventListener();

            mMonitoringStarted = true;
        }
    }

    /* =============================================== File Functions ===============================================*/

    /**
     * Add Event listener to the file manager
     */
    private void AddFileEventListener(){
        FILE_MANAGER.AddEventListener(new IFileEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void onFileAdded(File file, byte mode) {
                if(file.isDirectory())
                    return;

                DATABASE_MANAGER.UserGroupsRetriever(object -> {
                    ArrayList<Group> groups = (ArrayList<Group>) object;
                    String path = file.getAbsolutePath();
                    for(Group group : groups){
                        if (path.contains(group.getId() + " " + group.getName())){
                            FileUploadProcedure(group, file, file.getName(), "New", mode);
                            break;
                        }
                    }
                }, EXECUTOR_SERVICE);
            }

            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void onFileRemoved(File file, byte mode) {
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

            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void onFileChanged(File file, byte mode) {
                if(file.isDirectory())
                    return;

                DATABASE_MANAGER.UserGroupsRetriever(object -> {
                    ArrayList<Group> groups = (ArrayList<Group>) object;
                    String path = file.getAbsolutePath();
                    for(Group group : groups){
                        if (path.contains(group.getId() + " " + group.getName())){
                            FileUploadProcedure(group, file, file.getName(), "Modified", mode);
                            break;
                        }
                    }
                }, EXECUTOR_SERVICE);
            }

            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void onFileRenamed(File file, String oldName, byte mode) {
                if(file.isDirectory())
                    return;

                DATABASE_MANAGER.UserGroupsRetriever(object -> {
                    ArrayList<Group> groups = (ArrayList<Group>) object;
                    String path = file.getAbsolutePath();
                    for(Group group : groups){
                        if (path.contains(group.getId() + " " + group.getName())){
                            FileUploadProcedure(group,file, oldName,"Renamed", mode);
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
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void FileUploadProcedure(Group group, File file, String fileName, String change, byte mode){
        EXECUTOR_SERVICE.execute(() -> {
            if(change.equals("New")){
                DATABASE_MANAGER.GenerateNewFileId(UploadAction(group, file, change,mode), EXECUTOR_SERVICE);
            }
            else{
                DATABASE_MANAGER.FindFileId(group.getId(), fileName, UploadAction(group, file, change,mode), EXECUTOR_SERVICE);
            }
        });
    }

    /**
     * Define the action upon uploading the file
     *
     * @param group associated group to the file
     * @param file the file being uploaded
     *
     * @return  the action to be done upon uploading the file
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private IAction UploadAction(Group group, File file, String change, byte mode){
        return fileId -> {
            EXECUTOR_SERVICE.execute(() -> {
                File encryptedFile;
                try {
                    encryptedFile = FILE_MANAGER.EncryptDecryptFile(file, (String)fileId, group, Cipher.ENCRYPT_MODE);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                if(change.equals("New")){
                    // Get encrypted file location in physical storage
                    Uri fileUri = Uri.fromFile(encryptedFile);
                    // Upload the file to cloud Storage
                    STORAGE_MANAGER.UploadGroupFile(group.getId(), fileUri,0, object -> EXECUTOR_SERVICE.execute(() -> {
                        // Extract information
                        StorageMetadata storageMetadata = (StorageMetadata) object;
                        String groupId = group.getId();
                        String fileName = file.getName();

                        // Add the file to Database
                        DATABASE_MANAGER.AddFile(groupId, (String)fileId, fileName, storageMetadata, null, EXECUTOR_SERVICE);

                        // Clear temp directory
                        FILE_MANAGER.DeleteFile(encryptedFile);
                    }), EXECUTOR_SERVICE);
                }
                else{
                    Log.d(TAG, "UploadAction: YES =======================================");
                    // Get encrypted file location in physical storage
                    Uri fileUri = Uri.fromFile(encryptedFile);
                    DATABASE_MANAGER.VersionNumberRetriever((String) fileId,versionNumber->{
                        // Upload the file to cloud Storage
                        STORAGE_MANAGER.UploadGroupFile(group.getId(), fileUri,(int)versionNumber, object -> EXECUTOR_SERVICE.execute(() -> {
                            // Extract information
                            StorageMetadata storageMetadata = (StorageMetadata) object;
                            String groupId = group.getId();
                            String fileName = file.getName();

                            // Add the file to Database
                            DATABASE_MANAGER.Versioning(fileName,(String) fileId,groupId,change,storageMetadata,null,EXECUTOR_SERVICE);

                            // Clear temp directory
                            FILE_MANAGER.DeleteFile(encryptedFile);
                        }), EXECUTOR_SERVICE);
                    },EXECUTOR_SERVICE);

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
            String path;
            path = FILE_MANAGER.GetApplicationDirectory() + File.separator +
                    group.getId() + " " + group.getName() + File.separator + "Normal Files";

            File file = new File(path, fileName);
            STORAGE_MANAGER.DownloadGroupFile(url, file,
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
     * Remove file from storage
     *
     * @param groupId associated group id to the file
     * @param fileName file name
     */
    private void FileRemoveProcedure(String groupId, String fileName){
        DATABASE_MANAGER.FindFileId(groupId, fileName, fileId -> {
            DATABASE_MANAGER.DeleteFile(groupId, (String)fileId, null, EXECUTOR_SERVICE);

            // DON'T DELETE THE FILE
                /*DATABASE_MANAGER.VersionNumberRetriever((String)fileId, versionNumber -> {
                    //TODO: change according to mode
                    Uri cloudPath = Uri.parse(groupId + "/" + (String)fileId + "/" + ((int)versionNumber - 1) + "/" + fileId);
                    STORAGE_MANAGER.DeleteGroupFile(cloudPath, object -> {
                        DATABASE_MANAGER.DeleteFile(groupId, (String)fileId, null, EXECUTOR_SERVICE);
                }, EXECUTOR_SERVICE);
            }, EXECUTOR_SERVICE);*/
        }, EXECUTOR_SERVICE);
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public void CustomDownload(Group group, String cloudPath, File physicalPath, IAction action) {
        EXECUTOR_SERVICE.execute(() -> {
            STORAGE_MANAGER.DownloadGroupFile(Uri.parse(cloudPath), physicalPath,
                    object -> {
                        try {
                            FileManager.getInstance().EncryptDecryptFile(physicalPath, physicalPath.getName(), group, Cipher.DECRYPT_MODE);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }, EXECUTOR_SERVICE);

        });
    }
}
