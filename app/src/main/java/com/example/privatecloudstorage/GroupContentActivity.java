package com.example.privatecloudstorage;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;

public class GroupContentActivity extends AppCompatActivity {
    private Button _AddData;
    private Button _SeeGroupData;
    private ImageView _QrCode;
    private String mSelectedGroupName;
    private String mSelectedGroupKey;
    FirebaseDatabaseManager mFirebaseDatabaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //receive parameters from previous activity(GroupListActivity)
        Bundle bundle = getIntent().getExtras();
        if(bundle == null)
            finish();

        mSelectedGroupName = bundle.getString("selectedGroupName");
        mSelectedGroupKey = bundle.getString("selectedGroupKey");
        mFirebaseDatabaseManager=FirebaseDatabaseManager.getInstance();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_content);

        _QrCode=findViewById(R.id.QrCodeImage);
        File file=new File(getFilesDir() + File.separator + mSelectedGroupKey + " " + mSelectedGroupName + File.separator + mSelectedGroupName + " QR Code" + ".png");
        _QrCode.setImageURI(Uri.fromFile(file) );

        _AddData=findViewById(R.id.AddData);
        _AddData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkPermission()){
                Intent intent = new Intent(GroupContentActivity.this,FileManagerListActivity.class);
                String path = Environment.getExternalStorageDirectory().getPath();
                intent.putExtra("path",path);
                intent.putExtra("action",Intent.ACTION_GET_CONTENT);
                intent.putExtra("selectedGroupName", mSelectedGroupName);
                intent.putExtra("selectedGroupKey", mSelectedGroupKey);
                startActivity(intent);
                }
                else{
                    requestPermission();
                }
            }
        });

        _SeeGroupData=findViewById(R.id.SeeGroupData);
        _SeeGroupData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkPermission()) {
                    Intent intent = new Intent(GroupContentActivity.this, FileManagerListActivity.class);
                    String path = getFilesDir()+ File.separator + mSelectedGroupKey + " " + mSelectedGroupName;
                    intent.putExtra("path", path);
                    intent.putExtra("action",Intent.ACTION_VIEW);
                    startActivity(intent);
                }
                else
                    requestPermission();
            }
        });
    }

    /** Check if permisson is granted or not
     * true : if granted
     * false : if not
     * @return true/false
     */
    private boolean checkPermission(){
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(result == PackageManager.PERMISSION_GRANTED)
            return true;
        else
            return false;
    }

    /**
     * request permission if it's not granted
     */
    private void requestPermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE))
            Toast.makeText(this,"Storage Perimission is required, please allow from settings",Toast.LENGTH_SHORT);
        else
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
    }
}
