package com.example.privatecloudstorage.controller;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.privatecloudstorage.BuildConfig;
import com.example.privatecloudstorage.R;
import com.example.privatecloudstorage.databinding.ActivityGroupSliderBinding;
import com.example.privatecloudstorage.interfaces.IAction;
import com.example.privatecloudstorage.model.FileManager;
import com.example.privatecloudstorage.model.RecyclerViewItem;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Stack;

//replace fragment activity with AppCompatActivity

/**
 * mange tab layout
 */
public class GroupSliderActivity extends AppCompatActivity {
    public static final byte MEMBERS=0x01;
    public static final byte NORMAL_FILES=0x02;
    public static final byte STRIPPED_FILES=0x03;

    ArrayList<String> titels=new ArrayList<>();
    private ActivityGroupSliderBinding _ActivityGroupSliderBinding;
    //getSupportActionBar().setTitle(mSelectedGroupName);

    /**
     * The number of pages (wizard steps) to show in this demo.
     */
    private static final int NUM_PAGES = 3;

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private ViewPager2 viewPager;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private FragmentStateAdapter pagerAdapter;
    private String mSelectedGroupName;
    private String mSelectedGroupKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _ActivityGroupSliderBinding = ActivityGroupSliderBinding.inflate(getLayoutInflater());
        setContentView(_ActivityGroupSliderBinding.getRoot());
        TabLayout tabLayout;

        _ActivityGroupSliderBinding.menu.setVisibility(View.VISIBLE);

        Bundle bundle = getIntent().getExtras();
        if(bundle == null)
            finish();
        mSelectedGroupName = bundle.getString("selectedGroupName");
        mSelectedGroupKey = bundle.getString("selectedGroupKey");
        getSupportActionBar().setTitle(mSelectedGroupName);

        viewPager = findViewById(R.id.pager);
        pagerAdapter = new ScreenSlidePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        tabLayout= findViewById(R.id.tab_Layout);
        titels.add("MEMBERS");
        titels.add("NORMAL FILES");
        titels.add("STRIPPED FILES");

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    tab.setText(titels.get(position));
                }
        ).attach();

        _ActivityGroupSliderBinding.fabShareFile.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void onClick(View view) {
                _ActivityGroupSliderBinding.menu.setVisibility(View.INVISIBLE);
                if(checkPermission()){
                    _ActivityGroupSliderBinding.menu.close(true);
                    Intent intent = new Intent(GroupSliderActivity.this, FileExplorerActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("selectedGroupName", mSelectedGroupName);
                    bundle.putString("selectedGroupKey", mSelectedGroupKey);
                    intent.putExtras(bundle);//Put Group number to your next Intent
                    startActivity(intent);
                }
                else requestPermission();
            }
        });

        _ActivityGroupSliderBinding.fabShowQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkPermission()){
                    String path = getFilesDir()+ File.separator + mSelectedGroupKey + " " + mSelectedGroupName
                            + File.separator + mSelectedGroupName +" QR Code.png";
                    ShowQrCode(path);
                    _ActivityGroupSliderBinding.menu.close(true);
                }
                else requestPermission();
            }
        });

        _ActivityGroupSliderBinding.fabCreateTextFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                _ActivityGroupSliderBinding.menu.close(true);
                CreateTxtDialog("Enter File Name :");
            }
        });

    }

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else {
            // Otherwise, select the previous step.
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.setting_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item1:
                Toast.makeText(this, "Item 1 selected", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.item2:
                Toast.makeText(this, "Item 2 selected", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.item3:
                Toast.makeText(this, "Item 3 selected", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * A simple pager adapter that represents 3 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private class ScreenSlidePagerAdapter extends FragmentStateAdapter {
        public ScreenSlidePagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @Override
        public Fragment createFragment(int position) {
            Bundle bundle = new Bundle();
            bundle.putString("selectedGroupName", mSelectedGroupName);
            bundle.putString("selectedGroupKey", mSelectedGroupKey);

            GroupFragment groupFragment=new GroupFragment();
            switch (position){
                case 0:
                    bundle.putByte("tab", MEMBERS);
                    break;
                case 1:
                    bundle.putByte("tab", NORMAL_FILES);
                    break;
                case 2:
                    bundle.putByte("tab", STRIPPED_FILES);
                    break;
                default:
                    throw new ArrayIndexOutOfBoundsException(String.format("fragment position %s out of bounds", position));

            }
            groupFragment.setArguments(bundle);

            return groupFragment;
        }

        @Override
        public int getItemCount() {
            return NUM_PAGES;
        }


    }

    private void CreateTxtDialog(String msg){
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(msg);
        final EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        dialog.setView(editText);
        dialog.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String name = editText.getText().toString();
                if(TextUtils.isEmpty(name)){
                    Toast.makeText(GroupSliderActivity.this,"Name Field cannot be empty",Toast.LENGTH_SHORT).show();
                    return;
                }
                File txtFile = new File(getFilesDir()+ File.separator + mSelectedGroupKey + " " + mSelectedGroupName +
                        File.separator + "Normal Files" ,name + ".txt");

                if(txtFile.exists()){
                    ReplaceMsgDialog("Do you want to replace the text file ?",txtFile);
                }
                else
                    try {
                        FileManager.getInstance().CreateFile(txtFile,FileManager.NORMAL);
                        recreate();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        });
        dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        dialog.show();
    }

    private void ReplaceMsgDialog(String msg , File file){
        AlertDialog.Builder replaceDialog = new AlertDialog.Builder(this);
        replaceDialog.setTitle(msg);
        replaceDialog.setPositiveButton("Replace", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                try {
                    FileManager.getInstance().CreateFile(file,FileManager.NORMAL);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        replaceDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        replaceDialog.show();
    }

    /** Check if permission is granted or not
     * true : if granted
     * false : if not
     * @return true/false
     */
    private boolean checkPermission(){
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(result == PackageManager.PERMISSION_GRANTED)
            return true;
        else
            return false;
    }

    /**
     * request permission if it's not granted
     */
    private void requestPermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE))
            Toast.makeText(this,"Storage Perimission is required, please allow from settings",Toast.LENGTH_SHORT);
        else
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
    }

    private void ShowQrCode(String filePath) {
        File file = new File(filePath);
        Uri uri =  FileProvider.getUriForFile(Objects.requireNonNull(this), BuildConfig.APPLICATION_ID + ".provider",file);
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "image/png");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
