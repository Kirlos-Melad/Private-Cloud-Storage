package com.example.privatecloudstorage;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class GroupListActivity extends AppCompatActivity {
    ListView _ListView;
    ArrayList<String> mItems;
    ArrayAdapter<String> _Adabter;

    private FirebaseDatabaseManager mFirebaseDatabaseManager;
    private DatabaseReference databaseReference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_list);
        _ListView=findViewById(R.id.Listview);
        mItems =new ArrayList<>();
        mFirebaseDatabaseManager = FirebaseDatabaseManager.getInstance();

        _Adabter=new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, mItems);
        _ListView.setAdapter(_Adabter);

        databaseReference = mFirebaseDatabaseManager.GetUserGroups();
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot snapshot1 : snapshot.getChildren()){
                    mItems.add(snapshot1.getValue().toString());
                    _ListView.setAdapter(_Adabter);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        _ListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

                mFirebaseDatabaseManager.GetUserGroups().addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String selectedGroupName = null;
                        String selectedGroupKey = null;
                        int index = 0;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            if(index != position){
                                index++;
                                continue;
                            }

                            selectedGroupKey = child.getKey();
                            selectedGroupName = child.getValue().toString();
                            break;
                        }

                        if(index == position){
                            //before go to new activity send group name and id as a parameter
                            Intent intent = new Intent(GroupListActivity.this, GroupContentActivity.class);
                            Bundle bundle = new Bundle();
                            bundle.putString("selectedGroupName", selectedGroupName);
                            bundle.putString("selectedGroupKey", selectedGroupKey);

                            intent.putExtras(bundle); //Put Group number to your next Intent
                            startActivity(intent);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });

        // TODO: Display the QR Code
    }

}