package com.example.privatecloudstorage.model;

//Android Libraries
        import android.net.Uri;
        import android.os.Build;
        import android.util.Log;
        import android.util.Pair;

//3rd Party Libraries
        import androidx.annotation.NonNull;
        import androidx.annotation.Nullable;
        import androidx.annotation.RequiresApi;

        import com.google.android.gms.tasks.OnCompleteListener;
        import com.google.android.gms.tasks.OnFailureListener;
        import com.google.android.gms.tasks.OnSuccessListener;
        import com.google.android.gms.tasks.Task;
        import com.google.firebase.auth.FirebaseUser;
        import com.google.firebase.database.ChildEventListener;
        import com.google.firebase.database.DataSnapshot;
        import com.google.firebase.database.DatabaseError;
        import com.google.firebase.database.DatabaseReference;
        import com.google.firebase.database.FirebaseDatabase;
        import com.google.firebase.storage.StorageMetadata;


//Java Libraries
        import java.util.HashMap;
        import java.util.concurrent.ExecutorService;
        import java.util.concurrent.Executors;

        import io.reactivex.Observable;
        import io.reactivex.schedulers.Schedulers;


/**
 * Manages Firebase Real-Time Database
 */
public class FirebaseDatabaseManager {
    // Used for debugging
    private static final String TAG = "FirebaseDatabaseManager";

    private static FirebaseDatabaseManager mFirebaseDatabaseManager;
    private final FirebaseDatabase mDataBase;
    private final FirebaseUser mCurrentUser;
    private final ExecutorService mExecutorService;
    private final Observable mUserGroupsObservable;

    private FirebaseDatabaseManager(){
        mDataBase = FirebaseDatabase.getInstance();
        mCurrentUser = FirebaseAuthenticationManager.getInstance().getCurrentUser();
        mExecutorService = Executors.newSingleThreadExecutor();
        mUserGroupsObservable = CreateUserGroupsObservable();

        // Monitor all existing groups
        MonitorGroups();
    }

    /**
     * Create an instance if and only if it's null
     *
     * @return FirebaseDatabaseManager instance
     */
    public static FirebaseDatabaseManager getInstance(){
        if(mFirebaseDatabaseManager == null)
            mFirebaseDatabaseManager  = new FirebaseDatabaseManager();

        return mFirebaseDatabaseManager;
    }

    /**
     * Create new group in firebase and add the user to the group
     *
     * @param group The new group to be created
     *
     * @return Group Information [Group ID, Group Name]
     */
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
        mExecutorService.execute(MonitorSingleGroup(groupId));

