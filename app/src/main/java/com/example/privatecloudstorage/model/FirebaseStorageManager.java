package com.example.privatecloudstorage.model;
// Android Libraries
import android.net.Uri;
import android.os.Build;
import android.util.Pair;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

// 3rd Party Libraries
import com.example.privatecloudstorage.model.FirebaseDatabaseManager;
import com.example.privatecloudstorage.model.RecursiveDirectoryObserver;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
<<<<<<< HEAD
=======
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.core.Observer;
>>>>>>> 3cf31ad621841d00c810c9a9c646d8a050414dbd

// Java Libraries
import java.io.File;
import java.util.ArrayDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

<<<<<<< HEAD
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

=======
>>>>>>> 3cf31ad621841d00c810c9a9c646d8a050414dbd

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

        OnEvent(groupsFolder,events);
        // Create Recursive Directory Listener
        // Start Monitoring the Directory
        mRecursiveDirectoryObserver.startWatching();
    }

    /**
     * Initialize Recursive Directory Observer
     * @param groupsFolder
     * @param events
     */
    private void OnEvent(File groupsFolder, int events){
        mRecursiveDirectoryObserver = new RecursiveDirectoryObserver(groupsFolder.getAbsolutePath(), events,
                (event, file) -> {
                    if (!file.isFile())
                        return;

                    TakeAction(file);
                });
    }

    /**
     * if the event is file Upload it
     * else do nothing
     * @param file
     */
    private void TakeAction(File file){
        String path = file.getAbsolutePath();
        mFirebaseDatabaseManager.getUserGroupsObservable().observeOn(Schedulers.from(mExecutorService)).subscribe(new Observer() {
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
                        e.printStackTrace();
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
        //Check if the file is in the recently downloaded queue
        mExecutorService.execute(() -> {
            if(!mRecentlyDownloaded.isEmpty() && mRecentlyDownloaded.peek().equals(file)){
                mRecentlyDownloaded.remove();
                return;
            }
            //Start uploading file
            Uri fileUri = Uri.fromFile(file);
            StorageReference fileReference = mStorage.getReference().child(groupId ).child(fileUri.getLastPathSegment());
            fileReference.putFile(fileUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        mExecutorService.execute(() -> {
                            // Wait for task to complete
                            while(!(taskSnapshot.getTask().isComplete()&&taskSnapshot.getTask().isSuccessful()));
                            mFirebaseDatabaseManager.AddFile(groupId, taskSnapshot.getMetadata());
                        });
                    })
                    .addOnFailureListener(e -> e.printStackTrace());
        });
    }

    /**
     * Download new file from storage cloud
     *
     * @param url Group ID
     *  @param groupPath
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void Download(Uri url, String groupPath) {

        mExecutorService.execute(() -> {
<<<<<<< HEAD
//                if(url == Uri.EMPTY || groupPath.equals("")){
//                    return;
//                }
=======
>>>>>>> 3cf31ad621841d00c810c9a9c646d8a050414dbd
            //Get metadata info
            StorageReference storageReference = mStorage.getReference().child(url.toString());
            storageReference.getMetadata().addOnSuccessListener(storageMetadata -> mExecutorService.execute(() -> {
                //Start download and add it to the downloaded queue
                File file = new File(mGroupsFolder.toPath() + File.separator + groupPath, storageMetadata.getName());
                mRecentlyDownloaded.add(file);
                storageReference.getFile(file)
                        .addOnFailureListener(e -> {
                            mRecentlyDownloaded.remove();
                            e.printStackTrace();
                        });
            })).addOnFailureListener(e -> e.printStackTrace());
        });
    }
}