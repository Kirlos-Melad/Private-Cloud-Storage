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
import java.sql.DataTruncation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;


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

    /**
     * Add a user to the firebase
     * */
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
    @RequiresApi(api = Build.VERSION_CODES.Q)

    private void MonitorConnectionInSingleGroup(String groupId){
        mExecutorService.execute(() -> {
            DatabaseReference connectedRef = mDataBase.getReference(".info/connected");
            connectedRef.addValueEventListener(new ValueEventListener() {
                @RequiresApi(api = Build.VERSION_CODES.Q)
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean connected = snapshot.getValue(Boolean.class);

                    if(connected){
                        // increment the counter if Connected
                        mDataBase.getReference().child("Groups").child(groupId).child("OnlineUsersCounter").setValue(ServerValue.increment(1));
                        // decrement the counter on Disconnection
                        mDataBase.getReference().child("Groups").child(groupId).child("OnlineUsersCounter").onDisconnect().setValue(ServerValue.increment(-1));
                        // if any user disconnects, set AllOnline to false
                        mDataBase.getReference().child("Groups").child(groupId).child("AllOnline").onDisconnect().setValue(false);
                        // set user value as Online
                        mDataBase.getReference().child("Groups").child(groupId).child("Members")
                                .child(ManagersMediator.getInstance().GetCurrentUser().getUid()).setValue("Online");
                        // set user value as Offline on Disconnection
                        mDataBase.getReference().child("Groups").child(groupId).child("Members")
                                .child(ManagersMediator.getInstance().GetCurrentUser().getUid()).onDisconnect().setValue("Offline");
                    }


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
        newGroupReference.child("Description").setValue(group.getDescription());
        newGroupReference.child("Password").setValue(group.getPassword());
        newGroupReference.child("AllOnline").setValue(true);
        newGroupReference.child("OnlineUsersCounter").setValue(0);
        // Add the User as a member
        newGroupReference.child("Members").child(ManagersMediator.getInstance().GetCurrentUser().getUid()).setValue("Online");
        newGroupReference.child("SharedFiles").setValue("NoFile");

        // Add the group to the user
        mDataBase.getReference().child("Users").child(ManagersMediator.getInstance().GetCurrentUser().getUid())
                .child("Groups").child(groupId).setValue(group.getName());

        // Monitor the new group
        mExecutorService.execute(MonitorSingleGroup(group, false));

        return groupId;
    }
    /**
     * @param groupId get the Group's users
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
                                        User membersInformation[] = new User[(int)membersEntity.getChildrenCount()];
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
            put(ManagersMediator.getInstance().GetCurrentUser().getUid(), "Online");
        }});

        //Add the group to the user
        mDataBase.getReference().child("Users").child(ManagersMediator.getInstance().GetCurrentUser().getUid())
                .child("Groups").updateChildren(new HashMap<String,Object>() {{
            put(group.getId(), group.getName());
        }});
        // Monitor the new group
        mExecutorService.execute(MonitorSingleGroup(group,true));


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

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void OfflineUserGroupsRetriever(IAction action, ExecutorService executorService){
        executorService.execute(() -> {
            DatabaseReference groupsRef = mDataBase.getReference().child("Users").child(ManagersMediator.getInstance().GetCurrentUser().getUid())
                    .child("Groups");
            //groupsRef.keepSynced(true);
            groupsRef.get().addOnSuccessListener(dataSnapshot -> {
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

    public void GetMembersIDs(String groupId,IAction action, ExecutorService executorService){
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
                    ),false));
                }
            }
        });
    }

    /**
     * Decides if we should download the file or not
     * prepares the url for downloading whether it's in normal mode or striping
     *
     * @param fileSnapshot snapshot of the shared file
     * @param group group from which the snapshot was taken
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void TakeAction(DataSnapshot fileSnapshot, Group group){
        // Get Location on Cloud and Physical Storage
        Uri cloudLocation = Uri.parse(fileSnapshot.child("URL").getValue(String.class));

        // Download the file
        if(fileSnapshot.child("Mode").hasChild("Striping")) {
            // adding user's id to download his own chunk
            cloudLocation = Uri.parse(fileSnapshot.child("URL").getValue(String.class) + " " + ManagersMediator.getInstance().GetCurrentUser().getUid());
            ManagersMediator.getInstance().FileDownloadProcedure(group, cloudLocation,
                    fileSnapshot.getKey() + " " + ManagersMediator.getInstance().GetCurrentUser().getUid(),
                    FileManager.STRIP);

        }

        else {
            cloudLocation = Uri.parse(fileSnapshot.child("URL").getValue(String.class));

            ManagersMediator.getInstance().FileDownloadProcedure(group, cloudLocation,
                    fileSnapshot.child("Name").getValue(String.class),FileManager.NORMAL);
        }//FirebaseStorageManager.getInstance().Download(group,cloudLocation, fileSnapshot.child("Name").getValue().toString());

        // Add user to SeenBy
        fileSnapshot.child("SeenBy").getRef().updateChildren(new HashMap<String, Object>() {{
            put(ManagersMediator.getInstance().GetCurrentUser().getUid(), ManagersMediator.getInstance().GetCurrentUser().getDisplayName());
        }});
    }

    /**
     * get all shared files in a group
     *
     * @param groupId
     * @param action
     * @param executorService
     */

    public void GetSharedFiles(String groupId, IAction action,ExecutorService executorService){
        executorService.execute(() -> {
            mDataBase.getReference().child("Groups").child(groupId)
                    .child("SharedFiles").get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
                @Override
                public void onSuccess(DataSnapshot dataSnapshot) {
                    ArrayList<SharedFile> filesModes = new ArrayList<>();
                    SharedFile sharedFile = new SharedFile();
                    for (DataSnapshot file:dataSnapshot.getChildren()
                    ) {
                        sharedFile.Id = file.getKey();
                        sharedFile.mode = file.child("Mode").getValue(String.class);
                        sharedFile.Name = file.child("Name").getValue(String.class);
                        sharedFile.Url = file.child("URL").getValue(String.class);
                        filesModes.add(sharedFile);
                    }
                    action.onSuccess(filesModes);
                }
            });
        });
    }

    private ValueEventListener SharedFileNameEventListener(Group group, DataSnapshot sharedFileSnapshot){
        return new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot fileNameSnapshot) {
                try{
                    String previousName = sharedFileSnapshot.child("Name").getValue(String.class);
                    String mode = sharedFileSnapshot.child("Mode").getValue(String.class);
                    String groupFolder = group.getId() + " " + group.getName();
                    File file;
                    if(mode.equals("Normal")){
                        file= new File(FileManager.getInstance().GetApplicationDirectory() + File.separator + groupFolder + File.separator + "Normal Files",
                                previousName);
                    }
                    else{
                        file= new File(FileManager.getInstance().GetApplicationDirectory() + File.separator + groupFolder + File.separator + "Stripped Files",
                                previousName);
                    }
                    File newFile = new File(file.getPath().substring(0, file.getPath().lastIndexOf(File.separator)), fileNameSnapshot.getValue(String.class));
                    file.renameTo(newFile);

                    //String extension = file.toString().substring(file.getPath().lastIndexOf("."),file.toString().length());

                    //FileManager.getInstance().RenameFile(file, fileNameSnapshot.getValue().toString());
                }catch (Exception e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
    }


    private ChildEventListener SharedFilesEventListener(Group group){
        return new ChildEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void onChildAdded(@NonNull DataSnapshot sharedFileSnapshot, @Nullable String previousChildName) {
                mExecutorService.execute(() -> {
                    if(!sharedFileSnapshot.child("SeenBy").hasChild(ManagersMediator.getInstance().GetCurrentUser().getUid()))
                        TakeAction(sharedFileSnapshot, group);


                    sharedFileSnapshot.child("Name").getRef().addValueEventListener(SharedFileNameEventListener(group, sharedFileSnapshot));
                });
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void onChildRemoved(@NonNull DataSnapshot sharedFileSnapshot) {
                mDataBase.getReference().child("Groups").child(group.getId()).child("Name")
                        .get().addOnSuccessListener(dataSnapshot -> {
                    String fileName = sharedFileSnapshot.child("Name").getValue(String.class);
                    String mode = sharedFileSnapshot.child("Mode").getValue(String.class);
                    String groupName = dataSnapshot.getValue(String.class);
                    File file;
                    if(mode.equals("Normal")){
                        file = new File(FileManager.getInstance().GetApplicationDirectory(),
                                group.getId() + " " + groupName + File.separator + "Normal Files" + File.separator +fileName);
                        FileManager.getInstance().DeleteFile(file);
                    }
                    else if(mode.equals("Striping")){
                        file = new File(FileManager.getInstance().GetApplicationDirectory(),
                                group.getId() + " " + groupName + File.separator + "Merged Files" + File.separator +fileName);
                        FileManager.getInstance().DeleteFile(file);

                        File chunk = new File(FileManager.getInstance().GetApplicationDirectory(),
                                group.getId() + " " + groupName + File.separator + "Stripped Files" + File.separator + fileName + " " + ManagersMediator.getInstance().GetCurrentUser().getUid());
                        FileManager.getInstance().DeleteFile(chunk);
                    }
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
     * checks on th Online users Count and copare it with the number of members
     * to check AllOnline or not
     *
     * @return runnable to run the code in your thread
     */

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private Runnable MonitorSingleGroup(Group group, boolean fromJoin){
        return () -> {

                MonitorConnectionInSingleGroup(group.getId());

            mDataBase.getReference().child("Groups").child(group.getId()).child("OnlineUsersCounter").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        long onlineMembers = dataSnapshot.getValue(long.class);

                        mDataBase.getReference().child("Groups").child(group.getId()).child("Members").get().addOnSuccessListener(Snapshot -> {
                            long membersCount = Snapshot.getChildrenCount();

                            if(onlineMembers == membersCount){
                                mDataBase.getReference().child("Groups").child(group.getId()).child("AllOnline").setValue(true);
                            }
                        });
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });

            mDataBase.getReference().child("Groups").child(group.getId()).child("SharedFiles").addChildEventListener(SharedFilesEventListener(group));
            // if all online, execute AllOnline Scenario
            mDataBase.getReference().child("Groups").child(group.getId()).child("AllOnline").addValueEventListener(AllOnlineEventListener(group));

        };
    }

    /**
     * checks if all the members are online or not
     * and execute the suitabl scenario for each case
     *
     * @param group
     * @return
     */

private ValueEventListener AllOnlineEventListener(Group group) {
    return new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            //check if all members are online in the group
            if (snapshot.getValue(boolean.class)) {

                GetSharedFiles(group.getId(), new IAction() {
                    @Override
                    public void onSuccess(Object files) {

                        ArrayList<SharedFile> filesData = (ArrayList<SharedFile>) files;

                        GetMembersIDs(group.getId(), new IAction() {
                            @RequiresApi(api = Build.VERSION_CODES.Q)
                            @Override
                            public void onSuccess(Object memIds) {

                                ArrayList<String> membersIds = (ArrayList<String>) memIds;
                                ArrayList<Uri> filesUris=new ArrayList<Uri>();
                                ArrayList<String> filesNames = new ArrayList<>();
                                // adding members Ids to the url to be able to download there chunks
                                //adding members Ids to the name so I can sort it later on according to the members IDs
                                for (SharedFile file:filesData) {
                                    if (file.mode.equals("Striping")) {
                                        for (String id : membersIds) {
                                            filesUris.add(Uri.parse(file.Url+ " " + id));
                                            filesNames.add(file.Id+" "+id);
                                        }
                                        // removing my chunk's url from the urls list
                                        filesNames.remove(file.Id+ " " +ManagersMediator.getInstance().GetCurrentUser().getUid());
                                        String fileName = file.Name;
                                        ManagersMediator.getInstance().MergeProcedure(group,fileName,filesUris,filesNames,mExecutorService);
                                    }
                                }
                            }
                        },mExecutorService);

                    }
                }, mExecutorService);

            }
            else{
                //deleting merged files when at least one of the group's members is Offline
                File mergedToDelete = new File(FileManager.getInstance().GetApplicationDirectory()+File.separator+
                        group.getId() + " " + group.getName(),"Merged Files");
                File[] filesArray = mergedToDelete.listFiles();
                for(File file:filesArray){
                    file.delete();
                }
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {

        }

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
    public void AddFile(String groupId, String fileId, String fileName,byte Mode, StorageMetadata metadata, IAction action, ExecutorService executorService) {
        executorService.execute(() -> {
            HashMap<String,String>children=new HashMap<String,String>(){{
                put("Mode", Mode==FileManager.NORMAL ? "Normal":"Striping");
                put("Name",fileName);
                put("URL",metadata.getPath().split(" ")[0]);
                put("Md5Hash",metadata.getMd5Hash());
            }};
            mDataBase.getReference().child("Groups").child(groupId).child("SharedFiles")
                    .updateChildren(new HashMap<String,Object>() {{
                        put(fileId, children);
                    }});
            // Add user in seen
            mDataBase.getReference().child("Groups").child(groupId).child("SharedFiles")
                    .child(fileId).child("SeenBy").child(ManagersMediator.getInstance().GetCurrentUser().getUid())
                    .setValue(ManagersMediator.getInstance().GetCurrentUser().getDisplayName());
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
            mDataBase.getReference().child("Groups")
                    .child(groupId).child("SharedFiles").child(fileId).removeValue()
                    .addOnFailureListener(e -> e.printStackTrace());
        });
    }

    /**
     * Rename file on real-time database
     *
     * @param groupId group id
     * @param oldName old name
     * @param newName new name
     * @param action action to be executed on success
     * @param executorService thread to run on
     */
    public void RenameFile(String groupId, String oldName, String newName, IAction action, ExecutorService executorService){
        executorService.execute(() -> {
            //get id to delete the file from physical storage
            mFirebaseDatabaseManager.FindFileId(groupId, oldName, fileId -> {
                executorService.execute(() -> {
                    mDataBase.getReference().child("Groups").child(groupId).child("SharedFiles")
                            .child((String)fileId).child("Name").setValue(newName);
                });
            }, executorService);
        });
    }



    /**
     * TODO:
     *  create functions to retrieve any needed information for file stripping
     */

}
