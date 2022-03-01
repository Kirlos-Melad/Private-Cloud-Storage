package com.example.privatecloudstorage.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class EncryptDecryptFile {
    private File mFile;
    private String mKey;
    private static final String mAlgorithm = "AES";
    private static final String mTransformation = "AES";

    public EncryptDecryptFile(Group group) throws NoSuchAlgorithmException {
        this.mKey = group.getId() + group.getName() + group.getPassword();
        this.mKey.hashCode();

        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        // Convert the password to hash value
        byte[] hashedPassword = messageDigest.digest(mKey.getBytes(StandardCharsets.UTF_8));
        // Save hashed password as HexString
        mKey = new BigInteger(1, hashedPassword).toString(16);
    }

    public File DoCrypto(int cipherMode, File inputFile, File outputFile) {
        try {
            //initializing a Cipher Object with either Encryption mode or Decryption
            Key secretKey = new SecretKeySpec(mKey.getBytes(), mAlgorithm);
            Cipher cipher = Cipher.getInstance(mTransformation);
            cipher.init(cipherMode, secretKey);

            //reading from the input file (that needs to be Encrypted/Decrypted) to a byte array
            FileInputStream inputStream = new FileInputStream(inputFile);
            byte[] inputFileBytes = new byte[(int) inputFile.length()];
            inputStream.read(inputFileBytes);

            //Encrypting/Decrypting the input file bytes
            byte[] encryptedBytes = cipher.doFinal(inputFileBytes);

            FileOutputStream outputStream = new FileOutputStream(outputFile);
            outputStream.write(encryptedBytes);

            inputStream.close();
            outputStream.close();

            mFile = outputFile;

            return outputFile;

        } catch (NoSuchPaddingException | InvalidKeyException | NoSuchAlgorithmException
                | BadPaddingException | IllegalBlockSizeException | IOException ex) {
            //add an exception later
            return null;
        }

    }

    public boolean Delete(){
        return mFile.delete();
    }
    public String getKey() {
        return mKey;
    }
}
