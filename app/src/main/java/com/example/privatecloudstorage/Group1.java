package com.example.privatecloudstorage;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * repreesents users group 1, to view files shared withe users in the same group
 * to add new files into the group
 */
public class Group1 extends AppCompatActivity {
    private Uri mFileUri;
    private String mFilePath;
    Intent mIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group1);

        Button _btnChooseFile = (Button) findViewById(R.id.add_btn);
        //tvItemPath = (TextView) rootView.findViewById(R.id.tv_file_path);

        //after clicking the button, on click opens file manager
        _btnChooseFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                chooseFile.setType("*/*");
                chooseFile = Intent.createChooser(chooseFile, "Choose a file");
                startActivityForResult(chooseFile, 1);
            }
        });


    }

    //After taking image from gallery or camera then come back to current activity
    // first method that calls is onActivityResult(int requestCode, int resultCode, Intent data).
    // We get the result in this method.
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1:
                if (resultCode == -1) {
                    mFileUri = data.getData();
                    mFilePath = mFileUri.getPath();
                    //tvItemPath.setText(filePath);
                    Toast.makeText(Group1.this, mFilePath, Toast.LENGTH_LONG).show();
                    System.out.println(mFilePath);
                }

                break;
        }
    }

    public Uri getFileUri() {
        return mFileUri;
    }
}