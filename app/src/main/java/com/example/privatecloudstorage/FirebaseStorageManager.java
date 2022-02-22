package com.example.privatecloudstorage;
// Android Libraries
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.util.Pair;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

// 3rd Party Libraries
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

// Java Libraries
import java.io.File;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

//import io.reactivex.Observer;
//import io.reactivex.disposables.Disposable;
//import io.reactivex.schedulers.Schedulers;

public class FirebaseStorageManager {
    // Used for debugging
    private static final String TAG = "FirebaseStorageManager";

    private static FirebaseStorageManager mFirebaseStorageManager;
    private FirebaseStorage mStorage;
    private FirebaseDatabaseManager mFirebaseDatabaseManager;
    private RecursiveDirectoryObserver mRecursiveDirectoryObserver;
    private ExecutorService mExecutorService;

    private File mGroupsFolder;
    private ArrayDeque<File> mRecentlyDownloaded;


    @RequiresApi(api = Build.VERSION_CODES.Q)
    private FirebaseStorageManager(File groupsFolder, int events ){
        mRecentlyDownloaded = new ArrayDeque<File>();
        this.mGroupsFolder = groupsFolder;

        mStorage = FirebaseStorage.getInstance();
        mFirebaseDatabaseManager = FirebaseDatabaseManager.getInstance();

        mExecutorService = Executors.newSingleThreadExecutor();
//        mFirebaseDatabaseManager.SubscribeToGroupsSharedFilesFlowable(o -> {
//            Pair<Uri, String> fileLocation = (Pair<Uri, String>)o;
//            Download(fileLocation.first, fileLocation.second);
//        }, mExecutorService);

        // Create Recursive Directory Listener
        mRecursiveDirectoryObserver = new RecursiveDirectoryObserver(groupsFolder.getAbsolutePath(), events,
                (event, file) -> {
                    if (!file.isFile())
                        return;

                    TakeAction(file);
                });
        // Start Monitoring the Directory
        mRecursiveDirectoryObserver.startWatching();
    }

    /**
     * if the event is file Upload it
     * else do nothing
     *
     * @param file
     */
    private void TakeAction(File file){
        String path = file.getAbsolutePath();
        mFirebaseDatabaseManager.GetUserGroupsObservable()
                .observeOn(Schedulers.from(mExecutorService))
                .subscribe(new Observer() {
                    Disposable disposable = null;

                    @Override
                    public void onSubscribe(Disposable d) {

                        disposable = d;
                    }

                    @Override
                    public void onNext(@NonNull Object o) {


                        // [Group ID, Group Name]
                        Pair<String, String> groupInformation = (Pair<String, String>) o;

                        // If we found the group name and the file isn't a DIRECTORY THEN UPLOAD
                        if (path.contains(groupInformation.second)){
                            if(path.endsWith(groupInformation.second)){
                                disposable.dispose();
                                return;
                            }
                            UploadFile(groupInformation.first, new File(path));
                            disposable.dispose();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {
                        disposable.dispose();
                    }
                });
    }


    public static FirebaseStorageManager getInstance(){
        return mFirebaseStorageManager;
    }
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static void CreateInstance(File observed, int events){
        if(mFirebaseStorageManager == null)
            mFirebaseStorageManager  = new FirebaseStorageManager(observed, events);
    }

    /**
     * Upload new file to cloud
     * @param groupId Group ID
     * @param file File path
     */
    private void UploadFile(String groupId, File file) {
        Log.d(TAG, "Upload File >> " +Thread.currentThread().getId());
        if(!mRecentlyDownloaded.isEmpty() && mRecentlyDownloaded.peek().equals(file)){
            mRecentlyDownloaded.remove();
            return;
        }

        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("*/*")
                .build();

        Uri fileUri = Uri.fromFile(file);
        StorageReference fileReference = mStorage.getReference().child(groupId ).child(fileUri.getLastPathSegment());
        fileReference.putFile(fileUri, metadata)
                .addOnSuccessListener(taskSnapshot -> {
                    // Wait for task to complete
                    while(!(taskSnapshot.getTask().isComplete()&&taskSnapshot.getTask().isSuccessful()));


                    mFirebaseDatabaseManager.AddFile(groupId, taskSnapshot.getMetadata());
                })
                .addOnFailureListener(e -> e.printStackTrace());
    }

    /**
     * Download new file from storage cloud
     *
     * @param url Group ID
     *  @param groupPath
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void Download(Uri url, String groupPath) {

        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Download File >> " +Thread.currentThread().getId());
                if(url == Uri.EMPTY || groupPath.equals("")){
                    return;
                }

                StorageReference storageReference = mStorage.getReference().child(url.toString());
                //File file = new File(mGroupsFolder.toPath()+File.separator+groupPath, storageMetadata.getName())
                storageReference.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void onSuccess(StorageMetadata storageMetadata) {
                        mRecentlyDownloaded.add(new File(mGroupsFolder.toPath()+File.separator+groupPath, storageMetadata.getName()));
                        Log.d(TAG, mGroupsFolder.toPath()+File.separator+groupPath+ File.separator+ storageMetadata.getName() );
                        storageReference.getFile(new File(mGroupsFolder.toPath()+File.separator+groupPath, storageMetadata.getName()))
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        mRecentlyDownloaded.remove();
                                        e.printStackTrace();
                                    }
                                });
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        });
    }
}