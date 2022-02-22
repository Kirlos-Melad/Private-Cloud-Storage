package com.example.privatecloudstorage;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

//import io.reactivex.Flowable;
//import io.reactivex.Observable;
//import io.reactivex.disposables.Disposable;
//import io.reactivex.functions.Consumer;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;
//import io.reactivex.schedulers.Schedulers;


/**
 * Manages Firebase Real-Time Database
 */
public class FirebaseDatabaseManager {
    // Used for debugging
    private static final String TAG = "FirebaseDatabaseManager";

    private static FirebaseDatabaseManager mFirebaseDatabaseManager;
    private FirebaseDatabase mDataBase;
    private FirebaseAuthenticationManager mFirebaseAuthenticationManager;

    private Flowable mGroupsSharedFilesFlowable;
    private ExecutorService mFlowableExecutorService;

    private Consumer mOnNextAction;
    private Disposable mOnNextActionDisposable;

    private HashMap<ValueEventListener, Boolean> mIsDone;

    private FirebaseDatabaseManager(){
        mDataBase = FirebaseDatabase.getInstance();
        mFirebaseAuthenticationManager = FirebaseAuthenticationManager.getInstance();
        mFlowableExecutorService = Executors.newSingleThreadExecutor();

        // Create a flowable that works on this class thread
        mGroupsSharedFilesFlowable = Flowable.just(new Pair<Uri,String>(Uri.EMPTY,"")).subscribeOn(Schedulers.from(mFlowableExecutorService));
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
    public Pair<String, String> AddGroup(Group group){
        DatabaseReference groupsReference = mDataBase.getReference().child("Groups");
        //Generate new group id
        String groupId = groupsReference.push().getKey();
        DatabaseReference groupReference = groupsReference.child(groupId);
        // Add a New Object as a child and Use lambda function Listener On Complete -> DatabaseReference.CompletionListener
        groupReference.setValue(group);

        // Monitor the new group
        //MergeNewGroupSharedFilesFlowable(groupId);
        mFlowableExecutorService.execute(MonitorSingleGroup(groupId));
        // Add the User as a member
        FirebaseUser user = mFirebaseAuthenticationManager.getCurrentUser();
        groupReference.child("Members").child(user.getUid()).setValue(user.getDisplayName());

        // Add the group to the user
        DatabaseReference userReference = mDataBase.getReference().child("Users").child(user.getUid());
        userReference.child("Groups").child(groupId).setValue(group.mName);

        return new Pair<>(groupId, group.mName);
    }

    /**
     * Make user join the group
     *
     * @param groupInformation [Group ID, Group Name]
     *
     * @return True on Success
     */
    public boolean JoinGroup(String[] groupInformation){
        String groupId = groupInformation[0];
        String groupName = groupInformation[1];

        DatabaseReference groupMembersReference = mDataBase.getReference().child("Groups").child(groupId).child("Members");
        //Add the User as a member
        FirebaseUser user = mFirebaseAuthenticationManager.getCurrentUser();
        groupMembersReference.updateChildren(new HashMap<String,Object>() {{
            put(user.getUid(), user.getDisplayName());
        }});

        //Add the group to the user
        DatabaseReference userGroupsReference = mDataBase.getReference().child("Users");
        userGroupsReference.child(user.getUid()).child("Groups").
                updateChildren(new HashMap<String,Object>() {{
                    put(groupId,groupName);
                }});

        // Monitor the new group
        // MergeNewGroupSharedFilesFlowable(groupId);
        mFlowableExecutorService.execute(MonitorSingleGroup(groupId));
        return true;
    }

    /**
     * Create an Observable that works on this class thread
     * The observable emits User Groups as [ID, Name] pairs
     *
     * @return Observable for this user groups ENDS WITH onComplete
     */
    public Observable GetUserGroupsObservable() {
        return Observable.create(emitter -> {
            FirebaseUser user = mFirebaseAuthenticationManager.getCurrentUser();
            DatabaseReference databaseReference = mDataBase.getReference();
            databaseReference.child("Users").child(user.getUid()).child("Groups").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Pair<String, String> groupInformation;
                    for(DataSnapshot group : snapshot.getChildren()){
                        groupInformation = new Pair<>(group.getKey(), group.getValue().toString());
                        emitter.onNext(groupInformation);
                    }

                    emitter.onComplete();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        })
                .subscribeOn(Schedulers.from(mFlowableExecutorService));
    }

    /**
     * Monitor all user groups changes in cloud
     */
    private void MonitorGroups(){
//        ArrayList<Flowable<Pair<Uri,String>>> flowableArrayList = new ArrayList<>();

        DatabaseReference databaseReference = mDataBase.getReference();
        databaseReference.child("Users").child(mFirebaseAuthenticationManager.getCurrentUser().getUid())
                .child("Groups").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                for(DataSnapshot group : snapshot.getChildren()){
//                    flowableArrayList.add(GetSharedFileLocationFlowable(group.getKey()));
                    mFlowableExecutorService.execute(MonitorSingleGroup(group.getKey()));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private ValueEventListener getValueEventListener(String groupId,String sharedFileKey){
        return new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onDataChange(@NonNull DataSnapshot fileSnapshot) {
                ValueEventListener listener = this;
                mFlowableExecutorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "On Data Change inner listener >> " +Thread.currentThread().getId());
                        for (DataSnapshot seenBy : fileSnapshot.child("SeenBy").getChildren()) {
                            if (seenBy.getKey().equals(mFirebaseAuthenticationManager.getCurrentUser().getUid())) {
                                mDataBase.getReference().child("Files").child(sharedFileKey).removeEventListener(listener);
                                return;
                            }
                        }
                        // Get Location on Cloud and Physical Storage
                        Uri cloudLocation = Uri.parse(fileSnapshot.child("URL").getValue().toString());
                        String physicalLocation = groupId + " " + fileSnapshot.child("Group").child(groupId).getValue().toString();

                        FirebaseStorageManager.getInstance().Download(cloudLocation,physicalLocation);
                        // TODO: Sub to Successful DOWNLOAD
                        // Add user to SeenBy
                        FirebaseUser user = mFirebaseAuthenticationManager.getCurrentUser();
                        fileSnapshot.child("SeenBy").getRef().updateChildren(new HashMap<String, Object>() {{
                            put(user.getUid(), user.getDisplayName());
                        }});
                        mDataBase.getReference().child("Files").child(sharedFileKey).removeEventListener(listener);
                    }
                });
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
    }

