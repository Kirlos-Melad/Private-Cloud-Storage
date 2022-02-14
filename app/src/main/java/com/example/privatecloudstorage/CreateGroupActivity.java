package com.example.privatecloudstorage;

// Android Libraries
import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

// Java Libraries
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class CreateGroupActivity extends AppCompatActivity {
    EditText _GroupName;
    EditText _GroupDescription;
    EditText _Password;
    EditText _RePassword;
    Button _CreateGroup;

    FirebaseDatabaseManager mFirebaseDatabaseManager;

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

        _CreateGroup.setOnClickListener(view -> {
            Group group = ReadInput();
            CreateGroup(group);
        });
    }

    /**
     * Extract and Validate User Input
     *
     * @return return Group if successful else Null
     */
    private Group ReadInput(){
        //Get Values entered by user
        String groupName = _GroupName.getText().toString();
        String groupDescription = _GroupDescription.getText().toString();
        String password = _Password.getText().toString();
        String rePassword = _RePassword.getText().toString();

        if(password.equals(rePassword)){
            // Create new group with the values provided by the user
            Group group = new Group();
            group.mName = groupName;
            group.mDescription = groupDescription;
            group.setPassword(password);
            return group;
        }

        return null;
    }

    /**
     * Add new group to firebase
     * Create group folder
     * Save group QR Code
     *
     * @param group
     */
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

                //TODO: Delete this
                ImageView img=(ImageView) findViewById(R.id.img);
                img.setImageBitmap(bitmap);
            }
        }
    }
}