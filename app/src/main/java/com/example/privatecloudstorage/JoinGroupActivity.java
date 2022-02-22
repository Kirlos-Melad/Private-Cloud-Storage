package com.example.privatecloudstorage;

// Android Libraries

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.zxing.Result;

import java.io.File;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

/**
 * Opens the camera to scan the group QR Code
 * And decode/extract the information
 */
public class JoinGroupActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {
    // Used for debugging
    private static final String TAG = "Join Group Activity";

    FirebaseDatabaseManager mFirebaseDatabaseManager;
    ZXingScannerView mScannerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ask user for camera permission if not permitted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, 123);
        }

        // Close if permission denied
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED){
            finish();
        }
        mFirebaseDatabaseManager = FirebaseDatabaseManager.getInstance();

        // Start the Camera
        mScannerView = new ZXingScannerView(this);
        setContentView(mScannerView);
    }

    /**
     * Handles camera readings/result
     *
     * @param result camera readings
     */
    @Override
    public void handleResult(Result result) {
        super.onBackPressed();

        // groupInformation array holds [Group ID, Group Name]
        String[] groupInformation = result.getText().split(",");

        // If joined successfully Create a folder for the group
        if(mFirebaseDatabaseManager.JoinGroup(groupInformation)) {
            // Group Folder Name = GroupID GroupName
            File directory = new File(getFilesDir().toString() + File.separator + groupInformation[0] + " " + groupInformation[1]);
            if (!directory.exists()) directory.mkdir();
        }
    }

    /**
     * stop camera on click pause
     **/
    @Override
    protected void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }

    /**
     * set the result handler and start camera on resume
     **/
    @Override
    protected void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();
    }
}
