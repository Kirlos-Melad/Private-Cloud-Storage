package com.example.privatecloudstorage.model;
// Android Libraries
import android.net.Uri;
import android.os.Build;
import android.os.FileObserver;
import android.util.Log;
import android.util.Pair;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

// 3rd Party Libraries
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

// Java Libraries
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
enum Action{
    Upload,
    Update,
    Delete
}
public class FirebaseStorageManager {
    // Used for debugging
    private static final String TAG = "FirebaseStorageManager";

    private static FirebaseStorageManager mFirebaseStorageManager;
    private FirebaseStorage mStorage;
    private FirebaseDatabaseManager mFirebaseDatabaseManager;
    private ExecutorService mExecutorService;

    private File mGroupsFolder;
    private ArrayDeque<File> mRecentlyDownloaded;


    @RequiresApi(api = Build.VERSION_CODES.Q)
    private FirebaseStorageManager(File groupsFolder ){
        mRecentlyDownloaded = new ArrayDeque<File>();
        this.mGroupsFolder = groupsFolder;

        mStorage = FirebaseStorage.getInstance();
        mFirebaseDatabaseManager = FirebaseDatabaseManager.getInstance();

        mExecutorService = Executors.newSingleThreadExecutor();

        // Create Recursive Directory Listener
        OnEvent(groupsFolder);
        // Start Monitoring the Directory
    }

    /**
     * Initialize Recursive Directory Observer
     * @param groupsFolder
     */
    private void OnEvent(File groupsFolder){
        FileManager.CreateInstance(groupsFolder,
                (event, file) -> {
                    // We don't want to sync directories
                    switch (event){
                        case FileManager.CREATE:
                            TakeAction(file, Action.Upload);
                            break;
                        case FileManager.RENAME:
                            TakeAction(file, Action.Update);
                            break;
                        case FileManager.DELETE:
                            TakeAction(file, Action.Delete);
                            break;
                    }
                });
    }

    /**
     * Upload the file
     * @param file
     */
    private void TakeAction(File file, Action action){
        String path = file.getAbsolutePath();
        mFirebaseDatabaseManager.getUserGroupsObservable()
                .observeOn(Schedulers.from(mExecutorService))
                .subscribe(new Observer() {
            Disposable disposable = null;

            @Override
            public void onSubscribe(Disposable d) {

                disposable = d;
            }

            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onNext(@NonNull Object o) {
                // [Group ID, Group Name]
                Pair<String, String> groupInformation = (Pair<String, String>) o;

                // when we find the group name do action
                if (path.contains(groupInformation.second)){
                    switch (action){
                        case Upload:
                            //Check if the file is in the recently downloaded queue
                            if(!mRecentlyDownloaded.isEmpty() && mRecentlyDownloaded.peek().equals(file))
                                mRecentlyDownloaded.remove();
                            else
                                Upload(groupInformation, new File(path));
                            break;
                        case Update:
                            /*try {
                                Update(groupInformation.first, new File(path));
                            } catch (IOException e) {
                                Log.d(TAG, "onNext: line 130");
                                e.printStackTrace();
                            }*/
                            break;
                        case Delete:
                            Delete(groupInformation.first, new File(path));
                            break;
                    }
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
    public static void CreateInstance(File observed){
        if(mFirebaseStorageManager == null)
            mFirebaseStorageManager  = new FirebaseStorageManager(observed);
    }

    /**
     * Upload new file to cloud
     * @param groupId Group ID
     * @param file File path
     */
    private void Upload(Pair<String, String> groupInformation, File file) {
        mExecutorService.execute(() -> {
            StorageMetadata storageMetadata = new StorageMetadata.Builder()
                    .setCustomMetadata("FileOwner",FirebaseAuthenticationManager.getInstance().getCurrentUser().getUid())
                    .build();
            Uri fileUri = Uri.fromFile(file);
            StorageReference fileReference = mStorage.getReference().child(groupInformation.first).child(fileUri.getLastPathSegment());

            //Start uploading file
            fileReference.putFile(fileUri, storageMetadata)
                    .addOnSuccessListener(taskSnapshot -> {
                        mExecutorService.execute(() -> {
                            // Wait for task to complete
                            //while(!(taskSnapshot.getTask().isComplete() && taskSnapshot.getTask().isSuccessful()));
                            taskSnapshot.getTask().addOnSuccessListener(taskSnapshot1 -> {
                                mFirebaseDatabaseManager.AddFile(groupInformation, taskSnapshot.getMetadata());
                            });
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.d(TAG, "Upload: line 188");
                        e.printStackTrace();
                    });
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
            //Get metadata info
            StorageReference storageReference = mStorage.getReference().child(url.toString());
            storageReference.getMetadata()
                    .addOnSuccessListener(storageMetadata -> mExecutorService.execute(() -> {
                        //Start download and add it to the downloaded queue
                        File file = new File(mGroupsFolder.toPath() + File.separator + groupPath, storageMetadata.getName());

                        storageReference.getFile(file)
                                .addOnSuccessListener(taskSnapshot -> {
                                    mRecentlyDownloaded.add(file);
                                })
                                .addOnFailureListener(e -> {
                                    Log.d(TAG, "Download: line 203");
                                    e.printStackTrace();
                                });
                    }))
                    .addOnFailureListener(e -> {
                        Log.d(TAG, "Download: line 207");
                        e.printStackTrace();
                    });
        });
    }

    private void Delete(String groupId, File file){
        mExecutorService.execute(() -> {
            StorageReference fileReference = mStorage.getReference().child(groupId).child(Uri.fromFile(file).getLastPathSegment());
            fileReference.delete().addOnSuccessListener(unused -> {
                mFirebaseDatabaseManager.DeleteFile(groupId, fileReference.toString());
            }).addOnFailureListener(e -> {
                Log.d(TAG, "onFailure: line 219");
                e.printStackTrace();
            });
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void Update(String groupId, File file) throws IOException {
        StorageReference fileReference = mStorage.getReference().child(groupId).child(Uri.fromFile(file).getLastPathSegment());

        BasicFileAttributes basicFileAttributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);

        fileReference.updateMetadata((StorageMetadata) basicFileAttributes).addOnSuccessListener(storageMetadata -> {
            // TODO: Update real time
            //mFirebaseDatabaseManager.UpdateFile(fileReference.toString(), storageMetadata.getPath());
        }).addOnFailureListener(e -> {
            Log.d(TAG, "ModifyAttribute: line 242");
            e.printStackTrace();
        });
    }
}