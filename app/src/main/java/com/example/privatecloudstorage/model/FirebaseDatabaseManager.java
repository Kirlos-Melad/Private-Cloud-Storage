package com.example.privatecloudstorage.model;

//Android Libraries
import android.net.Uri;
import android.os.Build;
import android.util.Pair;

//3rd Party Libraries
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.android.gms.tasks.OnCompleteListener;
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

<<<<<<< HEAD
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
=======
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
>>>>>>> 3cf31ad621841d00c810c9a9c646d8a050414dbd


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
        for (DataSnapshot seenBy : fileSnapshot.child("SeenBy").getChildren()) {
            // Check if user is synced then nothing should be done
            if (seenBy.getKey().equals(mCurrentUser.getUid())) {
                return;
            }
        }
        // Get Location on Cloud and Physical Storage
        Uri cloudLocation = Uri.parse(fileSnapshot.child("URL").getValue().toString());
        String physicalLocation = groupId + " " + fileSnapshot.child("Group").child(groupId).getValue().toString();

        // Download the file
        FirebaseStorageManager.getInstance().Download(cloudLocation,physicalLocation);

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
                         mDataBase.getReference().child("Files")
                                .child(sharedFileSnapshot.getKey()).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DataSnapshot> task) {
                                      mExecutorService.execute(new Runnable() {
                                          @Override
                                          public void run() {
                                              TakeAction(task.getResult(), groupId);
                                          }
                                      });
                                    }
                                });
                    });
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {

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
    public void AddFile(String groupId, StorageMetadata metadata) {
        mExecutorService.execute(() -> {
            // Generate an ID for the file
            DatabaseReference fileReference = mDataBase.getReference().child("Files").push();
            // Add Metadata Value
            fileReference.child("Name").setValue(metadata.getReference().getName());
            // Add URL Value
            fileReference.child("URL").setValue(metadata.getPath());
            // Add MetaData
            fileReference.child("Md5Hash").setValue(metadata.getMd5Hash());
            // Add user in seen
            fileReference.child("SeenBy").child(mCurrentUser.getUid()).setValue(mCurrentUser.getDisplayName());

            // Add the group to the file
             mDataBase.getReference().child("Groups").child(groupId)
                    .get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DataSnapshot> task) {
                            fileReference.child("Group").child(groupId).setValue(task.getResult().child("Name").getValue());
                        }
                    });

<<<<<<< HEAD
=======


>>>>>>> 3cf31ad621841d00c810c9a9c646d8a050414dbd
            // Add the file to the group
            mDataBase.getReference().child("Groups").child(groupId).child("SharedFiles").updateChildren(new HashMap<String,Object>() {{
                put(fileReference.getKey(), metadata.getReference().getName());
            }});
        });
    }
}