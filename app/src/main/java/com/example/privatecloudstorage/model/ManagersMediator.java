package com.example.privatecloudstorage.model;

import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.example.privatecloudstorage.interfaces.IAction;
import com.example.privatecloudstorage.interfaces.IFileEventListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageMetadata;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;
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
        mMonitoringStarted=false;

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
            boolean isCreated = group.CreateGroup();
            if(!isCreated)
                return false;

            return true;
        }

        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public boolean CreateGroup(Group group){
        String groupId = DATABASE_MANAGER.AddGroup(group);
        if (groupId != null) {
            boolean isCreated = group.CreateGroup(groupId);
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
//take uri from profile activity and update in (authantication,update storage,update
    //real time with no picture for now)
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void SetUserProfilePicture(Uri physicalPath){
        AUTHENTICATION_MANAGER.UpdateUserProfileImage(physicalPath, object ->
                STORAGE_MANAGER.UploadUserFile(physicalPath, cloudPath ->{
                    DATABASE_MANAGER.SetUserProfilePicture((String) cloudPath);
                }, EXECUTOR_SERVICE)
        );
    }


    /*@RequiresApi(api = Build.VERSION_CODES.Q)
    //phyicalpath = childuserid child name maugod fe el uploud
    //path eli py5do el Filw file hwa elli elsors ptnzl feh

    public void GetUserProfilePicture(Uri phyicalPath , String fileName ){
        String path;
        path = FILE_MANAGER.GetApplicationDirectory() + File.separator + "Normal Files";
        File file = new File(path, fileName);
        STORAGE_MANAGER.DownloadUserFile(phyicalPath, file,
                object -> {
                }, EXECUTOR_SERVICE);

    }*/

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





    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void StartMonitoring(){
        if(!mMonitoringStarted){
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
                Log.d(TAG, "===========================Im in FileUploadProcedure ONFILEADDED=========================");
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

            @RequiresApi(api = Build.VERSION_CODES.Q)
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

            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void onFileChanged(File file) {
                if(file.isDirectory())
                    return;
                Log.d(TAG, "===========================Im in FileUploadProcedure ONFILECHANGED=========================");
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

            @RequiresApi(api = Build.VERSION_CODES.Q)
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
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void FileUploadProcedure(Group group, File file, boolean isNew, byte mode){
        EXECUTOR_SERVICE.execute(() -> {
            if(isNew){
                DATABASE_MANAGER.GenerateNewFileId(UploadAction(group, file, true,mode), EXECUTOR_SERVICE);
                Log.d(TAG, "===========================Im in FileUploadProcedure isNew=========================");
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
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private IAction UploadAction(Group group, File file, boolean isNew, byte mode){
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
                        STORAGE_MANAGER.UploadGroupFile(group.getId(), fileUri, object -> EXECUTOR_SERVICE.execute(() -> {
                        // Extract information
                        StorageMetadata storageMetadata = (StorageMetadata) object;
                        String groupId = group.getId();
                        String fileName = file.getName();

                        // Add the file to Database
                        DATABASE_MANAGER.AddFile(groupId, (String)fileId, fileName,mode, storageMetadata, null, EXECUTOR_SERVICE);

                        // Clear temp directory
                        FILE_MANAGER.DeleteFile(encryptedFile);
                    }), EXECUTOR_SERVICE);
                }
                else{
                    DATABASE_MANAGER.GetMembersIDs(group.getId(), new IAction() {
                        @Override
                        public void onSuccess(Object memIds) {
                            try {
                                ArrayList<String> membersIds = (ArrayList<String>)memIds;
                                int myIndex = membersIds.indexOf(GetCurrentUser().getUid());
                                Log.d(TAG, "membersID ===================="+membersIds.size()+"=============================");
                                ArrayList<File> splitFiles = FILE_MANAGER.SplitFile(encryptedFile, membersIds);
                                Collections.swap(splitFiles,0,myIndex);
                                // Delete encrypted file
                                FILE_MANAGER.DeleteFile(encryptedFile);

                                boolean isFirst=true;
                                for(File chunk:splitFiles){
                                    Log.d(TAG, "Iteration:::::::::::::::::: "+chunk.toString()+"========================================");
                                    Uri chunkUri = Uri.fromFile(chunk);
                                    if(isFirst){
                                            STORAGE_MANAGER.UploadGroupFile(group.getId(), chunkUri, object -> EXECUTOR_SERVICE.execute(() -> {
                                            // Extract information
                                            StorageMetadata storageMetadata = (StorageMetadata) object;
                                            String groupId = group.getId();
                                            String fileName = file.getName();

                                            // Add the file to Database
                                            DATABASE_MANAGER.AddFile(groupId, (String) fileId, fileName, mode, storageMetadata, null, EXECUTOR_SERVICE);

                                                try {
                                                    File path = new File(file.getPath().substring(0, file.getPath().lastIndexOf(File.separator)),
                                                            (String) fileId + " "+ membersIds.get(0));
                                                    Files.copy(chunk.toPath(), path.toPath());
                                                    chunk.delete();
                                                    FILE_MANAGER.DeleteFile(file);
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            }), EXECUTOR_SERVICE);
                                            isFirst=false;
                                    }
                                    else{


                                        STORAGE_MANAGER.UploadGroupFile(group.getId(),chunkUri, object -> {
                                            chunk.delete();
                                            }, EXECUTOR_SERVICE);
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
    public void FileDownloadProcedure(Group group, Uri url, String fileName, byte mode) {
        EXECUTOR_SERVICE.execute(() -> {
            String path;

            // TODO: implemented in file stripping
            if(mode == FILE_MANAGER.NORMAL){
                path = FILE_MANAGER.GetApplicationDirectory() + File.separator +
                        group.getId() + " " + group.getName() + File.separator + "Normal Files";
            }
            else{
                path = FILE_MANAGER.GetApplicationDirectory() + File.separator +
                        group.getId() + " " + group.getName()+ File.separator + "Stripped Files";
            }

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
            STORAGE_MANAGER.DeleteGroupFile(groupId, (String)fileId, object -> {
                DATABASE_MANAGER.DeleteFile(groupId, (String)fileId, null, EXECUTOR_SERVICE);
            }, EXECUTOR_SERVICE);
        }, EXECUTOR_SERVICE);
    }


    /**
     * TODO:
     *  create private functions to extend other procedures
     *  as the behaviour should change depending on the mode
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void MergeProcedure(Group group, String toMergeFileName, ArrayList<Uri> filesUri,ArrayList<String> filesNames, ExecutorService executorService) {
        executorService.execute(() -> {

            for (Uri uri:filesUri) {
                FileDownloadProcedure(group, uri,
                        uri.toString().substring(uri.toString().lastIndexOf(File.separator)+1,uri.toString().length()), FileManager.STRIP);
            }


                    //Start merging the downloaded chunks while all online
                    File stripedFilesArr = new File(FileManager.getInstance().GetApplicationDirectory()+File.separator+
                            group.getId() + " " + group.getName(),"Stripped Files");

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            File[] filesArray = stripedFilesArr.listFiles();

                    File mergedFile = new File(FileManager.getInstance().GetApplicationDirectory()+File.separator+
                            group.getId() + " " + group.getName(),"Merged Files"+File.separator+toMergeFileName);
                    ArrayList<File> chunks = new ArrayList<>();

                    String fileId=filesUri.get(0).toString().substring(filesUri.get(0).toString().lastIndexOf(File.separator)+1,
                                                                        filesUri.get(0).toString().lastIndexOf(" "));

                    for (File checkingFile : filesArray) {
                        if (checkingFile.getName().contains(fileId)) {
                            chunks.add(checkingFile);
                        }
                    }
                    //sorting the chunks according to members IDs
                    DATABASE_MANAGER.GetMembersIDs(group.getId(), new IAction() {
                        @Override
                        public void onSuccess(Object memIds) {
                            ArrayList<String> membersIds = (ArrayList<String>)memIds;
                            for(int i=0;i<membersIds.size();i++){
                                for (int j=0;j<chunks.size();j++){
                                    if (chunks.get(j).getName().contains(membersIds.get(i))) {
                                        Collections.swap(chunks,i,j);
                                    }
                                }
                            }
                        }
                    },EXECUTOR_SERVICE);

                    try {
                        Log.d(TAG, "MergeProcedure: ============================"+String.valueOf(chunks.size())+ "=========================================");
                        FileManager.getInstance().MergeFiles(chunks,mergedFile);
                        //Deleting all chunks after merging
                        File chunksToDelete = new File(FileManager.getInstance().GetApplicationDirectory()+File.separator+
                                group.getId() + " " + group.getName(),"Stripped Files");
                        File[] fileArr = chunksToDelete.listFiles();
                        for(File file:fileArr){
                            if(file.getName().contains(GetCurrentUser().getUid()))
                                continue;
                            file.delete();
                        }
                        FileManager.getInstance().EncryptDecryptFile(mergedFile,mergedFile.getName(),group, Cipher.DECRYPT_MODE);
                        Log.d(TAG, "MergeProcedure: ====================== DECRYPTION ENDED ===================================");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
        });
    }
}
