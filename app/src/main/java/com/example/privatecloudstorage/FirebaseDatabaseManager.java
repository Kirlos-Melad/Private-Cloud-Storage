package com.example.privatecloudstorage;

//Android Libraries
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
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


/**
 * Manages Firebase Real-Time Database
 */
public class FirebaseDatabaseManager {
    private static FirebaseDatabaseManager mFirebaseDatabaseManager;
    private FirebaseDatabase mDataBase;
    private FirebaseAuthenticationManager mFirebaseAuthenticationManager;
    private File mParentDirectory;

    private FirebaseDatabaseManager(){
        mDataBase = FirebaseDatabase.getInstance();
        mFirebaseAuthenticationManager = FirebaseAuthenticationManager.getInstance();
        
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
        DatabaseReference userGrousReference = mDataBase.getReference().child("Users");
        userGrousReference.child(user.getUid()).child("Groups").child(groupId)
                .setValue(groupMembersReference.getParent().child(groupName));

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
                    MonitorSingleGroup(group.getKey());

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
    private void MonitorSingleGroup(String groupId){
        mDataBase.getReference().child("Groups").child(groupId).child("Files").addChildEventListener(new ChildEventListener() {
            //snabshot==Id
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                //TODO: Download the file
                Download(groupId,snapshot);
                FirebaseUser user = mFirebaseAuthenticationManager.getCurrentUser();
                snapshot.child("Seen").getRef().updateChildren(new HashMap<String,Object>() {{
                    put(user.getUid(),user.getDisplayName());
                }});
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

    /**
     * Download new file from storage cloud
     *
     * @param groupId Group ID
     *  @param fileSnapshot
     */
    private void Download(String groupId, DataSnapshot fileSnapshot){
        if(fileSnapshot.child("URL").getValue()==null){
            return;
        }
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReferenceFromUrl(fileSnapshot.child("URL").getValue().toString());
        StorageMetadata storageMetadata=(StorageMetadata) fileSnapshot.child("Metadata").getValue();
        mFirebaseDatabaseManager.GetUserGroups().addValueEventListener(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot child:snapshot.getChildren()){
                    if(child.getKey()==groupId){
                        String groupName=child.getValue().toString();
                        String groupDirectory= mParentDirectory + File.separator + groupName + "xd.txt";
                        File file=null;
                        if(storageMetadata!=null) {
                            file=new File(groupDirectory,storageMetadata.getName());
                        }
                        else {
                            file=new File(groupDirectory);
                        }
                        storageRef.getFile(file).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    /**
     * Add the uploaded file to the Real-Time database -- Must call on Upload Success
     *
     * @param groupId Group ID
     * @param metadata Uploaded file Metadata
     */
    public void AddFile(String groupId, StorageMetadata metadata) {
        // Get File reference
        DatabaseReference fileReference = mDataBase.getReference().child("Groups").child(groupId).child("Files");
        // Add new ID
        String fileID = fileReference.push().getKey();
        // Add URL Value
        fileReference.child(fileID).child("URL").setValue(metadata.getReference().getDownloadUrl().getResult());
        // Add MetaData
        fileReference.child(fileID).child("Metadata").setValue(metadata);
        // Add user in seen
        FirebaseUser user = mFirebaseAuthenticationManager.getCurrentUser();
        fileReference.child(fileID).child("Seen").child(user.getUid()).setValue(user.getDisplayName());
    }
}

