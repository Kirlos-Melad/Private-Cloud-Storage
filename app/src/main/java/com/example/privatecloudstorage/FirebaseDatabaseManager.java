package com.example.privatecloudstorage;

//Android Libraries
import android.graphics.Bitmap;
import android.graphics.Color;

//3rd Party Libraries
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

//Java Libraries
import java.util.HashMap;


/**
 * Manages Firebase Real-Time Database
 */
public class FirebaseDatabaseManager {
    private static FirebaseDatabaseManager mFirebaseDatabaseManager;
    private FirebaseDatabase mDataBase;
    private FirebaseAuthenticationManager mFirebaseAuthenticationManager;

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
        //Generate new group id
        String groupId = mDataBase.getReference().child("Groups").push().getKey();
        DatabaseReference groupReference = mDataBase.getReference().child("Groups").child(groupId);
        // Add a New Object as a child and Use lambda function Listener On Complete -> DatabaseReference.CompletionListener
        groupReference.setValue(group);

        //Add the User as a member
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
        return false;

       // DatabaseReference groupMembersReference = mDataBase.getReference().child("Groups").child(groupInformation[0]).child("Members");
        //Add the User as a member
       // FirebaseUser user = mFirebaseAuthenticationManager.getCurrentUser();
        //groupMembersReference.updateChildren(new HashMap<String,Object>() {{
          //  put(user.getUid(), user.getDisplayName());
        //}});

        // Add the group to the user
        //DatabaseReference userGrousReference = mDataBase.getReference().child("Users");
        //userGrousReference.child(user.getUid()).child("Groups").child(groupInformation[0]).setValue(groupMembersReference.getParent().child(groupInformation[1]));

       // return true;
    }
}

