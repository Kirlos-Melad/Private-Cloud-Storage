package com.example.privatecloudstorage.controller;

// Android Libraries
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

// 3rd Party Libraries
import com.example.privatecloudstorage.databinding.ActivityCreateGroupBinding;
import com.example.privatecloudstorage.model.Group;
import com.google.zxing.WriterException;

// Java Libraries
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.jar.Attributes;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles the user input and Create a NEW group accordingly
 */
public class CreateGroupActivity extends AppCompatActivity {
    // Used for debugging
    private static final String TAG = "CreateGroupActivity";

    private ActivityCreateGroupBinding _ActivityCreateGroupBinding;

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _ActivityCreateGroupBinding = ActivityCreateGroupBinding.inflate(getLayoutInflater());
        setContentView(_ActivityCreateGroupBinding.getRoot());

        _ActivityCreateGroupBinding.CreateGroup.setOnClickListener(view -> {
            Group group = ReadInput();

            // skip if user input wasn't a success
            if(group == null) return;
            // skip if group wasn't created
            if(!group.CreateGroup(false)) return;
            // Try Generating & Saving the QR Code
            try {
                group.GenerateGroupQRCode(getFilesDir().toString());
            } catch (IOException e) {
                Toast.makeText(this, "Failed to save QR Code", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            } catch (WriterException e) {
                Toast.makeText(this, "Failed to create QR Code", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }

            // Tell user it was a success
            Toast.makeText(this, "Group Created Successfully", Toast.LENGTH_LONG).show();

            // Go to the group activity
            startActivity(new Intent(CreateGroupActivity.this,GroupListActivity.class));
            //Intent intent = new Intent(this, GroupContentActivity.class);
            //intent.putExtra("selectedGroupKey",group.getId());
            //intent.putExtra("selectedGroupName", group.getName());

            //startActivity(intent);
        });
    }

    /**
     * Extract and Validate User Input
     *
     * @return return Group if successful else Null
     */
    private Group ReadInput() {
        //Get Values entered by user
        String groupName = _ActivityCreateGroupBinding.Name.getText().toString().trim();
        String groupDescription = _ActivityCreateGroupBinding.Description.getText().toString().trim();
        String password = _ActivityCreateGroupBinding.Password.getText().toString().trim();
        String confirmPassword = _ActivityCreateGroupBinding.ConfirmPassword.getText().toString().trim();

        if(TextUtils.isEmpty(groupName)){
            _ActivityCreateGroupBinding.groupname.setError("Group Name is required");
            return null;
        }
        if(TextUtils.isEmpty(groupDescription)){
            _ActivityCreateGroupBinding.groupdesc.setError("Group Description is required");
            return null;
        }
        if(TextUtils.isEmpty(password)){
            _ActivityCreateGroupBinding.grouppass.setError("Group password is required");
            return null;
        }
        if(TextUtils.isEmpty(confirmPassword)){
            _ActivityCreateGroupBinding.repass.setError("Group Repassword is required");
            return null;
        }
        // Check if password matches
        if(!password.equals(confirmPassword)){
            Toast.makeText(this, "Password doesn't match", Toast.LENGTH_LONG).show();
            return null;
        }

        // Check if password if accepted
        if(!isValidPassword(password)){
            Toast.makeText(this, "Password is not Strong Enough", Toast.LENGTH_LONG).show();
            return null;
        }

        // Create new group with the values provided by the user
        return new Group(groupName, groupDescription, password);
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