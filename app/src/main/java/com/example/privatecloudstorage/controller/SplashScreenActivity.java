package com.example.privatecloudstorage.controller;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.privatecloudstorage.model.FirebaseAuthenticationManager;
import com.example.privatecloudstorage.R;

/**
 * It is used as an interface for the Application
 */
public class SplashScreenActivity extends AppCompatActivity {
    FirebaseAuthenticationManager mFirebaseAuthenticationManager;
    private static int Delay_Time=4000;
    Animation _TopAnim,_BottomAnim;
    ImageView _Img;
    TextView _App_name;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
         mFirebaseAuthenticationManager=FirebaseAuthenticationManager.getInstance();

        setContentView(R.layout.activity_splash_screen);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        _TopAnim= AnimationUtils.loadAnimation(this,R.anim.top_animation);
        _BottomAnim =AnimationUtils.loadAnimation(this,R.anim.bottom_animation);

        _Img = findViewById(R.id.shape2);
        _App_name = findViewById(R.id.appName);

        _Img.setAnimation(_TopAnim);
       _App_name.setAnimation(_BottomAnim);
<<<<<<< HEAD
=======


>>>>>>> 3cf31ad621841d00c810c9a9c646d8a050414dbd
        /**
         * take the user to main activity after delay
         */
        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
<<<<<<< HEAD

=======
>>>>>>> 3cf31ad621841d00c810c9a9c646d8a050414dbd
                Intent intent=new Intent(SplashScreenActivity.this,SignInActivity.class);
                startActivity(intent);
                finish();
            }
        },Delay_Time);
<<<<<<< HEAD
=======

>>>>>>> 3cf31ad621841d00c810c9a9c646d8a050414dbd
    }
}