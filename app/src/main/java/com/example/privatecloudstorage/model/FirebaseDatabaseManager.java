package com.example.privatecloudstorage.model;

//Android Libraries
        import android.hardware.biometrics.BiometricPrompt;
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
        import com.google.android.gms.tasks.Task;
        import com.google.firebase.auth.FirebaseUser;
        import com.google.firebase.database.ChildEventListener;
        import com.google.firebase.database.DataSnapshot;
        import com.google.firebase.database.DatabaseError;
        import com.google.firebase.database.DatabaseReference;
        import com.google.firebase.database.FirebaseDatabase;
        import com.google.firebase.database.ValueEventListener;
        import com.google.firebase.storage.StorageMetadata;


//Java Libraries
        import java.io.File;
        import java.util.ArrayList;
        import java.util.HashMap;
        import java.util.List;
        import java.util.Map;
        import java.util.concurrent.ExecutorService;
        import java.util.concurrent.Executors;

        import io.reactivex.Observable;
        import io.reactivex.Observer;
        import io.reactivex.disposables.Disposable;
        import io.reactivex.schedulers.Schedulers;


/**
 * Manages Firebase Real-Time Database
 */
public class FirebaseDatabaseManager {
    // Used for debugging
    public static final String TAG = "FirebaseDatabaseManager";

    private static FirebaseDatabaseManager mFirebaseDatabaseManager;

    public FirebaseDatabase getmDataBase() {
        return mDataBase;
    }

    private final FirebaseDatabase mDataBase;
    private final FirebaseUser mCurrentUser;
    private final ExecutorService mExecutorService;

    private byte mMode;

    public void setMode(byte mode) {
        this.mMode = mode;
    }

    public byte getMode() {
        return mMode;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private FirebaseDatabaseManager(){
        mDataBase = FirebaseDatabase.getInstance();
        mCurrentUser = FirebaseAuthenticationManager.getInstance().getCurrentUser();
        mExecutorService = Executors.newSingleThreadExecutor();

        // Monitor all existing groups
        MonitorGroups();
    }

    public void CheckConnection(IAction action,ExecutorService executorService){
        executorService.execute(() -> {
            DatabaseReference connectedRef = mDataBase.getReference(".info/connected");
            connectedRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean connected = snapshot.getValue(Boolean.class);
                    action.onSuccess(connected);
                    if (connected) {
                        Log.d(TAG, "-----------------------connected-----------------------");
                    } else {
                        Log.d(TAG, "-----------------------not connected-----------------------");
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.w(TAG, "Listener was cancelled");
                }
            });
        });
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
        // Add the User as a member
        newGroupReference.child("Members").child(mCurrentUser.getUid()).setValue(mCurrentUser.getDisplayName());
        newGroupReference.child("SharedFiles").setValue("NoFile");

        // Add the group to the user
        mDataBase.getReference().child("Users").child(mCurrentUser.getUid())
                .child("Groups").child(groupId).setValue(group.getName());

        // Monitor the new group
        mExecutorService.execute(MonitorSingleGroup(group));

