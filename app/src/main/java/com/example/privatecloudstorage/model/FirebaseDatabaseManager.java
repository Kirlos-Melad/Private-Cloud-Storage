package com.example.privatecloudstorage.model;

//Android Libraries
        import android.net.Uri;
        import android.os.Build;
        import android.os.Handler;
        import android.os.Looper;

//3rd Party Libraries
        import androidx.annotation.NonNull;
        import androidx.annotation.Nullable;
        import androidx.annotation.RequiresApi;

        import com.example.privatecloudstorage.interfaces.IAction;
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
    private final FirebaseUser mCurrentUser;
    private final ExecutorService mExecutorService;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private FirebaseDatabaseManager(){
        mDataBase = FirebaseDatabase.getInstance();
        mCurrentUser = FirebaseAuthenticationManager.getInstance().getCurrentUser();
        mExecutorService = Executors.newSingleThreadExecutor();

        // Monitor all existing groups
        MonitorGroups();
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
    /**
     * Create an Observable that works on this class thread
     * The observable emits Group Members as Pair<String, String>(ID, Name)
     *
     * @param action action to be executed on success
     * @param executorService thread to run on
     */

    public void GroupMembersRetriever(String groupId,IAction action, ExecutorService executorService){
        executorService.execute(() -> {
            mDataBase.getReference().child("Groups").child(groupId)
                    .child("Members").get().addOnSuccessListener(dataSnapshot -> {
                executorService.execute(() -> {
                    ArrayList<String> users = new ArrayList<>();

                    for(DataSnapshot user : dataSnapshot.getChildren()){
                        users.add(user.getKey());
                    }
                    // Must run this on main thread to avoid problems
                    new Handler(Looper.getMainLooper()).post(() -> {
                        action.onSuccess(users);
                    });
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
        Uri cloudLocation = Uri.parse(fileSnapshot.child("URL").getValue(String.class));
        //String physicalLocation = group.getId() + " " + group.getName();

        // Download the file
        ManagersMediator.getInstance().FileDownloadProcedure(group, cloudLocation, fileSnapshot.child("Name").getValue(String.class));
        //FirebaseStorageManager.getInstance().Download(group,cloudLocation, fileSnapshot.child("Name").getValue().toString());

        // Add user to SeenBy
        fileSnapshot.child("SeenBy").getRef().updateChildren(new HashMap<String, Object>() {{
            put(mCurrentUser.getUid(), mCurrentUser.getDisplayName());
        }});
    }

    /**
     * Add listener to the group Shared Files
     *
     * @return runnable to run the code in your thread
     */

    private Runnable MonitorSingleGroup(Group group){
        return () -> {
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
                                    String groupFolder = group.getId() + " " + group.getName();
                                    File file = new File(FileManager.getInstance().GetApplicationDirectory() + File.separator + groupFolder,
                                            previousName);
                                    //String extension = file.toString().substring(file.getPath().lastIndexOf("."),file.toString().length());
                                    File newFile = new File(file.getPath().substring(0, file.getPath().lastIndexOf(File.separator)), fileNameSnapshot.getValue(String.class));
                                    file.renameTo(newFile);
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
                                String groupName = dataSnapshot.getValue(String.class);
                                File file = new File(FileManager.getInstance().GetApplicationDirectory(),
                                        group.getId() + " " + groupName + File.separator + fileName);
                        FileManager.getInstance().DeleteFile(file);
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

}
