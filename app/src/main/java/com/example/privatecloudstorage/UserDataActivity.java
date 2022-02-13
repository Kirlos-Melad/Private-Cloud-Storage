package com.example.privatecloudstorage;

/**
 * contains user data
 */

//TODO: Change class name
public class UserDataActivity {
    private String mEmail, mPass, mUserName;

    public UserDataActivity() {
    }

    /**
     * constractur of the class to assign data
     * @param email the user email
     * @param pass the user password
     * @param userName the user name
     */

    //TODO: Remove password variable -- Add UID -- Add isOnline boolean
    public UserDataActivity(String email, String pass, String userName) {
        mEmail = mEmail;
        mPass = mPass;
        mUserName = mUserName;
    }

    //TODO: remove any Prefix and unused Setters & Getters
    public String getmEmail() {
        return mEmail;
    }

    public void setmEmail(String mEmail) {
        this.mEmail = mEmail;
    }

    public String getmPass() {
        return mPass;
    }

    public void setmPass(String mPass) {
        this.mPass = mPass;
    }

    public String getmUserName() {
        return mUserName;
    }

    public void setmUserName(String mUserName) {
        this.mUserName = mUserName;
    }
}
