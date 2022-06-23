package com.example.privatecloudstorage.model;

//Android Libraries
        import android.net.Uri;
        import android.os.Build;
        import android.os.Handler;
        import android.os.Looper;
        import android.util.Log;

//3rd Party Libraries
        import androidx.annotation.NonNull;
        import androidx.annotation.Nullable;
        import androidx.annotation.RequiresApi;

        import com.example.privatecloudstorage.interfaces.IAction;
        import com.google.android.gms.tasks.OnSuccessListener;
        import com.google.firebase.database.ChildEventListener;
        import com.google.firebase.database.DataSnapshot;
        import com.google.firebase.database.DatabaseError;
        import com.google.firebase.database.DatabaseReference;
        import com.google.firebase.database.FirebaseDatabase;
        import com.google.firebase.database.ServerValue;
        import com.google.firebase.database.ValueEventListener;
        import com.google.firebase.storage.StorageMetadata;


//Java Libraries
        import java.io.File;
        import java.io.IOException;
        import java.nio.file.Files;
        import java.text.SimpleDateFormat;
        import java.util.ArrayList;
        import java.util.Date;
        import java.util.HashMap;
        import java.util.Locale;
        import java.util.concurrent.ExecutorService;
        import java.util.concurrent.Executors;


/**
 * Manages Firebase Real-Time Database
 */
public class FirebaseDatabaseManager {
    // Used for debugging
    public static final String TAG = "FirebaseDatabaseManager";

    private static FirebaseDatabaseManager mFirebaseDatabaseManager;
    private final FirebaseDatabase mDataBase;
    private final ExecutorService mExecutorService;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private FirebaseDatabaseManager(){
        mDataBase = FirebaseDatabase.getInstance();
        mExecutorService = Executors.newSingleThreadExecutor();

    }

