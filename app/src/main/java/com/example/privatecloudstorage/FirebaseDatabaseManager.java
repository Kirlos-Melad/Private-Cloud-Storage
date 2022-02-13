package com.example.privatecloudstorage;

//Android Libraries
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

//3rd Party Libraries
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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

    private FirebaseDatabaseManager(){
        mDataBase = FirebaseDatabase.getInstance();
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
    @RequiresApi(api = Build.VERSION_CODES.O)
    public Bitmap AddGroup(Group group){
        //Generate new child Key
        DatabaseReference databaseReference = mDataBase.getReference().child("Groups").push();
        // Add a New Object as a child and Use lambda function Listener On Complete -> DatabaseReference.CompletionListener
        databaseReference.setValue(group,
                (databaseError, databaseReference1) -> {
                    //Toast.makeText(FirebaseDatabaseManager.this, "Inserted successfully", Toast.LENGTH_LONG).show();
                });

        //Add the User as a member
        //TODO: Use AUTH-ID not HARD-CODE
        databaseReference.child("Members").child("lolxD2").setValue("User Name");
        
       return GenerateQRCodeImage(databaseReference.getKey(),800,800);

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
    @RequiresApi(api = Build.VERSION_CODES.O)
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

            //File path=new File(parentDirectory, File.separator + groupName + File.separator + groupName + ".png");
            // MatrixToImageWriter.writeToPath(bitMatrix, "PNG",path.toPath());

            Log.d("QrCode", "generateQRCodeImage: Done");
            return bitmap;
        }catch (WriterException e){
            e.printStackTrace();
        }
        return null;
    }

    public boolean JoinGroup(String groupId){
        DatabaseReference databaseReference = mDataBase.getReference().child("Groups").child(groupId).child("Members");
        //Add the User as a member
        databaseReference.updateChildren(new HashMap<String,Object>() {{
            //TODO: Use AUTH-ID not HARD-CODE
            put("lolxD3", (Object)"User Name");
        }});

        return true;
    }
}