        return groupId;
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
            put(mCurrentUser.getUid(), mCurrentUser.getDisplayName());
        }});

        //Add the group to the user
        mDataBase.getReference().child("Users").child(mCurrentUser.getUid())
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

    public void UserGroupsRetriever(IAction action, ExecutorService executorService){
        executorService.execute(() -> {
            mDataBase.getReference().child("Users").child(mCurrentUser.getUid())
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
    private void MonitorGroups(){
        DatabaseReference databaseReference = mDataBase.getReference();
        databaseReference.child("Users").child(mCurrentUser.getUid())
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
    private void TakeAction(DataSnapshot fileSnapshot, Group group){
        // Get Location on Cloud and Physical Storage
        //String physicalLocation = group.getId() + " " + group.getName();
        // Download the file
        if(!fileSnapshot.child("Mode").hasChild("Striping")) {
            Uri cloudLocation = Uri.parse(fileSnapshot.child("URL").getValue(String.class) + "/"
                    + fileSnapshot.getKey() + " " + ManagersMediator.getInstance().GetCurrentUser().getUid());
            ManagersMediator.getInstance().FileDownloadProcedure(group, cloudLocation,
                    fileSnapshot.child("Name").getValue(String.class));
        }
        else {
            Uri cloudLocation = Uri.parse(fileSnapshot.child("URL").getValue(String.class));

            ManagersMediator.getInstance().FileDownloadProcedure(group, cloudLocation,
                    fileSnapshot.child("Name").getValue(String.class));
        }//FirebaseStorageManager.getInstance().Download(group,cloudLocation, fileSnapshot.child("Name").getValue().toString());

        // Add user to SeenBy
        fileSnapshot.child("SeenBy").getRef().updateChildren(new HashMap<String, Object>() {{
            put(mCurrentUser.getUid(), mCurrentUser.getDisplayName());
        }});
    }


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


    /**
     * Add listener to the group Shared Files
     *
     * @return runnable to run the code in your thread
     */

    private Runnable MonitorSingleGroup(Group group){
        return () -> {
            /*mDataBase.getReference().child("Groups").child(group.getId()).child("AllOnline").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if(snapshot.getValue(String.class).equals("True")){
                        GetMembersIDs(group.getId(), new IAction() {
                            @RequiresApi(api = Build.VERSION_CODES.Q)
                            @Override
                            public void onSuccess(Object memIds) {
                                ArrayList<String> membersIds = (ArrayList<String>) memIds;
                                membersIds.remove(ManagersMediator.getInstance().GetCurrentUser().getUid());

                                GetSharedFiles(group.getId(), new IAction() {
                                    @Override
                                    public void onSuccess(Object f) {
                                        ArrayList<SharedFile> filesdata = (ArrayList<SharedFile>) f;
                                            for (SharedFile file:filesdata) {
                                            if (file.mode.equals("Striping")) {
                                                for (String id:membersIds) {
                                                    Uri cloudLocation = Uri.parse(file.Url + "/"
                                                            + file.Id + " " + id);
                                                    ManagersMediator.getInstance().FileDownloadProcedure(group, cloudLocation,
                                                            file.Id + " " + id);
                                                }

                                            }
                                        }
                                    }
                                },mExecutorService);

                            }
                        },mExecutorService);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });*/
            // Listen to newly added files
            mDataBase.getReference().child("Groups").child(group.getId()).child("SharedFiles").addChildEventListener(new ChildEventListener() {
                @RequiresApi(api = Build.VERSION_CODES.Q)
                @Override
                public void onChildAdded(@NonNull DataSnapshot sharedFileSnapshot, @Nullable String previousChildName) {
                    mExecutorService.execute(() -> {
                        if(!sharedFileSnapshot.child("SeenBy").hasChild(mCurrentUser.getUid()))
                            TakeAction(sharedFileSnapshot, group);

                        sharedFileSnapshot.child("Name").getRef().addValueEventListener(new ValueEventListener() {
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
                                            group.getId() + " " + groupName + File.separator + "Stripped Files" + File.separator + fileName + " " + mCurrentUser.getUid());
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
    public void AddFile(String groupId, String fileId, String fileName, StorageMetadata metadata, IAction action, ExecutorService executorService) {
        executorService.execute(() -> {
            HashMap<String,String>children=new HashMap<String,String>(){{
                put("Name",fileName);
                put("URL",metadata.getPath());
                put("Md5Hash",metadata.getMd5Hash());
            }};
            mDataBase.getReference().child("Groups").child(groupId).child("SharedFiles")
                    .updateChildren(new HashMap<String,Object>() {{
                        put(fileId, children);
                    }});
            // Add user in seen
            mDataBase.getReference().child("Groups").child(groupId).child("SharedFiles")
                    .child(fileId).child("SeenBy").child(mCurrentUser.getUid())
                    .setValue(mCurrentUser.getDisplayName());
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
