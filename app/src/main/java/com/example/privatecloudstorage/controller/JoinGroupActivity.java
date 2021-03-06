package com.example.privatecloudstorage.controller;

// Android Libraries
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

// 3rd Party Libraries
import me.dm7.barcodescanner.zxing.ZXingScannerView;

import com.example.privatecloudstorage.model.FirebaseDatabaseManager;
import com.example.privatecloudstorage.model.Group;
import com.example.privatecloudstorage.model.ManagersMediator;
import com.google.zxing.Result;
import com.google.zxing.WriterException;

// Java Libraries
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * Opens the camera to scan the group QR Code
 * And decode/extract the information
 */
public class JoinGroupActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {
    // Used for debugging
    private static final String TAG = "JoinGroupActivity";
    private static final int CAMERA_PERMISSION_REQUEST = 123;

    ZXingScannerView mScannerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ask user for camera permission if not permitted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        }

        // Start the Camera
        mScannerView = new ZXingScannerView(this);
        setContentView(mScannerView);
    }

    /**
     * Handles camera readings/result
     *
     * @param result camera readings
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void handleResult(Result result) {
        super.onBackPressed();

        // groupInformation array holds [Group ID, Group Name]
        String[] groupInformation = result.getText().split(",");
        Group group = new Group(groupInformation[0], groupInformation[1], "", "","");

        if(!ManagersMediator.getInstance().JoinGroup(group)){
            Toast.makeText(this, "Failed to Join group", Toast.LENGTH_LONG).show();
            return;
        }

        // Tell user it was a success
        Toast.makeText(this, "Joined Group Successfully", Toast.LENGTH_LONG).show();

        // Go to the group activity
        Intent intent = new Intent(this, GroupSliderActivity.class);
        intent.putExtra("selectedGroupKey", group.getId());
        intent.putExtra("selectedGroupName", group.getName());

        startActivity(intent);
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
