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
    public void SetGroupProfilePicture(Uri physicalPath,String groupId){
                STORAGE_MANAGER.UploadGroupFile(groupId,physicalPath, metaData ->{
                    DATABASE_MANAGER.SetGroupProfilePicture(((StorageMetadata) metaData).getPath(),groupId);
                }, EXECUTOR_SERVICE);
    }*/
    public void UserSingleGroupRetriever(String groupId,IAction action){
            DATABASE_MANAGER.UserSingleGroupRetriever(groupId,action,EXECUTOR_SERVICE);
    }
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void GetUserProfileData(IAction action){
        DATABASE_MANAGER.GetUserProfileData(action,EXECUTOR_SERVICE);
    }
    public void GetGroupDescription(String groupId,IAction action){
        DATABASE_MANAGER.GetGroupDescription(groupId,action,EXECUTOR_SERVICE);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void ExitGroup(String groupId, String groupName){
        DATABASE_MANAGER.ExitGroup(groupId);
        File folder = new File(FILE_MANAGER.GetApplicationDirectory(), groupId + " " + groupName);
        FILE_MANAGER.DeleteDirectory(folder);
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
    public void VoteProcedure(String groupId, String userId, boolean vote){
        DATABASE_MANAGER.VoteToKick(groupId, userId,vote);
    }

    public void VotedRecently(String groupId , String memId, IAction action){
        DATABASE_MANAGER.VotedBefore(groupId , memId, action);
    }

    public void IsNoVote(String groupId , String memId, IAction action){
        DATABASE_MANAGER.IsNoVote(groupId , memId, action);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void StartMonitoring(){
        if(!mMonitoringStarted){
            DATABASE_MANAGER.MonitorGroups();
            AddFileEventListener();
            mMonitoringStarted = true;
        }
    }

    /* =============================================== File Functions ===============================================*/

    public void RestoreRecycledFile(String groupId,String fileId,IAction action){
        EXECUTOR_SERVICE.execute(()->{

            DATABASE_MANAGER.RestoreRecycledFile(groupId,fileId,action,EXECUTOR_SERVICE);

        });
    }
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
            public void onFileChanged(File file,byte mode) {
                if(file.isDirectory())
                    return;

                DATABASE_MANAGER.UserGroupsRetriever(object -> {
                    ArrayList<Group> groups = (ArrayList<Group>) object;
                    String path = file.getAbsolutePath();
                    for(Group group : groups){
                        if (path.contains(group.getId() + " " + group.getName())){
                            FileUploadProcedure(group, file,file.getName(), "Modified", mode);
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
                            FileUploadProcedure(group,file,oldName,"Renamed", mode);
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
    private void FileUploadProcedure(Group group, File file,String fileName, String change, byte mode){
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

                // Get encrypted file location in physical storage
                //Uri fileUri = Uri.fromFile(encryptedFile);

                if (mode == FILE_MANAGER.NORMAL){
                    if(change.equals("New")){
                        // Get encrypted file location in physical storage
                        Uri fileUri = Uri.fromFile(encryptedFile);
                        // Upload the file to cloud Storage
                        STORAGE_MANAGER.UploadGroupFile(group.getId(), fileUri.getLastPathSegment() ,fileUri,0, object -> EXECUTOR_SERVICE.execute(() -> {
                            // Extract information
                            StorageMetadata storageMetadata = (StorageMetadata) object;
                            String groupId = group.getId();
                            String fileName = file.getName();

                            // Add the file to Database
                            DATABASE_MANAGER.AddFile(groupId, (String)fileId, fileName,FILE_MANAGER.NORMAL,storageMetadata, null, EXECUTOR_SERVICE);

                            // Clear temp directory
                            FILE_MANAGER.DeleteFile(encryptedFile,FILE_MANAGER.NORMAL);
                        }), EXECUTOR_SERVICE);
                    }
                    else{
                        // Get encrypted file location in physical storage
                        Uri fileUri = Uri.fromFile(encryptedFile);
                        DATABASE_MANAGER.VersionNumberRetriever((String) fileId,versionNumber->{
                            // Upload the file to cloud Storage
                            STORAGE_MANAGER.UploadGroupFile(group.getId(), (String) fileId, fileUri,(int)versionNumber, object -> EXECUTOR_SERVICE.execute(() -> {
                                // Extract information
                                StorageMetadata storageMetadata = (StorageMetadata) object;
                                String groupId = group.getId();
                                String fileName = file.getName();

                                // Add the file to Database
                                DATABASE_MANAGER.Versioning(fileName,(String) fileId,groupId,change,storageMetadata,null,EXECUTOR_SERVICE);

                                // Clear temp directory
                                FILE_MANAGER.DeleteFile(encryptedFile,FILE_MANAGER.NORMAL);
                            }), EXECUTOR_SERVICE);
                        },EXECUTOR_SERVICE);
                    }
                }
                else if(mode == FILE_MANAGER.STRIP){
                    DATABASE_MANAGER.GetMembersIDs(group.getId(), new IAction() {
                        @Override
                        public void onSuccess(Object memIds) {
                            DATABASE_MANAGER.VersionNumberRetriever((String)fileId, versionNumber ->{
                                try {
                                    ArrayList<String> membersIds = (ArrayList<String>)memIds;
                                    int myIndex = membersIds.indexOf(GetCurrentUser().getUid());

                                    ArrayList<File> splitFiles = FILE_MANAGER.SplitFile(encryptedFile, membersIds);
                                    Collections.swap(splitFiles,0,myIndex);
                                    // Delete encrypted file
                                    FILE_MANAGER.DeleteFile(encryptedFile,FILE_MANAGER.STRIP);

                                    boolean isFirst=true;

                                    if(change.equals("New")) {

                                        for (File chunk : splitFiles) {

                                            Uri chunkUri = Uri.fromFile(chunk);
                                            if (isFirst) {
                                                STORAGE_MANAGER.UploadGroupFile(group.getId(), (String) fileId, chunkUri, 0, object -> EXECUTOR_SERVICE.execute(() -> {
                                                    // Extract information
                                                    StorageMetadata storageMetadata = (StorageMetadata) object;
                                                    String groupId = group.getId();
                                                    String fileName = file.getName();

                                                    // Add the file to Database
                                                    DATABASE_MANAGER.AddFile(groupId, (String) fileId, fileName, FILE_MANAGER.STRIP, storageMetadata, null, EXECUTOR_SERVICE);

                                                    try {
                                                        File path = new File(file.getPath().substring(0, file.getPath().lastIndexOf(File.separator)),
                                                                (String) fileId + " " + membersIds.get(0));
                                                        Files.copy(chunk.toPath(), path.toPath());
                                                        chunk.delete();
                                                        file.delete();
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }
                                                }), EXECUTOR_SERVICE);
                                                isFirst = false;
                                            } else {


                                                STORAGE_MANAGER.UploadGroupFile(group.getId(), (String) fileId, chunkUri, 0, object -> {
                                                    chunk.delete();
                                                }, EXECUTOR_SERVICE);
                                            }
                                        }
                                    }
                                    else{
                                        for (File chunk : splitFiles) {

                                            Uri chunkUri = Uri.fromFile(chunk);
                                            if (isFirst) {
                                                STORAGE_MANAGER.UploadGroupFile(group.getId(), (String) fileId, chunkUri, (int)versionNumber, object -> EXECUTOR_SERVICE.execute(() -> {
                                                    // Extract information
                                                    StorageMetadata storageMetadata = (StorageMetadata) object;
                                                    String groupId = group.getId();
                                                    String fileName = file.getName();

                                                    // Add the file to Database
                                                    //DATABASE_MANAGER.Versioning(fileName,(String) fileId,groupId,change,storageMetadata,null,EXECUTOR_SERVICE);
                                                    DATABASE_MANAGER.Versioning(fileName, (String) fileId, groupId,change , storageMetadata, null, EXECUTOR_SERVICE);

                                                    try {
                                                        File path = new File(file.getPath().substring(0, file.getPath().lastIndexOf(File.separator)),
                                                                (String) fileId + " " + membersIds.get(0));
                                                        Files.copy(chunk.toPath(), path.toPath());
                                                        chunk.delete();
                                                        file.delete();
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }
                                                }), EXECUTOR_SERVICE);
                                                isFirst = false;
                                            } else {
                                                STORAGE_MANAGER.UploadGroupFile(group.getId(), (String) fileId, chunkUri, (int)versionNumber, object -> {
                                                    chunk.delete();
                                                }, EXECUTOR_SERVICE);
                                            }
                                        }
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            },EXECUTOR_SERVICE);
                        }
                    }, EXECUTOR_SERVICE);
                }
            });
        };
    }

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
     * Remove file from storage
     *
     * @param groupId associated group id to the file
     * @param fileName file name
     */
    private void FileRemoveProcedure(String groupId, String fileName){
        DATABASE_MANAGER.FindFileId(groupId, fileName, fileId -> {
            DATABASE_MANAGER.DeleteFile(groupId, (String)fileId,fileName, null, EXECUTOR_SERVICE);

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
    public void RecycledFilesRetriever(String groupId,IAction action ){
        DATABASE_MANAGER.RecycledFilesRetriever(groupId,action,EXECUTOR_SERVICE);
    }
    /**
     * Download all chunks and merge them into Merged Files folder
     *
     * @param group
     * @param toMergeFileName the name we will use to display the merged file
     * @param filesUri the chunks URIs to be downloaded
     * @param executorService
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void MergeProcedure(Group group, String toMergeFileName, ArrayList<Uri> filesUri,ArrayList<String> filesNames, ExecutorService executorService) {
        executorService.execute(() -> {
            //downloading each chunk
            for (Uri uri:filesUri) {
                FileDownloadProcedure(group, uri,
                        uri.toString().substring(uri.toString().lastIndexOf(File.separator)+1,uri.toString().length()),
                        FileManager.STRIP);
            }

            // a file pointing to the striping folder (chunks folder)
            File stripedFilesArr = new File(FileManager.getInstance().GetApplicationDirectory()+File.separator+
                    group.getId() + " " + group.getName(),"Stripped Files");

            // using sleep to have a moment to allow all files to be downloaded before reading them into a list
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // loading chunks to a list
            File[] filesArray = stripedFilesArr.listFiles();
            //the file to be merged into using the merging function later
            File mergedFile = new File(FileManager.getInstance().GetApplicationDirectory()+File.separator+
                    group.getId() + " " + group.getName(),"Merged Files"+File.separator+toMergeFileName);
            ArrayList<File> chunks = new ArrayList<>();

            // getting the files id (between the last '/' and " ")
            String fileId=filesUri.get(0).toString().substring(filesUri.get(0).toString().lastIndexOf(File.separator)+1,
                    filesUri.get(0).toString().lastIndexOf(" "));

            //check for files that start with the same name (but with just different users ID)
            //in case there's different files with totally different names
            for (File checkingFile : filesArray) {
                if (checkingFile.getName().contains(fileId)) {
                    chunks.add(checkingFile);
                }
            }
            // sorting the chunks according to members IDs.. as in the merging function, the chunks are ordered
            // and the resulting file isn't just some garbage
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

                FileManager.getInstance().MergeFiles(chunks,mergedFile);
                //decrypting the merged file

                //Deleting all chunks after merging
                File chunksToDelete = new File(FileManager.getInstance().GetApplicationDirectory()+File.separator+
                        group.getId() + " " + group.getName(),"Stripped Files");
                File[] fileArr = chunksToDelete.listFiles();

                for(File file:fileArr){
                    if(file.getName().contains(GetCurrentUser().getUid())) {
                        continue;
                    }
                    file.delete();
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                FileManager.getInstance().EncryptDecryptFile(mergedFile,mergedFile.getName(),group, Cipher.DECRYPT_MODE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
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
