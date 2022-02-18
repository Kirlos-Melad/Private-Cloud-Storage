package com.example.privatecloudstorage;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;


public class GroupContentActivity extends AppCompatActivity {
    Button _AddData;
    Button _SeeGroupData;
    ImageView _QrCode;
    private Uri mFileUri;
    private String mFilePath;


    String selectedGroupName;
    String selectedGroupKey;
    FirebaseDatabaseManager mFirebaseDatabaseManager;
    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //receive parameters from previous activity(GroupListActivity)
        Bundle bundle = getIntent().getExtras();
        if(bundle == null)
            finish();

        selectedGroupName = bundle.getString("selectedGroupName");
        selectedGroupKey = bundle.getString("selectedGroupKey");
        mFirebaseDatabaseManager=FirebaseDatabaseManager.getInstance();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_content);
        _AddData=findViewById(R.id.AddData);
        _SeeGroupData=findViewById(R.id.SeeGroupData);

        _QrCode=findViewById(R.id.QrCodeImage);
        //Bitmap drawable = BitmapFactory.decodeFile(getFilesDir() + File.separator + selectedGroupName + File.separator + selectedGroupName + " QR Code" + ".png");
       File file=new File(getFilesDir() + File.separator + selectedGroupName + File.separator + selectedGroupName + " QR Code" + ".png");
        if(file.exists()){
            Log.d("path", file.getPath());
        }
       _QrCode.setImageURI(Uri.fromFile(file) );

        _AddData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkPermission()){
                Intent intent = new Intent(GroupContentActivity.this,FileManagerListActivity.class);
                String path = Environment.getExternalStorageDirectory().getPath();
                intent.putExtra("path",path);
                startActivity(intent);
                }
                else{
                    requestPermission();
                }
            }
        });
        _SeeGroupData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkPermission()) {
                    Intent intent = new Intent(GroupContentActivity.this, FileManagerListActivity.class);
                    String path = getFilesDir() + File.separator + selectedGroupName;
                    intent.putExtra("path", path);
                    startActivity(intent);
                }
                else
                    requestPermission();
            }
        });
    }

    private boolean checkPermission(){
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(result == PackageManager.PERMISSION_GRANTED)
            return true;
        else
            return false;
    }

    private void requestPermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE))
            Toast.makeText(this,"Storage Perimission is required, please allow from settings",Toast.LENGTH_SHORT);
        else
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
    }


    private void CallNextActivity(Class<?> next){
        Intent intent = new Intent(GroupContentActivity.this, next);
        Bundle bundle = new Bundle();
        bundle.putString("selectedGroupName", selectedGroupName); //Your Group number in listview
        intent.putExtras(bundle); //Put Group number to your next Intent
        bundle.putString("selectedGroupKey", selectedGroupKey); //Your Group number in listview
        intent.putExtras(bundle); //Put Group number to your next Intent
        startActivity(intent);
        finish();
    }
    @RequiresApi(api = Build.VERSION_CODES.O)

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PICK_IMAGE_REQUEST:
                if (resultCode == -1) {
                    mFileUri = data.getData();
                    mFilePath = mFileUri.getPath();
                    //tvItemPath.setText(filePath);
                    Toast.makeText(GroupContentActivity.this, mFilePath, Toast.LENGTH_SHORT).show();
                    System.out.println(mFilePath);

                    //copy the file chosen to the group folder
                    if(getExternalFilesDir(mFilePath).exists())
                    {
                        Log.d("file", "exists:------------------------------- ");
                    }
                    try {
                        File file= new File(getFilesDir() + File.separator + selectedGroupName
                                            + File.separator + Uri.fromFile(new File(mFilePath)).getLastPathSegment());

                        Files.copy(getExternalFilesDir(mFilePath).toPath(),file.toPath(), StandardCopyOption.REPLACE_EXISTING);

                        if(file.exists())
                        {
                            Log.d("file", "exists:------------------------------- ");
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

}