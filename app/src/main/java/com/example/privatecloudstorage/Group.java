package com.example.privatecloudstorage;

// Java Libraries
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * class Group to contain each Group's name, Description, and Password
 */

public class Group {
    private static final String TAG = "Group";
    public String mName;
    public String mDescription;
    //Hashed Password
    private String mPassword;

    /**
     * Recieves the password from the user and hash it using SHA-256 Hashing function
     *
     * @param password sent Password from user
     * @return true when the password is hashed and save, false when there's an exception
     */


    public boolean setPassword(String password) {
        try {
            // Create Hashing Function instance of SHA-256
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            // Convert the password to hash value
            byte[] hashedPassword = messageDigest.digest(password.getBytes(StandardCharsets.UTF_8));
            // Save hashed password as HexString
            mPassword = new BigInteger(1, hashedPassword).toString(16);

            return true;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return false;
    }
}
