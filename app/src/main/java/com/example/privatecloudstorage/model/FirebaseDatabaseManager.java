package com.example.privatecloudstorage.model;

//Android Libraries
        import android.net.Uri;
        import android.os.Build;

//3rd Party Libraries
        import androidx.annotation.NonNull;
        import androidx.annotation.Nullable;
        import androidx.annotation.RequiresApi;

        import com.google.android.gms.tasks.OnSuccessListener;
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
        import java.security.NoSuchAlgorithmException;
        import java.util.HashMap;
        import java.util.concurrent.ExecutorService;
        import java.util.concurrent.Executors;

        import io.reactivex.Observable;
        import io.reactivex.ObservableEmitter;
        import io.reactivex.ObservableOnSubscribe;
        import io.reactivex.Observer;
        import io.reactivex.disposables.Disposable;
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
                    Group group = null;
                    group = new Group(groupSnapshot.getKey(),
                          groupSnapshot.getValue().toString(),
                            "","");

                    emitter.onNext(group);
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
                    mExecutorService.execute(MonitorSingleGroup(new Group(
                            group.getKey(),
                          group.getValue().toString(),
                            "",
                            ""
                    )));
                }
            }
        });
    }

    /**
     * Decides if we should download the file or not
     *
     * @param fileSnapshot snapshot of the shared file
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void TakeAction(DataSnapshot fileSnapshot, Group group){
        // Get Location on Cloud and Physical Storage
        Uri cloudLocation = Uri.parse(fileSnapshot.child("URL").getValue().toString());
        //String physicalLocation = group.getId() + " " + group.getName();

        // Download the file
        FirebaseStorageManager.getInstance().Download(group,cloudLocation, fileSnapshot.child("Name").getValue().toString());

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
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Listen to newly added files
            mDataBase.getReference().child("Groups").child(group.getId()).child("SharedFiles").addChildEventListener(new ChildEventListener() {
                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void onChildAdded(@NonNull DataSnapshot sharedFileSnapshot, @Nullable String previousChildName) {
                    mExecutorService.execute(() -> {
                        if(!sharedFileSnapshot.child("SeenBy").hasChild(mCurrentUser.getUid()))
                            TakeAction(sharedFileSnapshot,group);

                        sharedFileSnapshot.child("Name").getRef().addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot fileNameSnapshot) {
                                // TODO: Change file name !
                                try{

                                    String previousName = sharedFileSnapshot.child("Name").getValue().toString();
                                    String groupFolder = group.getId() + " " + group.getName();
                                    File file = new File(FileManager.getInstance().getApplicationDirectory() + File.separator + groupFolder,
                                            previousName);
                                    //String extension = file.toString().substring(file.getPath().lastIndexOf("."),file.toString().length());
                                    File newFile = new File(file.getPath().substring(0, file.getPath().lastIndexOf(File.separator)), fileNameSnapshot.getValue().toString());
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
                                String fileName = sharedFileSnapshot.child("Name").getValue().toString();
                                String groupName = dataSnapshot.getValue().toString();
                                File file = new File(FileManager.getInstance().getApplicationDirectory(),
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

    public String getFileKey(String groupId){
        return mDataBase.getReference().child("Groups").child(groupId).child("SharedFiles").push().getKey();
    }

    /**
     * Add the uploaded file to the Real-Time database -- Must call on Upload Success
     *
     * @param groupId Group ID
     * @param metadata Uploaded file Metadata
     */
    public void AddFile(String groupId, String fileId, String fileName,StorageMetadata metadata) {
        mExecutorService.execute(() -> {
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

    public void DeleteFile(String groupId, String fileId){
        mExecutorService.execute(() -> {
            mDataBase.getReference().child("Groups")
                    .child(groupId).child("SharedFiles").child(fileId).removeValue()
                    .addOnFailureListener(e -> e.printStackTrace());
        });
    }
    public Observable GetFileId(String groupId , String fileName){
        return Observable.create(new ObservableOnSubscribe<Object>() {
            @Override
            public void subscribe(ObservableEmitter<Object> emitter) throws Exception {
                mDataBase.getReference().child("Groups")
                        .child(groupId).child("SharedFiles").get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
                    @Override
                    public void onSuccess(DataSnapshot dataSnapshot) {
                        for(DataSnapshot file:dataSnapshot.getChildren() ){
                            if(file.child("Name").getValue().toString().equals(fileName)){
                                emitter.onNext(file.getKey());
                                emitter.onComplete();
                                return;
                            }
                        }
                    }
                });
            }
        });

    }
    public void RenameFile(String groupId,String oldName,String newName){
        mExecutorService.execute(() -> {
            //get id to delete the file from physical storage
            mFirebaseDatabaseManager.GetFileId(groupId,oldName)
                    .observeOn(Schedulers.from(mExecutorService))
                    .subscribe(new Observer(){
                        Disposable disposable = null;
                        @Override
                        public void onSubscribe(Disposable d) {
                            disposable = d;
                        }

                        @Override
                        public void onNext(Object o) {
                            mDataBase.getReference().child("Groups").child(groupId).child("SharedFiles")
                                    .child((String)o).child("Name").setValue(newName);
                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                            disposable.dispose();
                        }

                        @Override
                        public void onComplete() {
                            disposable.dispose();
                        }
                    });
        });
    }
}
