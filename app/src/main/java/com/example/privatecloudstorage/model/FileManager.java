package com.example.privatecloudstorage.model;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class FileManager {
    private static FileManager mFileManager;
    private File mApplicationDirectory;
    private EventListener mEventListener;

    private static final String mTemporaryDirectory = "TempDir";
    private static final String mAlgorithm = "AES";
    private static final String mTransformation = "AES";
    private static final String mHashFunction = "SHA-256";

    // FileManager Events
    public static final int CREATE=1;
    public static final int DELETE=2;
    public static final int RENAME=3;

    // Custom Event Listener
    public interface EventListener {
        void onChildAdded( File file);
        void onChildRemoved( File file);
        void onChildChanged( File oldFile,File newFile);
    }

    private FileManager(File managedDirectory,EventListener eventListener) {
        this.mApplicationDirectory = managedDirectory;
        mEventListener=eventListener;

        CreateDirectory(new File(mApplicationDirectory.toString(), mTemporaryDirectory));
    }

    public static void CreateInstance(File parentDirectory,EventListener eventListener){
        if(mFileManager == null)
            mFileManager  = new FileManager(parentDirectory,eventListener);
    }

    public static FileManager getInstance(){
        return mFileManager;
    }

    public String getApplicationDirectory(){
        return mApplicationDirectory.toString();
    }

    public String getTemporaryDirectory(){
        return mApplicationDirectory.toString() + File.separator + mTemporaryDirectory;
    }

    private void CallOnEvent(int event, File oldFile,File newFile){
        if(oldFile.toString().contains(mApplicationDirectory.toString())){
            switch (event){
                case CREATE:
                    mEventListener.onChildAdded(oldFile);
                    break;
                case RENAME:
                    mEventListener.onChildChanged(oldFile,newFile);
                    break;
                case DELETE:
                    mEventListener.onChildRemoved(oldFile);
                    break;
            }
        }

    }

    public boolean RenameFile(File oldFile, String newName){
        //String extension = oldFile.toString().substring(oldFile.getPath().lastIndexOf("."),oldFile.toString().length());
        File newFile = new File(oldFile.getPath().substring(0, oldFile.getPath().lastIndexOf(File.separator)), newName );
        oldFile.renameTo(newFile);

        CallOnEvent(RENAME, oldFile ,newFile);

        return true;
    }

    public boolean CreateFile(File file) throws IOException {
        boolean success = file.createNewFile();

        if(success){
            CallOnEvent(CREATE,file,null);
            return true;
        }

        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public boolean CopyFile(Path src, Path dst) throws IOException {
        Files.copy(src, dst);

        CallOnEvent(CREATE, new File(dst.toString()),null);

        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public boolean MoveFile(Path src, Path dst) throws IOException {
        return (CopyFile(src, dst) && DeleteFile(src.toFile()));
    }

    public boolean CreateDirectory(File directory) {
        if(!directory.exists())
            return directory.mkdir();

        return false;
    }

    public boolean CreateDirectoryWithParents(File directory){
        if(!directory.exists())
            return directory.mkdirs();

        return false;
    }

    public boolean DeleteFile(File file) {
        boolean success = file.delete();
        if(success){
            CallOnEvent(DELETE, file,null);
            return true;
        }

        return false;
    }

    public boolean CreateImage(Bitmap bitmap, File image) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(image);
        bitmap.compress(Bitmap.CompressFormat.PNG, 85, fileOutputStream);
        fileOutputStream.flush();
        fileOutputStream.close();

        CallOnEvent(CREATE, image,null);

        return true;
    }

    @SuppressLint("LongLogTag")
    public File EncryptDecryptFile(File file, Group group, int cipherMode) throws IOException {
        try {
            // Use group info as a key
            String key = group.getId();

            MessageDigest messageDigest = MessageDigest.getInstance(mHashFunction);
            // Convert the key to hash value
            byte[] hashedPassword = messageDigest.digest(key.getBytes(StandardCharsets.UTF_8));
            // Save hashed key as HexString
            key = new BigInteger(1, hashedPassword).toString(16);

            // Initializing a Cipher Object with either Encryption or Decryption mode
            Key secretKey = new SecretKeySpec(key.substring(0,32).getBytes(StandardCharsets.UTF_8), mAlgorithm);

            Cipher cipher = Cipher.getInstance(mTransformation);
            cipher.init(cipherMode, secretKey);

            // Reading from the input file (that needs to be Encrypted/Decrypted) to a byte array
            FileInputStream inputStream = new FileInputStream(file);
            byte[] inputFileBytes = new byte[(int) file.length()];
            inputStream.read(inputFileBytes);

            //Encrypting/Decrypting the input file bytes
            byte[] encryptedBytes = cipher.doFinal(inputFileBytes);


            // Create output file
            File outputFile;
            if(cipherMode == Cipher.ENCRYPT_MODE){
                String fileName = FirebaseDatabaseManager.getInstance().getFileKey(group.getId());
                outputFile = new File(mApplicationDirectory.toString() + File.separator + mTemporaryDirectory, fileName);
            } else {
                outputFile = file;
            }

            FileOutputStream outputStream = new FileOutputStream(outputFile);
            outputStream.write(encryptedBytes);

            inputStream.close();
            outputStream.close();


            return outputFile;

        } catch (NoSuchPaddingException | InvalidKeyException | NoSuchAlgorithmException
                | BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
            return null;
        }
    }
}
