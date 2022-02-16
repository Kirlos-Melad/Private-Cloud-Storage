package com.example.privatecloudstorage;

import static android.os.Environment.getExternalStorageDirectory;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;


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
                Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                chooseFile.setType("*/*");
                chooseFile = Intent.createChooser(chooseFile, "Choose a file");
                startActivityForResult(chooseFile, PICK_IMAGE_REQUEST);
            }
        });
        _SeeGroupData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(getFilesDir() + File.separator + selectedGroupName), "*/*");
                startActivityForResult(intent, 2);
            }
        });
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
                        File file= new File(getFilesDir() + File.separator + selectedGroupName + File.separator + Uri.fromFile(new File(mFilePath)).getLastPathSegment());
                        Files.copy(getExternalFilesDir(mFilePath).toPath(),file.toPath());
                        //copy(getExternalFilesDir(mFilePath),file);
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