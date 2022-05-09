package com.example.privatecloudstorage.model;

// Java Libraries
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;

/**
 * Handles the group creation
 */

public class Group {
    private static final String TAG = "Group";
    private String mId;
    private String mName;
    private String mDescription;
    private String mPassword;

    /**
     *
     * @param name
     * @param description
     * @param password
     * @throws NoSuchAlgorithmException failed to use SHA-256 to hash the password
     */
    public Group(String id, String name, String description, String password)  {
        this(name, description, password);
        mId = id;
    }

    /**
     *
     * @param name
     * @param description
     * @param password
     * @throws NoSuchAlgorithmException failed to use SHA-256 to hash the password
     */
    public Group(String name, String description, String password)  {
        this.mName = name;
        this.mDescription = description;
        setPassword(password);
    }

    /**
     * Recieves the password from the user and hash it using SHA-256 Hashing function
     *
     * @param password sent Password from user
     * @return true when the password is hashed and save, false when there's an exception
     */
    private void setPassword(String password) {
        if(password.isEmpty())
            return;
        // Create Hashing Function instance of SHA-256
        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        // Convert the password to hash value
        byte[] hashedPassword = messageDigest.digest(password.getBytes(StandardCharsets.UTF_8));
        // Save hashed password as HexString
        mPassword = new BigInteger(1, hashedPassword).toString(16);
    }

    public String getPassword(){
        return mPassword;
    }

    public void setId(String id){
        mId = id;
    }

    public String getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public String getDescription() {
        return mDescription;
    }

    /**
     * Add new group to firebase
     * Create group folder in the root directory
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public boolean CreateGroup(boolean isJoin){
        if(isJoin){
            // Join an existing group in the Real-Time Database
            FirebaseDatabaseManager.getInstance().JoinGroup(this);
        }
        else {
            // Add this group to Real-Time Database
            mId = FirebaseDatabaseManager.getInstance().AddGroup(this);
        }

        // Create group folder = GroupID GroupName
        File groupDirectory = new File(FileManager.getInstance().GetApplicationDirectory(), mId + " " + mName);
        boolean isCreated = FileManager.getInstance().CreateDirectory(groupDirectory);

        // Create Stripped folder = GroupID GroupName/Stripped Files
        File StrippedDirectory = new File(FileManager.getInstance().GetApplicationDirectory()+File.separator+ mId + " " + mName,"Stripped Files");
        FileManager.getInstance().CreateDirectory(StrippedDirectory);

        // Create Normal Files = GroupID GroupName/Normal Files
        File NormaDirectory = new File(FileManager.getInstance().GetApplicationDirectory()+File.separator+ mId + " " +
                mName , "Normal Files");
        FileManager.getInstance().CreateDirectory(NormaDirectory);

        // Create Merged Files = GroupID GroupName/Merged Files
        File MergedDirectory = new File(FileManager.getInstance().GetApplicationDirectory()+File.separator+ mId + " " +
                mName , "Merged Files");
        FileManager.getInstance().CreateDirectory(MergedDirectory);

        return isCreated;
    }

    /**
     * Generate and Save the group QR code to the group folder
     *
     * @param rootDirectory root directory of the group folder
     *
     * @throws WriterException if can't generate the QR Code
     * @throws IOException root directory isn't correct or can't save the QR Code to the device
     */
    public void GenerateGroupQRCode(String rootDirectory) throws IOException, WriterException {
        // Create QR Code
        Bitmap bitmap = GenerateQRCodeImage(800,800);

        // Save the QR code in the folder -> path = ${rootDir}/${id} ${name}/${name} QR Code.png
        File image = new File(rootDirectory, mId + " " + mName + File.separator + mName + " QR Code.png");
        FileManager.getInstance().CreateImage(bitmap, image);
    }

    /**
     * Generate QR Code
     *
     * @param width
     * @param height
     *
     * @return return QR Code as Bitmap
     */
    private Bitmap GenerateQRCodeImage(int width, int height) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        // Encode the information to the QR Code image
        BitMatrix bitMatrix = qrCodeWriter.encode(mId + "," + mName, BarcodeFormat.QR_CODE, width, height);

        //Convert the BitMatrix to a Bitmap to be able to use it in android
        Bitmap bitmap = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return bitmap;
    }
}