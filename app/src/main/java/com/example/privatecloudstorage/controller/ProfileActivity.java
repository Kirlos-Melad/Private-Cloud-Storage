package com.example.privatecloudstorage.controller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.privatecloudstorage.databinding.ActivityProfileBinding;
import com.example.privatecloudstorage.model.FirebaseAuthenticationManager;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.firebase.auth.FirebaseUser;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity implements Custom_Dialog2.ExampleDialogListener2 , Custom_Dialog1.ExampleDialogListener1{

    CircleImageView profile;
    FirebaseAuthenticationManager mFirebaseAuthenticationManager;
    Uri uri;

    private @NonNull ActivityProfileBinding _ActivityProfileBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        _ActivityProfileBinding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(_ActivityProfileBinding.getRoot());

        String userName = mFirebaseAuthenticationManager.getInstance().getCurrentUser().getDisplayName();
        //Log.d("user name------------", mFirebaseAuthenticationManager.getInstance().getCurrentUser().getDisplayName());

        getSupportActionBar().setTitle("Profile");
        _ActivityProfileBinding.userName.setText(userName);

        _ActivityProfileBinding.edetImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImagePicker.Companion.with(ProfileActivity.this)
                        .crop()//Crop image(Optional), Check Customization for more option
                        .compress(1024)			//Final image size will be less than 1 MB(Optional)
                        .maxResultSize(1080, 1080)	//Final image resolution will be less than 1080 x 1080(Optional)
                        .start();

            }
        });

        _ActivityProfileBinding.edetName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Custom_Dialog1 custom_dialog1=new Custom_Dialog1();
                custom_dialog1.show(getSupportFragmentManager(),"test");
            }
        });
        _ActivityProfileBinding.edetAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Custom_Dialog2 custom_dialog2=new Custom_Dialog2();
                custom_dialog2.show(getSupportFragmentManager(),"test");
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            uri=data.getData();
            Log.d("uri-------------------", uri.toString());
            profile.setImageURI(uri);

            //Intent intent=new Intent(ProfileActivity.this,GroupListActivity.class);
            //intent.putExtra("profile",uri.toString());

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void ApplyTexts2(String s) {
        if(s.isEmpty()) {
            Toast.makeText(ProfileActivity.this, "User info cann't be empty", Toast.LENGTH_LONG).show();
        }else{
            _ActivityProfileBinding.aboutText.setText(s);
        }


    }

    @Override
    public void ApplyTexts1(String s) {
        if(s.isEmpty()) {
            Toast.makeText(ProfileActivity.this, "User name cann't be empty", Toast.LENGTH_LONG).show();
        }else{
            _ActivityProfileBinding.userName.setText(s);
            mFirebaseAuthenticationManager.getInstance().UpdateUserProfile(s,uri);
        }

    }
}


