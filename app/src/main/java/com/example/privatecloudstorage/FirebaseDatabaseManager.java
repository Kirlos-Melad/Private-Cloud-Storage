package com.example.privatecloudstorage;

//Android Libraries
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.nio.file.Path;
import java.nio.file.Paths;

//3rd Party Libraries
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

//Java Libraries
import java.io.File;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Manages Firebase Real-Time Database
 */
public class FirebaseDatabaseManager {
    private static FirebaseDatabaseManager mFirebaseDatabaseManager;
    private FirebaseDatabase mDataBase;
    private FirebaseAuthenticationManager mFirebaseAuthenticationManager;
    private File mParentDirectory;
    private ExecutorService mExecutorService;

    private FirebaseDatabaseManager(){
        mDataBase = FirebaseDatabase.getInstance();
        mFirebaseAuthenticationManager = FirebaseAuthenticationManager.getInstance();
        mExecutorService = Executors.newSingleThreadExecutor();
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
     * @param group: The new group to be created
     *
     * @return QR Code of the new Group
     */
    public Bitmap AddGroup(Group group){
        DatabaseReference groupsReference = mDataBase.getReference().child("Groups");
        //Generate new group id
        String groupId = groupsReference.push().getKey();
        DatabaseReference groupReference = groupsReference.child(groupId);
        // Add a New Object as a child and Use lambda function Listener On Complete -> DatabaseReference.CompletionListener
        groupReference.setValue(group);

        // Monitor the new group
        MonitorSingleGroup(groupId);

        // Add the User as a member
        FirebaseUser user = mFirebaseAuthenticationManager.getCurrentUser();
        groupReference.child("Members").child(user.getUid()).setValue(user.getDisplayName());

        // Add the group to the user
        DatabaseReference userReference = mDataBase.getReference().child("Users").child(user.getUid());
        userReference.child("Groups").child(groupId).setValue(group.mName);

        String information = groupId + "," + group.mName;

        return GenerateQRCodeImage(information,800,800);

    }

    /**
     * Generate QR Code
     *
     * @param information
     * @param width
     * @param height
     *
     * @return return QR Code
     */
    private Bitmap GenerateQRCodeImage(String information, int width, int height) {
        try{
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            // Encode the information to the QR Code image
            BitMatrix bitMatrix = qrCodeWriter.encode(information, BarcodeFormat.QR_CODE, width, height);

            //Convert the BitMatrix to a Bitmap to be able to use it in android
            Bitmap bitmap = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            return bitmap;
        }catch (WriterException e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Make user join the group
     *
     * @param groupInformation array holds [Group ID, Group Name]
     * @return
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
        userGroupsReference.child(user.getUid()).child("Groups").child(groupId)
                .updateChildren(new HashMap<String,Object>() {{
                    put(groupId, groupName);
                }});

        // Monitor the new group
        MonitorSingleGroup(groupId);

        return true;
    }
    public DatabaseReference GetUserGroups() {
        FirebaseUser user = mFirebaseAuthenticationManager.getCurrentUser();
        return mDataBase.getReference().child("Users").child(user.getUid()).child("Groups");
    }

    /**
     * Monitor all user groups changes in cloud
     */
    public void MonitorGroups(File parentDirectry){
        mParentDirectory = parentDirectry;
        mDataBase.getReference().child("Users").child(mFirebaseAuthenticationManager.getCurrentUser().getUid())
                .child("Groups").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot group : snapshot.getChildren()){
                    mExecutorService.execute(MonitorSingleGroup(group.getKey()));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    /**
     * Monitor a single user group in cloud
     *
     * @param groupId Group ID
     */
    private Runnable MonitorSingleGroup(String groupId){
        return new Runnable() {
            @Override
            public void run() {
                // Get Shared Files in the group
                mDataBase.getReference().child("Groups").child(groupId).child("SharedFiles").addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot sharedFileSnapshot, @Nullable String previousChildName) {
                        // Get File Data
                        mDataBase.getReference().child("Files").child(sharedFileSnapshot.getKey()).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot fileSnapshot) {
                                for(DataSnapshot seenBy : fileSnapshot.child("SeenBy").getChildren()){
                                    if(seenBy.getKey()==mFirebaseAuthenticationManager.getCurrentUser().getUid()){
                                        return;
                                    }
                                }

                                // Get Location on Cloud and Physical Storage
                                Uri url = Uri.parse(fileSnapshot.child("URL").getValue().toString());
                                String groupPath = mParentDirectory.getPath() + File.separator + fileSnapshot.child("Group").child(groupId).getValue();

                                // Download File
                                FirebaseStorageManager.getInstance().Download(url, groupPath);

                                // Add user to SeenBy
                                FirebaseUser user = mFirebaseAuthenticationManager.getCurrentUser();
                                fileSnapshot.child("SeenBy").getRef().updateChildren(new HashMap<String,Object>() {{
                                    put(user.getUid(),user.getDisplayName());
                                }});
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

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

