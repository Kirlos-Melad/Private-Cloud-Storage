package com.example.privatecloudstorage;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //set a Listener to the button to call Group1 activity
        //this button should represent the groups list activity (view)
        Button _GroupBtn = (Button) findViewById(R.id.group1_btn);
        _GroupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), Group1.class);
                startActivity(intent);
            }
        });
        
    }
}