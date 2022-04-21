package com.example.privatecloudstorage.controller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.privatecloudstorage.databinding.ActivityProfileBinding;
import com.example.privatecloudstorage.model.FirebaseAuthenticationManager;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.firebase.auth.FirebaseUser;

import de.hdodenhof.circleimageview.CircleImageView;



public class ProfileActivity extends AppCompatActivity implements Custom_Dialog2.ExampleDialogListener2 , Custom_Dialog1.ExampleDialogListener1{

    FirebaseAuthenticationManager mFirebaseAuthenticationManager;
    Uri uri;

    private @NonNull ActivityProfileBinding _ActivityProfileBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        _ActivityProfileBinding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(_ActivityProfileBinding.getRoot());
        getSupportActionBar().setTitle("Profile");

        String userName = mFirebaseAuthenticationManager.getInstance().getCurrentUser().getDisplayName();
        Uri ImageUri = mFirebaseAuthenticationManager.getInstance().getCurrentUser().getPhotoUrl();


        _ActivityProfileBinding.userName.setText(userName);
        _ActivityProfileBinding.profileImage.setImageURI(ImageUri);


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
        if(resultCode == RESULT_OK && null != data){
            uri = data.getData();
            _ActivityProfileBinding.profileImage.setImageURI(uri);
            mFirebaseAuthenticationManager.getInstance().UpdateUserProfileImage(uri);

        }
    }

    @Override
    public void ApplyTexts2(String s) {
        if(s.isEmpty()) {
            Toast.makeText(ProfileActivity.this, "User info can't be empty", Toast.LENGTH_LONG).show();
        }else{
            _ActivityProfileBinding.aboutText.setText(s);
        }
    }

    @Override
    public void ApplyTexts1(String s) {
        if(s.isEmpty()) {
            Toast.makeText(ProfileActivity.this, "User name can't be empty", Toast.LENGTH_LONG).show();
        }else{
            _ActivityProfileBinding.userName.setText(s);
            mFirebaseAuthenticationManager.getInstance().UpdateUserProfileName(s);
        }

    }
}


