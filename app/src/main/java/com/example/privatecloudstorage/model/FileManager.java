package com.example.privatecloudstorage.model;

import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.example.privatecloudstorage.interfaces.IFileEventListener;
import com.example.privatecloudstorage.interfaces.IFileNotify;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class FileManager implements IFileNotify {
    // Used for debugging
    public static final String TAG = "FileManager";

    private static FileManager mFileManager;

    private final File mApplicationDirectory;
    private final String TEMPORARY_DIRECTORY = "Temporary";
    private final String STRIPPED_FILES_DIRECTORY = "Stripped Files";
    private final String NORMAL_FILES_DIRECTORY = "Normal Files";
    private final String USER_DIRECTORY = "User";

    


    private final Vector<IFileEventListener> mObserver;

    // Events
    public static final byte CREATE = 0x01;
    public static final byte DELETE = 0x02;
    public static final byte RENAME = 0x03;
    public static final byte CHANGE = 0x04;

    
    // Modes
    public static final byte NORMAL = 0x00;
    public static final byte STRIP = (byte) 0xff; // -1


    private FileManager(File managedDirectory) {
        mApplicationDirectory = managedDirectory;
        mObserver = new Vector<>();

        // Create needed directories
        CreateDirectory(new File(mApplicationDirectory.toString(), TEMPORARY_DIRECTORY));

        CreateDirectory(new File(mApplicationDirectory.toString(), USER_DIRECTORY));

       
    }

    public static FileManager createInstance(File parentDirectory){
        if(mFileManager == null)
            mFileManager  = new FileManager(parentDirectory);

        return mFileManager;
    }

    public static FileManager getInstance(){
        return mFileManager;
    }

    public String GetApplicationDirectory(){
        return mApplicationDirectory.toString();
    }

    public String GetTemporaryDirectory(){
        return mApplicationDirectory.toString() + File.separator + TEMPORARY_DIRECTORY;
    }

    /**
     * Add new event listener
     *
     * @param fileEventListener event listener
     *
     * @return return false if an id already exists
     */
    public boolean AddEventListener(IFileEventListener fileEventListener){
        if(mObserver.contains(fileEventListener))
            return false;

        mObserver.add(fileEventListener);
        return true;
    }

    /**
     * Remove an event listener
     * 
     * @return true if an object exists and got removed
     */
    public boolean RemoveEventListener(IFileEventListener fileEventListener){
        return mObserver.remove(fileEventListener);
    }

    /**
     * Notifies all listener by the change
     *
     * @param event type of event
     * @param oldFile file before event
     * @param newFile file after event - can be null
     */
    @Override
    public void Notify(byte event,byte mode, File oldFile, File newFile){
        if(!oldFile.toString().contains(TEMPORARY_DIRECTORY)){
            for(IFileEventListener fileEventListener : mObserver){
                switch (event){
                    case CREATE:
                        fileEventListener.onFileAdded(oldFile,mode);
                        break;
                    case CHANGE:
                        fileEventListener.onFileChanged(oldFile,mode);
                        break;
                    case RENAME:
                        fileEventListener.onFileRenamed(newFile, oldFile.getName(),mode);
                        break;
                    case DELETE:
                        fileEventListener.onFileRemoved(oldFile);
                        break;
                }
            }
        }
    }

    /**
     * Rename a file
     *
     * @param oldFile file path
     * @param newName new file name
     * @return true on success
     */
    public boolean RenameFile(File oldFile, String newName, byte mode){
        File newFile = new File(oldFile.getPath().substring(0, oldFile.getPath().lastIndexOf(File.separator)), newName);
        boolean isRenamed = oldFile.renameTo(newFile);

        if(isRenamed){
            Notify(RENAME,mode, oldFile ,newFile);
            return true;
        }

        return false;
    }

    /**
     * Create a new file
     *
     * @param file file path
     *
     * @return true on success
     *
     * @throws IOException
     */
    public boolean CreateFile(File file,byte mode) throws IOException {
        boolean success = file.createNewFile();

        if(success){
            Notify(CREATE,mode, file, null);
            return true;
        }

        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public FileTime getLastModificationDate(File file) throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(file.toPath(),BasicFileAttributes.class);
        FileTime lastModified = attributes.lastModifiedTime();
        return lastModified;
    }

    /**
     * Create a new copy of the file
     *
     * @param src file path
     * @param dst copy path
     *
     * @return true on success
     *
     * @throws IOException
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public boolean CopyFile(Path src, Path dst,byte mode) throws IOException {
        Files.copy(src, dst);

        Notify(CREATE, mode, new File(dst.toString()),null);

        return true;
    }

    /**
     * Move file to a new location
     * Will cause CREATE & DELETE events to be triggered
     *
     * @param src old path
     * @param dst new path
     *
     * @return true on success
     *
     * @throws IOException
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public boolean MoveFile(Path src, Path dst, byte mode) throws IOException {
        return (CopyFile(src, dst, mode) && DeleteFile(src.toFile(),mode));
    }

    /**
     * Create a new directory
     *
     * @param directory directory path
     *
     * @return true on success
     */
    public boolean CreateDirectory(File directory) {
        if(!directory.exists()){
            boolean success = directory.mkdir();

            if(success){
                Notify(CREATE, NORMAL, directory, null);
            }

            return success;
        }

        return false;
    }

    public void DeleteDirectory(File directory){
        directory.delete();
    }

    /**
     * Create a new directory and its parents if doesn't exist
     *
     * @param directory directory path
     *
     * @return true on success
     */
    public boolean CreateDirectoryWithParents(File directory){
        if(!directory.exists()){
            boolean success = directory.mkdirs();

            if(success){
                Notify(CREATE, NORMAL, directory, null);
            }

            return success;
        }

        return false;
    }

    /**
     * Delete an existing file
     *
     * @param file path
     *
     * @return true on success
     */
    public boolean DeleteFile(File file, byte mode) {
        boolean success = file.delete();
        if(success){
            Notify(DELETE, mode, file,null);
            return true;
        }

        return false;
    }

    /**
     * Create image file
     *
     * @param image the image
     * @param path path to be saved
     *
     * @return true on success
     *
     * @throws IOException exception is thrown if the path is wrong
     */
    public boolean CreateImage(Bitmap image, File path) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(path);
        image.compress(Bitmap.CompressFormat.PNG, 85, fileOutputStream);
        fileOutputStream.flush();
        fileOutputStream.close();

        Notify(CREATE, NORMAL, path,null);

        return true;
    }

    /**
     * Encrypt/Decrypt files
     *
     * @param file file to be Encrypted/Decrypted
     * @param group group that the file belongs to
     * @param cipherMode Encrypt or Decrypt
     *
     * @return true on success
     *
     * @throws IOException exception is thrown if the path is wrong
     */
    public File EncryptDecryptFile(File file, String fileName, Group group, int cipherMode) throws IOException {
        try {
            String HASH_FUNCTION = "SHA-256";
            String CIPHER_ALGORITHM = "AES";
            String CIPHER_TRANSFORMATION = "AES";

            // Use group info as a key
            String key = group.getId();

            MessageDigest messageDigest = MessageDigest.getInstance(HASH_FUNCTION);
            // Convert the key to hash value
            byte[] hashedPassword = messageDigest.digest(key.getBytes(StandardCharsets.UTF_8));
            // Save hashed key as HexString
            key = new BigInteger(1, hashedPassword).toString(16);

            // Initializing a Cipher Object with either Encryption or Decryption mode
            Key secretKey = new SecretKeySpec(key.substring(0,32).getBytes(StandardCharsets.UTF_8), CIPHER_ALGORITHM);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
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
                outputFile = new File(mApplicationDirectory.toString() + File.separator + TEMPORARY_DIRECTORY, fileName);
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

 
    @RequiresApi(api = Build.VERSION_CODES.O)
    public ArrayList<File> SplitFile(File f, ArrayList<String> fileNames) throws IOException {
        long originalFileSize = Files.size(f.toPath());
        int partSize =(int) Math.ceil((double)(originalFileSize)/fileNames.size());
        byte[] buffer = new byte[partSize];
        String fileName = f.getName();
        ArrayList<File> Files = new ArrayList<File>();

        try (FileInputStream fis = new FileInputStream(f);
             BufferedInputStream bis = new BufferedInputStream(fis)) {

            int bytesAmount = 0;
            int i=0;
            while ((bytesAmount = bis.read(buffer)) > 0) {
                //write each chunk of data into separate file with different number in name
                String filePartName = fileName + " " + fileNames.get(i++);
                File newFile = new File(f.getParent(), filePartName);
                try (FileOutputStream out = new FileOutputStream(newFile)) {
                    Files.add(newFile);
                    out.write(buffer, 0, bytesAmount);
                }
            }
        }
        return Files;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void MergeFiles(List<File> files, File into) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(into);
             BufferedOutputStream mergingStream = new BufferedOutputStream(fos)) {
            for (File f : files)
                Files.copy(f.toPath(), mergingStream);
        }
    }
}
