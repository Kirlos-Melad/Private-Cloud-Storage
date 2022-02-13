package com.example.privatecloudstorage;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CreateGroupActivity extends AppCompatActivity {
    EditText _GroupName;
    EditText _GroupDescription;
    EditText _Password;
    EditText _RePassword;
    Button _CreateGroup;

    FirebaseDatabaseManager mFirebaseDatabaseManager;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        mFirebaseDatabaseManager = FirebaseDatabaseManager.getInstance();

        _GroupName = (EditText)findViewById(R.id.GroupName);
        _GroupDescription = (EditText)findViewById(R.id.GroupDescription);
        _Password = (EditText)findViewById(R.id.Password);
        _RePassword = (EditText)findViewById(R.id.RePassword);

        _CreateGroup = (Button)findViewById(R.id.CreateGroup);

        //Validate Data onClick
        _CreateGroup.setOnClickListener(view -> {
            ReadInput();


        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void ReadInput(){
        //Get Values entered by user
        String groupName = _GroupName.getText().toString();
        String groupDescription = _GroupDescription.getText().toString();
        String password = _Password.getText().toString();
        String rePassword = _RePassword.getText().toString();

        //Check if Password is correct
        if(password.equals(rePassword)){

            // Create new group with the values provided by the user
            Group group = new Group();
            group.mName = groupName;
            group.mDescription = groupDescription;

            try {
                // Create Hashing Function instance of SHA-256
                MessageDigest hashedPassword = MessageDigest.getInstance("SHA-256");
                // Convert the password to hash
                hashedPassword.digest(password.getBytes(StandardCharsets.UTF_8));
                // Save hashed password
                group.mPassword =new BigInteger(1, hashedPassword.digest(password.getBytes(StandardCharsets.UTF_8))).toString(16);
                //group.mPassword = hashedPassword.digest(password.getBytes(StandardCharsets.UTF_8)).toString();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

            CreateGroup(group);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void CreateGroup(Group group){
        // Add new group to firebase
        Bitmap bitmap = mFirebaseDatabaseManager.AddGroup(group);

        if(bitmap!=null){
            // Create group folder
            File directory = new File(getFilesDir().toString() + File.separator + group.mName);
            boolean success = true;
            if (!directory.exists()) success = directory.mkdirs();

            if(success){
                try {
                    // Save the QR code in the folder and show it
                    File image = new File(directory, group.mName + " QR Code" + ".png");
                    FileOutputStream fileOutputStream = new FileOutputStream(image);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 85, fileOutputStream);
                    fileOutputStream.flush();
                    fileOutputStream.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ImageView img=(ImageView) findViewById(R.id.img);
                img.setImageBitmap(bitmap);
            }
        }
    }
}