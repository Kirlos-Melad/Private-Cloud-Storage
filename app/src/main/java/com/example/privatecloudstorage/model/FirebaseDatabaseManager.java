package com.example.privatecloudstorage.model;

//Android Libraries
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;

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
import java.sql.DataTruncation;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
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
    public void SetUserProfilePicture(String StorageLocation){
        mDataBase.getReference().child("Users").child(ManagersMediator.getInstance().GetCurrentUser().getUid()).child("PictureAtStorage").setValue(StorageLocation);

    }
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void SetGroupProfilePicture(String url,String groupId){
        mDataBase.getReference().child("Groups").child(groupId).child("Picture").setValue(url);
    }
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void SetUserName(String name){
        mDataBase.getReference().child("Users").child(ManagersMediator.getInstance().GetCurrentUser().getUid()).child("Name").setValue(name);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void ExitGroup(String groupId){
        String userId = ManagersMediator.getInstance().GetCurrentUser().getUid();

        mDataBase.getReference().child("Groups").child(groupId).child("Members")
                .child(userId).child("Status").onDisconnect().cancel();

        mDataBase.getReference().child("Groups").child(groupId).child("Members").child(userId).removeValue();
        mDataBase.getReference().child("Groups").child(groupId).child("SharedFiles").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot filesId : snapshot.getChildren()){
                    filesId.child("SeenBy").child(userId).getRef().removeValue();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
        mDataBase.getReference().child("Users").child(userId).child("Groups").child(groupId).removeValue();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void GetUserProfileData(IAction action, ExecutorService executorService){
        executorService.execute(()->{
            String userId = ManagersMediator.getInstance().GetCurrentUser().getUid();
            mDataBase.getReference().child("Users").child(userId).get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
                @Override
                public void onSuccess(DataSnapshot dataSnapshot) {
                    HashMap<String, String> userData = new HashMap<>();
                    for(DataSnapshot child : dataSnapshot.getChildren()){
                        if(!child.getKey().equals("Groups")){
                            userData.put(child.getKey(), child.getValue(String.class));
                        }
                    }

                    action.onSuccess(userData);
                }
            });
        });


    }
    public void GetGroupDescription(String groupId,IAction action, ExecutorService executorService){
            mDataBase.getReference().child("Groups").child(groupId).child("Description").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                  action.onSuccess ((String)snapshot.getValue());
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });


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
                        mDataBase.getReference().child("Groups").child(groupId).child("Members").get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
                            @Override
                            public void onSuccess(DataSnapshot dataSnapshot) {
                                if(dataSnapshot.hasChild(ManagersMediator.getInstance().GetCurrentUser().getUid())){
                                    mDataBase.getReference().child("Groups").child(groupId).child("Members")
                                            .child(ManagersMediator.getInstance().GetCurrentUser().getUid()).child("Status").setValue("Online");
                                    // set user value as Offline on Disconnection
                                    mDataBase.getReference().child("Groups").child(groupId).child("Members")
                                            .child(ManagersMediator.getInstance().GetCurrentUser().getUid()).child("Status").onDisconnect().setValue("Offline");
                                }
                            }
                        });
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
        newGroupReference.child("Owner").setValue(ManagersMediator.getInstance().GetCurrentUser().getUid());
        newGroupReference.child("Description").setValue(group.getDescription());
        newGroupReference.child("Password").setValue(group.getPassword());
        newGroupReference.child("Picture").setValue(group.getPicture());
        // Add the User as a member
        newGroupReference.child("Members").child(ManagersMediator.getInstance().GetCurrentUser().getUid()).setValue(ManagersMediator.getInstance().GetCurrentUser().getDisplayName());
        newGroupReference.child("SharedFiles").setValue("NoFile");

        newGroupReference.child("AllOnline").setValue(true);
        newGroupReference.child("OnlineUsersCounter").setValue(0);
        // Add the User as a member
        HashMap<String,Object> memberChildren = new HashMap<String,Object>(){{
            put("Status", "Online");
            put("Vote","NoVote");
        }};
        newGroupReference.child("Members").child(ManagersMediator.getInstance().GetCurrentUser().getUid()).updateChildren(memberChildren);
        newGroupReference.child("SharedFiles").setValue("NoFile");

        // Add the group to the user
        mDataBase.getReference().child("Users").child(ManagersMediator.getInstance().GetCurrentUser().getUid())
                .child("Groups").child(groupId).setValue(group.getName());

        // Monitor the new group
        mExecutorService.execute(MonitorSingleGroup(group));

        return groupId;
    }
    public void RecycledFilesRetriever(String groupId,IAction action ,ExecutorService executorService){
        executorService.execute(() -> {
            mDataBase.getReference().child("Groups").child(groupId)
                    .child("RecycledFiles").get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
                        @Override
                        public void onSuccess(DataSnapshot sharedFiles) {
                            ArrayList<Pair<String,String>>sharedFilesArray=new ArrayList<>();
                            for (DataSnapshot file : sharedFiles.getChildren()){
                                sharedFilesArray.add(new Pair<>(file.getKey(),file.getValue(String.class)));
                            }
                            action.onSuccess(sharedFilesArray);
                        }
                    });
        });
    }


    public void RestoreRecycledFile(String groupId, String fileId,IAction action,ExecutorService executorService) {
        DatabaseReference FilesReference = mDataBase.getReference().child("Files").child(fileId);
        FilesReference.get().addOnSuccessListener(fileSnapshot -> {
            executorService.execute(() -> {
                mDataBase.getReference().child("Groups").child(groupId).child("RecycledFiles").child(fileId).removeValue();

                int versionNumber = (int) fileSnapshot.getChildrenCount();
                versionNumber -= 4;

                DatabaseReference SharedFileReference = mDataBase.getReference().child("Groups")
                        .child(groupId).child("SharedFiles").child(fileId);

                int finalVersionNumber = versionNumber;
                HashMap<String, Object> sharedFileChildren = new HashMap<String, Object>() {{
                    put("URL", fileSnapshot.child("URL").getValue() + "/" + finalVersionNumber);
                    put("Name", fileSnapshot.child(String.valueOf(finalVersionNumber)).child("Name").getValue(String.class));

                    put("Mode", fileSnapshot.child("Mode").getValue(String.class));
                    put("Change", fileSnapshot.child(String.valueOf(finalVersionNumber)).child("Change").getValue(String.class));
                }};

                if (sharedFileChildren.get("Change").equals("Rename")) {
                    sharedFileChildren.put("PreviousName", fileSnapshot.child(String.valueOf(versionNumber - 1)).child("Name").getValue());
                }

                SharedFileReference.updateChildren(sharedFileChildren);

                // MUST be called from main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    action.onSuccess(null);
                });

            });
        });
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

                                            user.mIsBeingKicked = !(membersEntity.child(user.mId).child("Vote").getValue() == null || membersEntity.child(user.mId).child("Vote").getValue().getClass() == String.class);

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
        DatabaseReference memberReference = mDataBase.getReference().child("Groups").child(group.getId())
                .child("Members").child(ManagersMediator.getInstance().GetCurrentUser().getUid());

        memberReference.updateChildren(new HashMap<String,Object>() {{
            put("Status", "Online");
            put("Vote", "NoVote");
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
public void UserSingleGroupRetriever(String groupId,IAction action, ExecutorService executorService){
    executorService.execute(() -> {
        mDataBase.getReference().child("Groups").child(groupId).get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
            @Override
            public void onSuccess(DataSnapshot group) {
                Group g = new Group(
                        group.getKey(),
                        group.child("Name").getValue(String.class),
                        group.child("Description").getValue(String.class),
                        "",
                        group.child("Picture").getValue(String.class)
                );
                g.mOwner=group.child("Owner").getValue().toString();
                action.onSuccess(g);
            }
        });
    });

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
                    //Group newGroup =new Group("","","","");
                    for(DataSnapshot group : dataSnapshot.getChildren()){
                        Group g = new Group(
                                group.getKey(),
                                group.getValue(String.class),
                                "",
                                "",
                                ""
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
                    for (DataSnapshot Member:dataSnapshot.getChildren()) {
                        membersIds.add(Member.getKey());
                    }
                    action.onSuccess(membersIds);
                });
            });
        });

    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void VoteToKick(String groubId , String userId, boolean vote){
        mDataBase.getReference().child("Groups").child(groubId).child("Members").child(userId).child("Vote")
                .updateChildren(new HashMap<String,Object>() {{ put(ManagersMediator.getInstance().GetCurrentUser().getUid(), (boolean)vote); }});
    }

    public void VotedBefore(String groupId, String memId ,IAction action){
        mDataBase.getReference().child("Groups").child(groupId).child("Members").child(memId).child("Vote").get()
                .addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
                    @RequiresApi(api = Build.VERSION_CODES.Q)
                    @Override
                    public void onSuccess(DataSnapshot dataSnapshot) {
                        action.onSuccess(dataSnapshot.hasChild(ManagersMediator.getInstance().GetCurrentUser().getUid()));
                    }
                });
    }

    public void IsNoVote(String groupId, String memId, IAction action){
        mDataBase.getReference().child("Groups").child(groupId).child("Members").child(memId).child("Vote").get()
                .addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
                    @RequiresApi(api = Build.VERSION_CODES.Q)
                    @Override
                    public void onSuccess(DataSnapshot dataSnapshot) {
                        action.onSuccess(dataSnapshot.getValue()==null || dataSnapshot.getValue().getClass() == String.class );
                    }
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
                            "", "",""
                    )));
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
    private void TakeAction(DataSnapshot fileSnapshot, boolean isRename, Group group){
        // Get Location on Cloud and Physical Storage
        Uri cloudLocation;

        if(fileSnapshot.getValue() instanceof String)
            return;
        // if true, rename and exit
        if(isRename){
            String folder = null;
            if(fileSnapshot.child("Mode").getValue().equals("Striping")){
                folder = "Merged Files";
            }
            else if (fileSnapshot.child("Mode").getValue().equals("Normal")){
                folder = "Normal Files";
            }

            File oldFile = new File(FileManager.getInstance().GetApplicationDirectory() + File.separator + group.getId() + " " + group.getName() +
                    File.separator + folder, fileSnapshot.child("PreviousName").getValue(String.class));
            File newFile = new File(FileManager.getInstance().GetApplicationDirectory() + File.separator + group.getId() + " " + group.getName() +
                    File.separator + folder, fileSnapshot.child("Name").getValue(String.class));

            try {
                // rename
                Files.copy(oldFile.toPath(), newFile.toPath());
                oldFile.delete();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return;
        }

        // Download the file
        if(fileSnapshot.child("Mode").getValue().equals("Striping")) {
            // adding user's id to download his own chunk
            cloudLocation = Uri.parse(fileSnapshot.child("URL").getValue(String.class) + "/" + fileSnapshot.getKey() +
                    " " + ManagersMediator.getInstance().GetCurrentUser().getUid());

            ManagersMediator.getInstance().FileDownloadProcedure(group, cloudLocation,
                    fileSnapshot.getKey() + " " + ManagersMediator.getInstance().GetCurrentUser().getUid(),
                    FileManager.STRIP);

        }
        else if(fileSnapshot.child("Mode").getValue().equals("Normal")){
            cloudLocation = Uri.parse(fileSnapshot.child("URL").getValue(String.class) + "/" + fileSnapshot.getKey());
            String fileName = fileSnapshot.child("Name").getValue(String.class);;
            //String physicalLocation = group.getId() + " " + group.getName();
            // Download the file
            ManagersMediator.getInstance().FileDownloadProcedure(group, cloudLocation, fileName,FileManager.NORMAL);
        }//FirebaseStorageManager.getInstance().Download(group,cloudLocation, fileSnapshot.child("Name").getValue().toString());

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
                        file.delete();
                    }
                    else if(mode.equals("Striping")){
                        file = new File(FileManager.getInstance().GetApplicationDirectory(),
                                group.getId() + " " + groupName + File.separator + "Merged Files" + File.separator +fileName);
                        FileManager.getInstance().DeleteFile(file,FileManager.STRIP);

                        File chunk = new File(FileManager.getInstance().GetApplicationDirectory(),
                                group.getId() + " " + groupName + File.separator + "Stripped Files" + File.separator + fileName + " " + ManagersMediator.getInstance().GetCurrentUser().getUid());
                        chunk.delete();
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
    private Runnable MonitorSingleGroup(Group group){
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

            try{
                //Listener if there is running vote to kick user
                mDataBase.getReference().child("Groups").child(group.getId()).child("Members").child(ManagersMediator.getInstance().GetCurrentUser().getUid())
                        .child("Vote").addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot voteSnapshot) {
                                if(voteSnapshot.getValue() == null || voteSnapshot.getValue().getClass() == String.class)
                                    return;

                                mDataBase.getReference().child("Groups").child(group.getId()).child("Members").get()
                                        .addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
                                    @Override
                                    public void onSuccess(DataSnapshot dataSnapshot) {
                                        int membersCount = (int) dataSnapshot.getChildrenCount();
                                        int agreed = 0;
                                        int disagreed = 0;
                                        for(DataSnapshot data : voteSnapshot.getChildren()){
                                            if(data.getValue(boolean.class) == true)
                                                agreed++;

                                            else if(data.getValue(boolean.class) == false)
                                                disagreed++;

                                            if((agreed+disagreed) == membersCount-1){
                                                if(agreed > membersCount/2)
                                                    ExitGroup(group.getId());

                                                else if(disagreed >= membersCount/2){
                                                    mDataBase.getReference().child("Groups").child(group.getId()).child("Members")
                                                            .child(ManagersMediator.getInstance().GetCurrentUser().getUid())
                                                            .child("Vote").setValue("NoVote");
                                                }
                                            }
                                        }
                                    }
                                });
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                            }
                        });
            } catch (Exception e){
                e.printStackTrace();
            }
        };
    }

    public void GetSharedFiles(String groupId, IAction action,ExecutorService executorService){
        executorService.execute(() -> {
            mDataBase.getReference().child("Groups").child(groupId)
                    .child("SharedFiles").get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
                @Override
                public void onSuccess(DataSnapshot dataSnapshot) {
                    ArrayList<UserFile> filesModes = new ArrayList<>();
                    for (DataSnapshot file:dataSnapshot.getChildren()
                    ) {
                        UserFile sharedFile = new UserFile();
                        sharedFile.Id = file.getKey();
                        sharedFile.mode = file.child("Mode").getValue(String.class);
                        UserFileVersion userFileVersion = new UserFileVersion();
                        userFileVersion.Name = file.child("Name").getValue(String.class);
                        sharedFile.VersionInformation.add(userFileVersion);
                        sharedFile.Url = file.child("URL").getValue(String.class);
                        filesModes.add(sharedFile);
                    }
                    action.onSuccess(filesModes);
                }
            });
        });
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

                        ArrayList<UserFile> filesData = (ArrayList<UserFile>) files;

                        GetMembersIDs(group.getId(), new IAction() {
                            @RequiresApi(api = Build.VERSION_CODES.Q)
                            @Override
                            public void onSuccess(Object memIds) {

                                ArrayList<String> membersIds = (ArrayList<String>) memIds;
                                // adding members Ids to the url to be able to download there chunks
                                //adding members Ids to the name so I can sort it later on according to the members IDs
                                for (UserFile file:filesData) {
                                    ArrayList<Uri> filesUris=new ArrayList<Uri>();
                                    ArrayList<String> filesNames = new ArrayList<>();
                                    if (file.mode.equals("Striping")) {
                                        for (String id : membersIds) {
                                            filesUris.add(Uri.parse(file.Url+ "/" + file.Id + " " + id));
                                            filesNames.add(file.Id+" "+id);
                                        }
                                        // removing my chunk's url from the urls list
                                        filesNames.remove(file.Id+ " " +ManagersMediator.getInstance().GetCurrentUser().getUid());
                                        String fileName = file.VersionInformation.get(0).Name;
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
                put("URL",groupId +"/"+ fileId);
                put("Md5Hash",metadata.getMd5Hash());
            }};
            mDataBase.getReference().child("Files")
                    .updateChildren(new HashMap<String,Object>() {{
                        put(fileId, children);
                    }});
            // Add user in seen
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
    public void DeleteFile(String groupId, String fileId,String fileName, IAction action, ExecutorService executorService){
        executorService.execute(() -> {
            mDataBase.getReference().child("Groups").child(groupId)
                    .child("RecycledFiles").child(fileId).setValue(fileName);
            mDataBase.getReference().child("Groups").child(groupId)
                    .child("SharedFiles").child(fileId).removeValue();

           /* VersionNumberRetriever(fileId, versionNumber -> {
                mDataBase.getReference().child("Groups").child(groupId)
                        .child("RecycledFiles").child(fileId).setValue((int)versionNumber - 1);

                mDataBase.getReference().child("Groups").child(groupId)
                        .child("SharedFiles").child(fileId).removeValue();
            }, executorService);*/
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
}

