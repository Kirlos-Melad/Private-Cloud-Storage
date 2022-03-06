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

import javax.crypto.Cipher;

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


    @RequiresApi(api = Build.VERSION_CODES.Q)
    private FirebaseStorageManager(File groupsFolder ){
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
        FileManager.CreateInstance(groupsFolder, new FileManager.EventListener() {
            @Override
            public void onChildAdded(File file) {
                TakeAction(file,null, Action.Upload);
            }

            @Override
            public void onChildRemoved(File file) {
                TakeAction(file,null, Action.Delete);

            }

            @Override
            public void onChildChanged(File oldFile,File newFile) {
                TakeAction(oldFile,newFile, Action.Update);

            }
        });
    }

    /**
     * Upload the file
     * @param file
     */
    private void TakeAction(File file,File newFile, Action action){
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
                Group group = (Group) o;

                // when we find the group name do action
                if (path.contains(group.getId()+" "+group.getName())){
                    switch (action){
                        case Upload:
                            Upload(group,file);
                            break;
                        case Update:
                            Update(group.getId(),file,newFile);
                            break;
                        case Delete:
                            Delete(group.getId(), file);
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
     * @param file File path
     */
    private void Upload(Group group, File file) {
        mExecutorService.execute(() -> {
            File encryptedFile = null;
            try {
                encryptedFile = FileManager.getInstance().EncryptDecryptFile(file, group, Cipher.ENCRYPT_MODE);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            //location file in physical storage
            Uri fileUri = Uri.fromFile(encryptedFile);

            String fileId = fileUri.getLastPathSegment();
            StorageReference fileReference = mStorage.getReference().child(group.getId()).child(fileId);

            //Start uploading file
            fileReference.putFile(fileUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        mExecutorService.execute(() -> {
                            // Wait for task to complete
                            //while(!(taskSnapshot.getTask().isComplete() && taskSnapshot.getTask().isSuccessful()));
                            taskSnapshot.getTask().addOnSuccessListener(taskSnapshot1 -> {
                                mFirebaseDatabaseManager.AddFile(group.getId(),fileId, Uri.fromFile(file).getLastPathSegment(),taskSnapshot.getMetadata());
                                // TODO: Delete the file
                                //FileManager.getInstance().DeleteFile(encryptedFile);
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
     * @param url Group ID
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void Download(Group group, Uri url, String fileName) {
        mExecutorService.execute(() -> {
            String path = FileManager.getInstance().getApplicationDirectory() + File.separator +
            group.getId() + " " + group.getName();

            File file = new File(path, fileName);

            //Get metadata info
            StorageReference storageReference = mStorage.getReference().child(url.toString());
            storageReference.getMetadata()
                    .addOnSuccessListener(storageMetadata -> mExecutorService.execute(() -> {
                        //Start download and add it to the downloaded queue

                        storageReference.getFile(file)
                                .addOnSuccessListener(taskSnapshot -> {
                                    try {
                                        FileManager.getInstance().EncryptDecryptFile(file, group, Cipher.DECRYPT_MODE);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
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
        //TODO:loop on fileName in SharedFiles
        mExecutorService.execute(() -> {
            //get id to delete the file from physical storage
            mFirebaseDatabaseManager.GetFileId(groupId,Uri.fromFile(file).getLastPathSegment())
            .observeOn(Schedulers.from(mExecutorService))
                    .subscribe(new Observer(){
                        Disposable disposable = null;
                        @Override
                        public void onSubscribe(Disposable d) {
                            disposable = d;
                        }

                        @Override
                        public void onNext(Object o) {
                            StorageReference fileReference = mStorage.getReference().child(groupId).child((String)o);
                            fileReference.delete().addOnSuccessListener(unused -> {
                                mFirebaseDatabaseManager.DeleteFile(groupId,(String) o);
                            }).addOnFailureListener(e -> {
                                Log.d(TAG, "onFailure: line 219");
                                e.printStackTrace();
                            });

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

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void Update(String groupId, File oldFile,File newFile) {
        mFirebaseDatabaseManager.RenameFile(groupId,oldFile.getName(), newFile.getName());
    }
}