    /**
     * Create an instance if and only if it's null
     *
     * @return FirebaseDatabaseManager instance
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static FirebaseDatabaseManager getInstance(){
        if(mFirebaseDatabaseManager == null)
            mFirebaseDatabaseManager  = new FirebaseDatabaseManager();

        return mFirebaseDatabaseManager;
    }

    public void AddUser(String UserId, String userName, String email){
        mDataBase.getReference().child("Users").child(UserId).child("Name").setValue(userName);
        mDataBase.getReference().child("Users").child(UserId).child("Email").setValue(email);
        mDataBase.getReference().child("Users").child(UserId).child("ProfilePicture").setValue("NoPicture");
        mDataBase.getReference().child("Users").child(UserId).child("About").setValue("Write something");
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void SetUserProfilePicture(String url){
        mDataBase.getReference().child("Users").child(ManagersMediator.getInstance().GetCurrentUser().getUid()).child("ProfilePicture").setValue(url);
    }
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void SetUserName(String name){
        mDataBase.getReference().child("Users").child(ManagersMediator.getInstance().GetCurrentUser().getUid()).child("Name").setValue(name);
    }


    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void SetUserAbout(String about){
        mDataBase.getReference().child("Users").child(ManagersMediator.getInstance().GetCurrentUser().getUid()).child("About").setValue(about);
    }


    /**
     * Monitor user connection and show it in Real-Time DB
     */
    public void MonitorUserConnection(){
        mExecutorService.execute(() -> {
            DatabaseReference connectedRef = mDataBase.getReference(".info/connected");
            connectedRef.addValueEventListener(new ValueEventListener() {
                @RequiresApi(api = Build.VERSION_CODES.Q)
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean connected = snapshot.getValue(Boolean.class);

                    UserGroupsRetriever(object -> {
                        ArrayList<Group> groups = (ArrayList<Group>) object;

                        for(Group group : groups){
                            String groupId = group.getId();

                            if(connected){
                                // increment the counter
                                mDataBase.getReference().child("Groups").child(groupId).child("OnlineUsersCounter").setValue(ServerValue.increment(1));
                                // set user value as Online
                                mDataBase.getReference().child("Groups").child(groupId).child("Members")
                                        .child(ManagersMediator.getInstance().GetCurrentUser().getUid()).setValue("Online");
                            } else{
                                // decrement the counter
                                mDataBase.getReference().child("Groups").child(groupId).child("OnlineUsersCounter").setValue(ServerValue.increment(-1));
                                // set user value as Offline
                                mDataBase.getReference().child("Groups").child(groupId).child("Members")
                                        .child(ManagersMediator.getInstance().GetCurrentUser().getUid()).setValue("Offline");
                            }
                        }
                    }, mExecutorService);
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.w(TAG, "Listener was cancelled");
                }
            });
        });
    }

    /* =============================================== Group Functions ===============================================*/

    /**
     * Create new group in firebase and add the user to the group
     *
     * @param group The new group to be created
     *
     * @return Group Information [Group ID, Group Name]
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public String AddGroup(Group group){
        DatabaseReference groupsReference = mDataBase.getReference().child("Groups");

        //Generate new group id
        String groupId = groupsReference.push().getKey();
        DatabaseReference newGroupReference = groupsReference.child(groupId);

        // Add group information
        newGroupReference.child("Name").setValue(group.getName());
        newGroupReference.child("Owner").setValue(ManagersMediator.getInstance().GetCurrentUser().getUid());
        newGroupReference.child("Description").setValue(group.getDescription());
        newGroupReference.child("Password").setValue(group.getPassword());
        // Add the User as a member
        newGroupReference.child("Members").child(ManagersMediator.getInstance().GetCurrentUser().getUid()).setValue(ManagersMediator.getInstance().GetCurrentUser().getDisplayName());
        newGroupReference.child("SharedFiles").setValue("NoFile");
        newGroupReference.child("OnlineUsersCounter").setValue(0);

        // Add the group to the user
        mDataBase.getReference().child("Users").child(ManagersMediator.getInstance().GetCurrentUser().getUid())
                .child("Groups").child(groupId).setValue(group.getName());

        // Monitor the new group
        mExecutorService.execute(MonitorSingleGroup(group));

        return groupId;
    }
    /**
     get users name from "Users" based on group id
     **/
    //=====================================================NEW=================================================
    public void GroupMembersInformationRetriever(String groupId, IAction action, ExecutorService executorService){
        executorService.execute(() -> {
            mDataBase.getReference().child("Groups").child(groupId)
                    .child("Members").get()
                    .addOnSuccessListener(membersEntity -> {
                        executorService.execute(() -> {
                            mDataBase.getReference().child("Users").get()
                                    .addOnSuccessListener(usersEntity -> {
                                        User[] membersInformation = new User[(int)membersEntity.getChildrenCount()];
                                        int index = 0;
                                        DataSnapshot firebaseUser;
                                        for (DataSnapshot member : membersEntity.getChildren())
                                        {
                                            firebaseUser = usersEntity.child(member.getKey());
                                            User user = new User();
                                            user.mId = firebaseUser.getKey();
                                            user.mName = firebaseUser.child("Name").getValue(String.class);
                                            user.mAbout = firebaseUser.child("About").getValue(String.class);
                                            user.mProfilePictureUrl = firebaseUser.child("ProfilePicture").getValue(String.class);
                                            membersInformation[index++] = user;
                                        }
                                        action.onSuccess(membersInformation);
                                    });
                        });
                    });
        });
    }


    /**
     * Make user join the group
     *
     * @param group
     *
     * @return True on Success
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public boolean JoinGroup(Group group){
        //Add the User as a member
        mDataBase.getReference().child("Groups").child(group.getId())
                .child("Members").updateChildren(new HashMap<String,Object>() {{
            put(ManagersMediator.getInstance().GetCurrentUser().getUid(), ManagersMediator.getInstance().GetCurrentUser().getDisplayName());
        }});

        //Add the group to the user
        mDataBase.getReference().child("Users").child(ManagersMediator.getInstance().GetCurrentUser().getUid())
                .child("Groups").updateChildren(new HashMap<String,Object>() {{
            put(group.getId(), group.getName());
        }});

        // Monitor the new group
        mExecutorService.execute(MonitorSingleGroup(group));

        return true;
    }

    /**
     * Create an Observable that works on this class thread
     * The observable emits User Groups as Pair<String, String>(ID, Name)
     *
     * @param action action to be executed on success
     * @param executorService thread to run on
     */

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void UserGroupsRetriever(IAction action, ExecutorService executorService){
        executorService.execute(() -> {
            mDataBase.getReference().child("Users").child(ManagersMediator.getInstance().GetCurrentUser().getUid())
                    .child("Groups").get().addOnSuccessListener(dataSnapshot -> {
                executorService.execute(() -> {
                    ArrayList<Group> retGroup = new ArrayList<>();

                    for(DataSnapshot group : dataSnapshot.getChildren()){
                        Group g = new Group(
                                group.getKey(),
                                group.getValue(String.class),
                                "", ""
                        );
                        retGroup.add(g);
                    }
                    // Must run this on main thread to avoid problems
                    new Handler(Looper.getMainLooper()).post(() -> {
                        action.onSuccess(retGroup);
                    });
                });
            });
        });
    }
    /**
     * Create an Observable that works on this class thread
     * The observable emits Group Members as Pair<String, String>(ID, Name)
     *
     * @param action action to be executed on success
     * @param executorService thread to run on
     */

    public void GetMembersIDs(String groupId, IAction action, ExecutorService executorService){
        executorService.execute(() -> {
            mDataBase.getReference().child("Groups").child(groupId)
                    .child("Members").get().addOnSuccessListener(dataSnapshot -> {
                executorService.execute(() -> {
                    ArrayList<String> membersIds = new ArrayList<>();
                    for (DataSnapshot Member:dataSnapshot.getChildren()
                    ) {
                        membersIds.add(Member.getKey());
                    }
                    action.onSuccess(membersIds);
                });

            });
        });
    }




    /**
     * Monitor all user groups changes in cloud
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void MonitorGroups(){
        DatabaseReference databaseReference = mDataBase.getReference();
        databaseReference.child("Users").child(ManagersMediator.getInstance().GetCurrentUser().getUid())
                .child("Groups")
                .get().addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                for(DataSnapshot group : task.getResult().getChildren()){
                    mExecutorService.execute(MonitorSingleGroup(new Group(
                            group.getKey(),
                            group.getValue(String.class),
                            "", ""
                    )));
                }
            }
        });
    }

    /**
     * Decides if we should download the file or not
     *
     * @param fileSnapshot snapshot of the shared file
     * @param group group from which the snapshot was taken
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void TakeAction(DataSnapshot fileSnapshot, boolean isRename, Group group){
        if(fileSnapshot.getValue() instanceof String)
            return;

        if(isRename){
            File oldFile = new File(FileManager.getInstance().GetApplicationDirectory() + File.separator + group.getId() + " " + group.getName() +
                    File.separator + "Normal Files", fileSnapshot.child("PreviousName").getValue(String.class));
            File newFile = new File(FileManager.getInstance().GetApplicationDirectory() + File.separator + group.getId() + " " + group.getName() +
                    File.separator + "Normal Files", fileSnapshot.child("Name").getValue(String.class));

            try {
                Files.copy(oldFile.toPath(), newFile.toPath());
                oldFile.delete();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return;
        }

        Log.d(TAG, "TakeAction: Group ID: " + group.getId() + "==============================");
        Log.d(TAG, "TakeAction: File ID: " + fileSnapshot.getKey() + "==============================");
        // Get Location on Cloud and Physical Storage
        // TODO: Change this according to mode
        Uri cloudLocation = Uri.parse(fileSnapshot.child("URL").getValue(String.class) + "/" + fileSnapshot.getKey());
        String fileName = fileSnapshot.child("Name").getValue(String.class);;
        //String physicalLocation = group.getId() + " " + group.getName();

        // Download the file
        ManagersMediator.getInstance().FileDownloadProcedure(group, cloudLocation, fileName);
        //FirebaseStorageManager.getInstance().Download(group,cloudLocation, fileSnapshot.child("Name").getValue().toString());

        // Add user to SeenBy
        fileSnapshot.child("SeenBy").getRef().updateChildren(new HashMap<String, Object>() {{
            put(ManagersMediator.getInstance().GetCurrentUser().getUid(), ManagersMediator.getInstance().GetCurrentUser().getDisplayName());
        }});
    }

    /**
     * Add listener to the group Shared Files
     *
     * @return runnable to run the code in your thread
     */

    public void GetSharedFileVersions(String fileId, IAction action, ExecutorService executorService){
        executorService.execute(() -> {
            mDataBase.getReference().child("Files").child(fileId)
                    .get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
                @Override
                public void onSuccess(DataSnapshot file) {
                    UserFile userFile = new UserFile();
                    userFile.Id = fileId;
                    userFile.mode = file.child("Mode").getValue(String.class);
                    userFile.Url = file.child("URL").getValue(String.class);

                    Log.d(TAG, "onSuccess: --- Id = " + userFile.Id);
                    Log.d(TAG, "onSuccess: --- mode = " + userFile.mode);
                    Log.d(TAG, "onSuccess: --- Url = " + userFile.Url);

                    for (DataSnapshot version : file.getChildren()) {
                        if(version.getChildrenCount() == 1)
                            continue;

                        UserFileVersion userFileVersion = new UserFileVersion();
                        userFileVersion.date = version.child("Date").getValue(String.class);
                        userFileVersion.Name = version.child("Name").getValue(String.class);
                        userFileVersion.change = version.child("Change").getValue(String.class);
                        Log.d(TAG, "onSuccess: Version Added Date = " + userFileVersion.date);
                        userFile.VersionInformation.add(userFileVersion);
                    }
                    action.onSuccess(userFile);
                }
            });
        });
    }

    private ChildEventListener SharedFilesEventListener(Group group){
        return new ChildEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void onChildAdded(@NonNull DataSnapshot sharedFileSnapshot, @Nullable String previousChildName) {
                mExecutorService.execute(() -> {
                    if (!sharedFileSnapshot.child("SeenBy").hasChild(ManagersMediator.getInstance().GetCurrentUser().getUid()))
                        TakeAction(sharedFileSnapshot, false, group);


                    sharedFileSnapshot.getRef().addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot fileSnapshot) {
                            mDataBase.getReference().child("Groups").child(group.getId())
                                    .child("SharedFiles").child(sharedFileSnapshot.getKey())
                                    .child("Change").get()
                                    .addOnSuccessListener(changeSnapshot ->{
                                                try {
                                                    TakeAction(fileSnapshot, changeSnapshot.getValue(String.class).equals("Renamed"), group);
                                                } catch (Exception e){
                                                    Log.e(TAG, "onDataChange: ", e);
                                                }
                                            }
                                    );
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                });
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot sharedFileSnapshot) {
                mDataBase.getReference().child("Groups").child(group.getId()).child("Name")
                        .get().addOnSuccessListener(dataSnapshot -> {
                    String fileName = sharedFileSnapshot.child("Name").getValue(String.class);
                    //String mode = sharedFileSnapshot.child("Mode").getValue(String.class);
                    String groupName = dataSnapshot.getValue(String.class);
                    File file;
                    file = new File(FileManager.getInstance().GetApplicationDirectory(),
                            group.getId() + " " + groupName + File.separator + "Normal Files" + File.separator +fileName);
                    file.delete();
                    //FileManager.getInstance().DeleteFile(file);
                });
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
    }

    /**
     * Add listener to the group Shared Files
     *
     * @return runnable to run the code in your thread
     */

    private Runnable MonitorSingleGroup(Group group){
        return () -> {
            // Listen to newly added files
            mDataBase.getReference().child("Groups").child(group.getId()).child("SharedFiles").addChildEventListener(SharedFilesEventListener(group));
            mDataBase.getReference().child("Groups").child(group.getId()).child("OnlineUsersCounter").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    long onlineMembers = snapshot.getValue(long.class);
                    mDataBase.getReference().child("Groups").child(group.getId()).child("Members").get().addOnSuccessListener(dataSnapshot -> {
                        long membersCount = dataSnapshot.getChildrenCount();

                        if(onlineMembers == membersCount){
                            //TODO: Start Merging files
                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        };
    }

    /* =============================================== File Functions ===============================================*/

    /**
     * Generate an ID for the new file
     *
     * @param action action to be executed on success
     * @param executorService thread to run on
     */
    public void GenerateNewFileId(IAction action, ExecutorService executorService){
        executorService.execute(() -> {
            String fileId = mDataBase.getReference().child("Files").push().getKey();
            action.onSuccess(fileId);
        });
    }

    /**
     * Find file ID using file name
     *
     * @param groupId the group that owns the file
     * @param fileName file name
     * @param action action to be executed on success
     * @param executorService thread to run on
     */
    public void FindFileId(String groupId, String fileName, IAction action, ExecutorService executorService){
        executorService.execute(() -> {
            mDataBase.getReference().child("Groups")
                    .child(groupId).child("SharedFiles").get().addOnSuccessListener(dataSnapshot -> {
                executorService.execute(() -> {
                    for(DataSnapshot file : dataSnapshot.getChildren()){
                        if(file.child("Name").getValue(String.class).equals(fileName)){
                            action.onSuccess(file.getKey());
                            return;
                        }
                    }
                });
            });
        });
    }

    /**
     * Add the uploaded file to the Real-Time database
     *
     * @param groupId Group ID
     * @param fileId file id
     * @param fileName file name
     * @param metadata Uploaded file Metadata
     * @param action action to be executed on success
     * @param executorService thread to run on
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void AddFile(String groupId, String fileId, String fileName, StorageMetadata metadata, IAction action, ExecutorService executorService) {
        executorService.execute(() -> {
            HashMap<String,String>children=new HashMap<String,String>(){{
                put("URL",groupId +"/"+ fileId);
                //TODO:change value of Mode
                put("Mode","Normal");
                put("Group",groupId);
            }};
            mDataBase.getReference().child("Files")
                    .updateChildren(new HashMap<String,Object>() {{
                        put(fileId, children);
                    }});

            Versioning(fileName,fileId,groupId,"New",metadata,action,executorService);
        });

    }

    /**
     * Delete file from real-time database
     *
     * @param groupId Group ID
     * @param fileId file id
     * @param action action to be executed on success
     * @param executorService thread to run on
     */
    public void DeleteFile(String groupId, String fileId, IAction action, ExecutorService executorService){
        executorService.execute(() -> {
            VersionNumberRetriever(fileId, versionNumber -> {
                mDataBase.getReference().child("Groups").child(groupId)
                        .child("RecycledFiles").child(fileId).setValue((int)versionNumber - 1);

                mDataBase.getReference().child("Groups").child(groupId)
                        .child("SharedFiles").child(fileId).removeValue();
            }, executorService);
        });
    }


    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void Versioning (String fileName,String fileId,String groupId,String change,StorageMetadata metadata,IAction action,ExecutorService executorService){

        //get current date and time
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd_HH:mm:ss", Locale.getDefault());
        String currentDateTime = sdf.format(new Date());

        DatabaseReference FilesReference = mDataBase.getReference().child("Files").child(fileId);
        FilesReference.get().addOnSuccessListener(dataSnapshot -> {
            executorService.execute(() -> {
                int versionNumber= (int) dataSnapshot.getChildrenCount();
                versionNumber -= 3;
                //get info from Files and update SharedFiles

                FilesReference.child(String.valueOf(versionNumber)).child("Name").setValue(fileName);
                FilesReference.child(String.valueOf(versionNumber)).child("Date").setValue(currentDateTime);
                FilesReference.child(String.valueOf(versionNumber)).child("Change").setValue(change);
                //FilesReference.child(String.valueOf(versionNumber)).child("StorageMetadata").setValue((Object)metadata);

                DatabaseReference SharedFileReference = mDataBase.getReference().child("Groups").child(groupId).child("SharedFiles")
                        .child(fileId);

                int finalVersionNumber = versionNumber;
                SharedFileReference.child("SeenBy").removeValue();
                HashMap<String,Object> sharedFileChildren = new HashMap<String,Object>(){{
                    put("URL", dataSnapshot.child("URL").getValue() +"/"+ finalVersionNumber);
                    put("Name", fileName);
                    //TODO:change value of Mode
                    put("Mode", dataSnapshot.child("Mode").getValue(String.class));
                    put("Change", change);
                }};

                //SharedFileReference.child("Mode").setValue(dataSnapshot.child("Mode").getValue());
                if(!change.equals("New")){
                    sharedFileChildren.put("PreviousName", dataSnapshot.child(String.valueOf(versionNumber-1)).child("Name").getValue());
                    //SharedFileReference.child("PreviousName").setValue(dataSnapshot.child(String.valueOf(versionNumber-1)).child("Name").getValue());
                }
                //SharedFileReference.child("URL").setValue(dataSnapshot.child("URL").getValue() +"/"+ String.valueOf(versionNumber));

                SharedFileReference.updateChildren(sharedFileChildren);
                SharedFileReference.child("SeenBy").child(ManagersMediator.getInstance().GetCurrentUser().getUid())
                        .setValue(ManagersMediator.getInstance().GetCurrentUser().getDisplayName());

            });
        });
    }
    public void VersionNumberRetriever(String fileId,IAction action,ExecutorService executorService){
        executorService.execute(()->{
            DatabaseReference FilesReference = mDataBase.getReference().child("Files").child(fileId);
            FilesReference.get().addOnSuccessListener(dataSnapshot -> {
                executorService.execute(() -> {
                    int versionNumber= (int) dataSnapshot.getChildrenCount();
                    versionNumber-=3;
                    action.onSuccess(versionNumber);
                });
            });
        });

    }
    /*public void UpdateUserInfo(String name){
        UserGroupsRetriever(new IAction() {
            @Override
            public void onSuccess(Object object) {
                ArrayList<Group> groups = (ArrayList<Group>) object;
                for(Group group : groups){
                    mDataBase.getReference().child("Groups").child( group.getId()).child("Members")
                            .child(mCurrentUser.getUid()).setValue(name);
                    mDataBase.getReference().child("Groups").child( group.getId()).child("SharedFiles").child().child("SeenBy")
                            .child(mCurrentUser.getUid()).setValue(name);
                }

            }
        });

    }*/

}
