package com.example.privatecloudstorage;

// Android Libraries
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

// 3rd Party Libraries
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

// Java Libraries
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles the user input and Create a NEW group accordingly
 */
public class CreateGroupActivity extends AppCompatActivity {
    // Used for debugging
    private static final String TAG = "Create Group Activity";

    EditText _GroupName;
    EditText _GroupDescription;
    EditText _Password;
    EditText _RePassword;
    Button _CreateGroup;
    FirebaseDatabaseManager mFirebaseDatabaseManager;

    @RequiresApi(api = Build.VERSION_CODES.Q)
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
            String groupKey = CreateGroup(group);
            Toast.makeText(CreateGroupActivity.this, "Group Created Successfully", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(CreateGroupActivity.this,GroupContentActivity.class);
            intent.putExtra("selectedGroupName", group.mName);
            intent.putExtra("selectedGroupKey",groupKey );
            startActivity(intent);
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
        if(!isValidPassword(password)){
            Toast.makeText(CreateGroupActivity.this, "Password is not Strong Enough", Toast.LENGTH_LONG).show();
        }
        if(password.equals(rePassword)){
            // Create new group with the values provided by the user
            Group group = new Group();
            group.mName = groupName;
            group.mDescription = groupDescription;
            group.setPassword(password);
            return group;
        }else {
            Toast.makeText(CreateGroupActivity.this, "Invalid Password", Toast.LENGTH_LONG).show();
        }
        return null;
    }

    /**
     * Add new group to firebase
     * Create group folder
     * Generate and Save group QR Code
     *
     * @param group
     */
    private String CreateGroup(Group group){
        // Add new group to firebase
        Pair<String, String> groupInformation = mFirebaseDatabaseManager.AddGroup(group);
        if(groupInformation != null){
            // Create group folder = GroupID GroupName
            File directory = new File(getFilesDir().toString() + File.separator + groupInformation.first + " " + groupInformation.second);
            boolean success = true;
            if (!directory.exists()) success = directory.mkdir();
            if(success){
                try {
                    Bitmap bitmap = GenerateQRCodeImage(groupInformation,800,800);
                    // Save the QR code in the folder and show it
                    File image = new File(directory, group.mName + " QR Code" + ".png");
                    FileOutputStream fileOutputStream = new FileOutputStream(image);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 85, fileOutputStream);
                    fileOutputStream.flush();
                    fileOutputStream.close();
                    return groupInformation.first;
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * Generate QR Code
     *
     * @param groupInformation [Group ID, Group Name]
     * @param width
     * @param height
     *
     * @return return QR Code as Bitmap
     */
    private Bitmap GenerateQRCodeImage(Pair<String, String> groupInformation, int width, int height) {
        try{
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            // Encode the information to the QR Code image
            BitMatrix bitMatrix = qrCodeWriter.encode(groupInformation.first + "," + groupInformation.second, BarcodeFormat.QR_CODE, width, height);

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
     * check password strength
     * @param password the sign up password
     * @return true if the sign up password is strong
     */
    public boolean isValidPassword(final String password) {

        Pattern pattern;
        Matcher matcher;
//      ^                 # start-of-string
//      (?=.*[0-9])       # a digit must occur at least once
//      (?=.*[a-z])       # a lower case letter must occur at least once
//      (?=.*[A-Z])       # an upper case letter must occur at least once
//      (?=.*[@#$%^&+=])  # a special character must occur at least once you can replace with your special characters
//      (?=\\S+$)         # no whitespace allowed in the entire string
//      .{4,}             # anything, at least six places though
//      $                 # end-of-string
        final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{4,}$";

        pattern = Pattern.compile(PASSWORD_PATTERN);
        matcher = pattern.matcher(password);

        return matcher.matches();
    }
}