    private Runnable  MonitorSingleGroup(String groupId){
        return new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Monitor Single group / before listener >> " +Thread.currentThread().getId());
                mDataBase.getReference().child("Groups").child(groupId).child("SharedFiles").addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot sharedFileSnapshot, @Nullable String previousChildName) {
                        mFlowableExecutorService.execute(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "Outer listener >> " +Thread.currentThread().getId());
                                ValueEventListener listener = getValueEventListener(groupId, sharedFileSnapshot.getKey());
                                //mIsDone.put(getValueEventListener(groupId), new Boolean(false));

                                mDataBase.getReference().child("Files").child(sharedFileSnapshot.getKey()).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DataSnapshot> task) {
                                        if(task.isSuccessful()){
                                            mFlowableExecutorService.execute(new Runnable() {
                                                @RequiresApi(api = Build.VERSION_CODES.O)
                                                @Override
                                                public void run() {
                                                    DataSnapshot fileSnapshot= task.getResult();
                                                    Log.d(TAG, "On Data Change inner listener >> " +Thread.currentThread().getId());
                                                    for (DataSnapshot seenBy : fileSnapshot.child("SeenBy").getChildren()) {
                                                        if (seenBy.getKey().equals(mFirebaseAuthenticationManager.getCurrentUser().getUid())) {
                                                            mDataBase.getReference().child("Files").child(sharedFileSnapshot.getKey()).removeEventListener(listener);
                                                            return;
                                                        }
                                                    }
                                                    // Get Location on Cloud and Physical Storage
                                                    Uri cloudLocation = Uri.parse(fileSnapshot.child("URL").getValue().toString());
                                                    String physicalLocation = groupId + " " + fileSnapshot.child("Group").child(groupId).getValue().toString();

                                                    FirebaseStorageManager.getInstance().Download(cloudLocation,physicalLocation);
                                                    // TODO: Sub to Successful DOWNLOAD
                                                    // Add user to SeenBy
                                                    FirebaseUser user = mFirebaseAuthenticationManager.getCurrentUser();
                                                    fileSnapshot.child("SeenBy").getRef().updateChildren(new HashMap<String, Object>() {{
                                                        put(user.getUid(), user.getDisplayName());
                                                    }});
                                                    mDataBase.getReference().child("Files").child(sharedFileSnapshot.getKey()).removeEventListener(listener);
                                                }
                                            });
                                        }
                                    }
                                });
                                //while(!mIsDone);
                                //mDataBase.getReference().child("Files").child(sharedFileSnapshot.getKey()).removeEventListener(listener);
                            }
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


            }
        };
    }

    /**
     * Add the uploaded file to the Real-Time database -- Must call on Upload Success
     *
     * @param groupId Group ID
     * @param metadata Uploaded file Metadata
     */
    public void AddFile(String groupId, StorageMetadata metadata) {
        // Get File reference
        DatabaseReference fileReference = mDataBase.getReference().child("Files").push();
        // Add Metadata Value
        fileReference.child("Name").setValue(metadata.getReference().getName());
        // Add URL Value
        fileReference.child("URL").setValue(metadata.getPath());
        // Add MetaData
        fileReference.child("Md5Hash").setValue(metadata.getMd5Hash());
        // Add user in seen
        FirebaseUser user = mFirebaseAuthenticationManager.getCurrentUser();
        fileReference.child("SeenBy").child(user.getUid()).setValue(user.getDisplayName());

        // Add the group to the file
        mDataBase.getReference().child("Groups").child(groupId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                fileReference.child("Group").child(groupId).setValue(snapshot.child("mName").getValue());
                mDataBase.getReference().child("Group").child(groupId).removeEventListener(this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        // Add the file to the group
        mDataBase.getReference().child("Groups").child(groupId).child("SharedFiles").updateChildren(new HashMap<String,Object>() {{
            put(fileReference.getKey(), metadata.getReference().getName());
        }});
    }
}