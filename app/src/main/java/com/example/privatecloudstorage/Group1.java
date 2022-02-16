package com.example.privatecloudstorage;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

/**
 * repreesents users group 1, to view files shared withe users in the same group
 * to add new files into the group
 */
public class Group1 extends AppCompatActivity {
    private Uri mFileUri;
    private String mFilePath;
    private StorageTask mUploadTask;
    private StorageReference mStorageRef;
    private DatabaseReference mDatabaseRef;
    private ProgressBar mProgressBar;
    private EditText mEditTextFileName;
    private TextView mTextViewShowUploads;
    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group1);

        mStorageRef = FirebaseStorage.getInstance().getReference("uploads");

        mProgressBar = findViewById(R.id.progress_bar);
        mEditTextFileName = findViewById(R.id.edit_text_file_name);
        Button _btnChooseFile = (Button) findViewById(R.id.pickFile_btn);
        Button _sendFile = (Button) findViewById(R.id.send_btn);
        mTextViewShowUploads = findViewById(R.id.text_view_show_uploads);
        //after clicking the button, on click opens file manager
        _btnChooseFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                chooseFile.setType("*/*");
                chooseFile = Intent.createChooser(chooseFile, "Choose a file");
                startActivityForResult(chooseFile, PICK_IMAGE_REQUEST);
            }
        });
        _sendFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mUploadTask != null && mUploadTask.isInProgress()) {
                    Toast.makeText(Group1.this, "Sending..", Toast.LENGTH_SHORT).show();
                } else {
                    uploadFile();
                }
            }
        });

        mTextViewShowUploads.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Group1.this, UploadsData.class);
                startActivity(intent);
            }
        });

    }

    private String getFileExtension(Uri uri) {
        ContentResolver contentResolverR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(contentResolverR.getType(uri));
    }

    private void uploadFile() {
        if (mFileUri != null) {
            StorageReference fileReference = mStorageRef.child(System.currentTimeMillis()+ "." + getFileExtension(mFileUri));

            mUploadTask = fileReference.putFile(mFileUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mProgressBar.setProgress(0);
                                }
                            }, 500);

                            Toast.makeText(Group1.this, "Upload successful", Toast.LENGTH_SHORT).show();
                            Upload upload = new Upload(mEditTextFileName.getText().toString().trim(),taskSnapshot.getMetadata().getReference().getDownloadUrl().toString());
                            String uploadId = mDatabaseRef.push().getKey();
                            mDatabaseRef.child(uploadId).setValue(upload);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(Group1.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                            mProgressBar.setProgress((int) progress);
                        }
                    });
        } else {
            Toast.makeText(Group1.this, "No file selected", Toast.LENGTH_SHORT).show();
        }
    }

    //After taking image from gallery or camera then come back to current activity
    // first method that calls is onActivityResult(int requestCode, int resultCode, Intent data).
    // We get the result in this method.

}