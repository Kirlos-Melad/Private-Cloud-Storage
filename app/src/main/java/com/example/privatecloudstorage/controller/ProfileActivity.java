package com.example.privatecloudstorage.controller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import android.app.AlertDialog;

import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.text.TextUtils;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;


import com.example.privatecloudstorage.R;
import com.example.privatecloudstorage.databinding.ActivityProfileBinding;
import com.example.privatecloudstorage.model.FirebaseAuthenticationManager;
import com.example.privatecloudstorage.model.ManagersMediator;
import com.github.dhaval2404.imagepicker.ImagePicker;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * show user profile allow him editing user name and user about
 */
public class ProfileActivity extends AppCompatActivity {

    FirebaseAuthenticationManager mFirebaseAuthenticationManager;
    Uri uri;
    ImageView imgv;
    private String mName;
    private String mDescription;
    private String mUri;
    private  Bundle bundle;
    private String mCaller;

    private @NonNull ActivityProfileBinding _ActivityProfileBinding;
    /**
     * handle user profile activity
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LayoutInflater inflater = getLayoutInflater();

        _ActivityProfileBinding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(_ActivityProfileBinding.getRoot());
        getSupportActionBar().setTitle("Profile");

        bundle = getIntent().getExtras();
        if(bundle == null)
            finish();
        mName = bundle.getString("Name");
        mDescription = bundle.getString("Description");
        mUri = bundle.getString("Uri");
        mCaller = bundle.getString("Caller");

        if(mUri.equals("NoPicture")){
            imgv=(ImageView)findViewById(R.id.profile_image);
            Drawable myDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.person_24, null);
            imgv.setImageDrawable(myDrawable);
            /*_ActivityProfileBinding.profileImage.setImageURI( mFirebaseAuthenticationManager.getInstance()
                    .getCurrentUser().getPhotoUrl());*/
        }else{
            _ActivityProfileBinding.profileImage.setImageURI(Uri.parse(mUri));
        }
        _ActivityProfileBinding.aboutText.setText(mDescription);
        _ActivityProfileBinding.userName.setText(mName);

        View _UserNameView = inflater.inflate(R.layout.user_custom_dialog,null);
        View _UserAboutView= inflater.inflate(R.layout.user_custom_dialog,null);
        EditText _UserNameEditText =_UserNameView.findViewById(R.id.edit_text);
        EditText _UserAboutEditText = _UserAboutView.findViewById(R.id.edit_text);


        /**
         * handle change user profile image
         */
        _ActivityProfileBinding.editImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImagePicker.Companion.with(ProfileActivity.this)
                        .crop()//Crop image(Optional), Check Customization for more option
                        .compress(1024)			//Final image size will be less than 1 MB(Optional)
                        .maxResultSize(1080, 1080)	//Final image resolution will be less than 1080 x 1080(Optional)
                        .start();
            }
        });
        /**
         * handle change user profile name
         */

            @RequiresApi(api = Build.VERSION_CODES.Q)

        _ActivityProfileBinding.editName.setOnClickListener(EditUserButtonClickListener("User Name",_UserNameView,new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String editUserName = _UserNameEditText.getText().toString();
                if(TextUtils.isEmpty(editUserName)){
                    _UserNameEditText.setError("User Name is required");
                    return;
                }
                _ActivityProfileBinding.userName.setText(editUserName);
                if(mCaller.equals("User")){
                    ManagersMediator.getInstance().SetUserName(editUserName);
                }

            }
        }));
        /**
         * handle change user profile about
         */

            @RequiresApi(api = Build.VERSION_CODES.Q)

        _ActivityProfileBinding.editAbout.setOnClickListener(EditUserButtonClickListener("User About",_UserAboutView,new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                String editUserAbout = _UserAboutEditText.getText().toString();
                if(TextUtils.isEmpty(editUserAbout)){
                    _UserAboutEditText.setError("User Name is required");
                    return;
                }
                _ActivityProfileBinding.aboutText.setText(editUserAbout);
                if(mCaller.equals("User")){
                    ManagersMediator.getInstance().SetUserAbout(editUserAbout);
                }

            }
        }));
    }
    /**
     * set image with new one using uri
     * @param requestCode
     * @param resultCode
     * @param data image data
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && null != data){
            uri = data.getData();
            _ActivityProfileBinding.profileImage.setImageURI(uri);
            if(mCaller.equals("User")){
                ManagersMediator.getInstance().SetUserProfilePicture(uri);
            }


        }
    }

    /**
     * handle user name and about
     * @param userSelection a string put into edit text and text view
     * @param userView custom dialog layout
     * @param dialogPositiveButton OnClickListener runs when user click ok
     * @return
     */
    private View.OnClickListener EditUserButtonClickListener(String userSelection,View userView ,DialogInterface.OnClickListener dialogPositiveButton ){
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder dialogBuilder=new AlertDialog.Builder(ProfileActivity.this);
                TextView _SelectionTextView = userView.findViewById(R.id.text_view);
                EditText _SelectionEditText=userView.findViewById(R.id.edit_text);
                _SelectionEditText.setHint(userSelection);
                _SelectionTextView.setText(userSelection);
                if(userView.getParent()!=null)
                    ( (ViewGroup)userView.getParent()).removeView(userView);
                dialogBuilder.setTitle(" ")
                        .setView(userView)
                        .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        })
                        .setPositiveButton("ok",dialogPositiveButton);
                dialogBuilder.create().show();

            }
        };
    }

}
