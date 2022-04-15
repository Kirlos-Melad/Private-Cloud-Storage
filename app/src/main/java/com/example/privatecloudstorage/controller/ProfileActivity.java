package com.example.privatecloudstorage.controller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.privatecloudstorage.Custom_Dialog1;
import com.example.privatecloudstorage.Custom_Dialog2;
import com.example.privatecloudstorage.R;
import com.github.dhaval2404.imagepicker.ImagePicker;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity implements Custom_Dialog2.ExampleDialogListener2 {
    ImageView edet_name;
    ImageView edet_about;
    ImageView image_edet;
    CircleImageView profile;
    TextView user_name,about_text;

    /*public void openDialog(View view){
        Custom_Dialog1 custom_dialog1=new Custom_Dialog1();
        custom_dialog1.show(getSupportFragmentManager(),"test");
    }*/


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        getSupportActionBar().setTitle("Profile");

        edet_name=findViewById(R.id.edet_name);
        edet_about=findViewById(R.id.edet_about);
        image_edet=findViewById(R.id.edet_image);

        image_edet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImagePicker.Companion.with(ProfileActivity.this)
                        .crop()//Crop image(Optional), Check Customization for more option
                        .compress(1024)			//Final image size will be less than 1 MB(Optional)
                        .maxResultSize(1080, 1080)	//Final image resolution will be less than 1080 x 1080(Optional)
                        .start();
            }
        });

        edet_name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Custom_Dialog1 custom_dialog1=new Custom_Dialog1();
                custom_dialog1.show(getSupportFragmentManager(),"test");
            }
        });
        edet_about.setOnClickListener(new View.OnClickListener() {
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
            Uri uri=data.getData();
            profile.setImageURI(uri);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void ApplyTexts2(String s) {
        about_text.setText(s);
    }
}