        return groupId;
    }

    /**
     * Make user join the group
     *
     * @param group
     *
     * @return True on Success
     */
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
        mExecutorService.execute(MonitorSingleGroup(group.getId()));

        return true;
    }

    /**
     * @return Observable for user groups
     */
    public Observable getUserGroupsObservable(){
        return mUserGroupsObservable;
    }

    /**
     * Create an Observable that works on this class thread
     * The observable emits User Groups as Pair<String, String>(ID, Name)
     *
     * @return Observable for this user groups
     */
    private Observable CreateUserGroupsObservable() {
        return Observable.create(emitter -> {
            mDataBase.getReference().child("Users").child(mCurrentUser.getUid())
                    .child("Groups").addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot groupSnapshot, @Nullable String previousChildName) {
                    Pair<String, String> groupInformation;
                    groupInformation = new Pair<>(groupSnapshot.getKey(), groupSnapshot.getValue().toString());
                    emitter.onNext(groupInformation);
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                    //TODO: return something to remove the group from the list
                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        })
                .subscribeOn(Schedulers.from(mExecutorService));
    }


    /**
     * Monitor all user groups changes in cloud
     */
    private void MonitorGroups(){
        DatabaseReference databaseReference = mDataBase.getReference();
        databaseReference.child("Users").child(mCurrentUser.getUid())
                .child("Groups")
                .get().addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                for(DataSnapshot group : task.getResult().getChildren()){
                    mExecutorService.execute(MonitorSingleGroup(group.getKey()));
                }
            }
        });
    }

    /**
     * Decides if we should download the file or not
     *
     * @param fileSnapshot snapshot of the shared file
     * @param groupId Group ID of the file
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void TakeAction(DataSnapshot fileSnapshot, String groupId){
        // Get Location on Cloud and Physical Storage
        Uri cloudLocation = Uri.parse(fileSnapshot.child("URL").getValue().toString());
        String physicalLocation;
        try {
            physicalLocation = groupId + " " + fileSnapshot.child("Group").getValue().toString();
        }catch (NullPointerException e){
            Log.d(TAG, "TakeAction: line 211");
            e.printStackTrace();
            return;
        }
        // Download the file
        FirebaseStorageManager.getInstance().Download(cloudLocation, physicalLocation);

        // Add user to SeenBy
        fileSnapshot.child("SeenBy").getRef().updateChildren(new HashMap<String, Object>() {{
            put(mCurrentUser.getUid(), mCurrentUser.getDisplayName());
        }});
    }

    /**
     * Add listener to the group Shared Files
     *
     * @param groupId Group ID
     * @return runnable to run the code in your thread
     */
    private Runnable  MonitorSingleGroup(String groupId){
        return () -> {
            // Listen to newly added files
            mDataBase.getReference().child("Groups").child(groupId).child("SharedFiles").addChildEventListener(new ChildEventListener() {
                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void onChildAdded(@NonNull DataSnapshot sharedFileSnapshot, @Nullable String previousChildName) {
                    mExecutorService.execute(() -> {
                        mDataBase.getReference().child("Files").child(sharedFileSnapshot.getKey())
                                .get().addOnCompleteListener(task -> {
                                    //while(!(task.isSuccessful() && task.isComplete()));
                                    task.addOnSuccessListener(fileSnapshot ->
                                            mExecutorService.execute(() -> {
                                                // Check if user is synced then nothing should be done
                                                if (!fileSnapshot.child("SeenBy").hasChild(mCurrentUser.getUid()))
                                                    TakeAction(fileSnapshot, groupId);
                                            })
                                    );
                        });
                    });
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot sharedFileSnapshot) {
                    // TODO: Remove file from physical storage
                    mDataBase.getReference().child("Groups").child(groupId).child("Name")
                            .get().addOnSuccessListener(dataSnapshot -> {
                                String fileName = sharedFileSnapshot.getValue().toString();
                                String groupName = dataSnapshot.getValue().toString();

                                // TODO: call delete function
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

    /**
     * Add the uploaded file to the Real-Time database -- Must call on Upload Success
     *
     * @param groupId Group ID
     * @param metadata Uploaded file Metadata
     */
    public void AddFile(Pair<String, String> groupInformation, StorageMetadata metadata) {
        mExecutorService.execute(() -> {
            // Use URL as an ID
            DatabaseReference fileReference = mDataBase.getReference().child("Files").push();
            // Add file name
            fileReference.child("Name").setValue(metadata.getName());
            // Add file URL
            fileReference.child("URL").setValue(metadata.getPath());
            // Add file MD5-Hash
            fileReference.child("Md5Hash").setValue(metadata.getMd5Hash());
            // Add user in seen
            fileReference.child("SeenBy").child(mCurrentUser.getUid())
                    .setValue(mCurrentUser.getDisplayName());

            // Add the group to the file
            fileReference.child("Group").setValue(groupInformation.second);

            // Add the file to the group
            mDataBase.getReference().child("Groups").child(groupInformation.first).child("SharedFiles")
                    .updateChildren(new HashMap<String,Object>() {{
                put(fileReference.getKey(), metadata.getName());
            }});
        });
    }

    public void DeleteFile(String groupId, String url){
        mExecutorService.execute(() -> {
            mDataBase.getReference().child("Files").get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
                @Override
                public void onSuccess(DataSnapshot dataSnapshot) {
                    for(DataSnapshot file : dataSnapshot.getChildren()){
                        if(file.child("URL").getValue().toString().equals(url)){
                            file.getRef().removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    mDataBase.getReference().child("Groups").child(groupId).child("SharedFiles").child(file.getKey()).removeValue();
                                }
                            }).addOnFailureListener(e -> e.printStackTrace());
                        }
                    }
                }
            }).addOnFailureListener(e -> e.printStackTrace());
        });
    }
}
