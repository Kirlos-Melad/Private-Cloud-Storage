package com.example.privatecloudstorage.controller;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.example.privatecloudstorage.BuildConfig;
import com.example.privatecloudstorage.databinding.ActivityFileExplorerBinding;
import com.example.privatecloudstorage.interfaces.IAction;
import com.example.privatecloudstorage.model.FileManager;
import com.example.privatecloudstorage.model.FirebaseDatabaseManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.Objects;

public class FileExplorerActivity extends AppCompatActivity {

    public static View.OnClickListener FileOnClickListener(Context context, File file){

        return new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View view) {
                try {
                    //open the selected file
                    Uri uri =  FileProvider.getUriForFile(Objects.requireNonNull(context), BuildConfig.APPLICATION_ID + ".provider",file);
                    String mime = context.getContentResolver().getType(uri);
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, mime);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }catch (Exception e){
                    e.printStackTrace();
                    Toast.makeText(context.getApplicationContext(),"Cannot open the file",Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    public static View.OnClickListener FolderOnClickListener(IAction action){
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                action.onSuccess(null);
            }
        };
    }

    public static View.OnLongClickListener ApplicationFileOnLongClickListener(Activity activity, File file){
        Log.d("=======================", activity.getLocalClassName().toString());
        return new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                PopupMenu popupMenu = new PopupMenu(activity,v);
                    popupMenu.getMenu().add("Rename");
                    popupMenu.getMenu().add("Delete");
                    if(file.getName().contains(".txt"))
                        popupMenu.getMenu().add("Edit");

                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if(item.getTitle().equals("Rename")){
                            AlertDialog.Builder renameDialog = new AlertDialog.Builder(activity);
                            renameDialog.setTitle("Rename File :");
                            final EditText fileNameEditText = new EditText(activity);
                            fileNameEditText.setInputType(InputType.TYPE_CLASS_TEXT);
                            fileNameEditText.setText(file.toString().
                                    substring((file.getPath().lastIndexOf(File.separator))+1,file.toString().length()));
                            fileNameEditText.setSelectAllOnFocus(true);
                            renameDialog.setView(fileNameEditText);
                            renameDialog.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    String newName = fileNameEditText.getText().toString();
                                    FileManager.getInstance().RenameFile(file ,newName);
                                    activity.recreate();
                                }
                            });
                            renameDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.cancel();
                                }
                            });
                            renameDialog.show();
                        }
                        if(item.getTitle().equals("Delete")){
                            FileManager.getInstance().DeleteFile(file);
                            activity.recreate();
                        }
                        if(item.getTitle().equals("Edit")){
                            //TODO : sync with firebase ---------------------------------------
                            AlertDialog.Builder editDialog = new AlertDialog.Builder(activity);
                            final EditText editText = new EditText(activity);
                            editText.setInputType(InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE);
                            editText.setSingleLine(false);
                            try {
                                byte[] b = Files.readAllBytes(file.toPath());
                                String content = new String(b);
                                editText.setText(content);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            editDialog.setView(editText);
                            editDialog.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    try {
                                        byte[] bytes = editText.getText().toString().getBytes();
                                        Files.write(file.toPath(),bytes);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                            editDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.cancel();
                                }
                            });
                            editDialog.show();
                        }
                        return true;
                    }
                });
                popupMenu.show();
                return true;
            }
        };
    }

    private static void SelectMode(Activity activity, IAction action) {
        final byte[] mode = {FileManager.NORMAL};
        AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
        dialog.setTitle("Select Mode");
        String[] items = {"Normal Mode","Striping Mode"};
        int checkedItem=0;

        dialog.setSingleChoiceItems(items, checkedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        mode[0] = FileManager.NORMAL;
                        Toast.makeText(activity, "Clicked on Normal", Toast.LENGTH_LONG).show();
                        break;
                    case 1:
                        mode[0] = FileManager.STRIP;
                        Toast.makeText(activity, "Clicked on Striping", Toast.LENGTH_LONG).show();
                        break;
                }
            }
        });
        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                action.onSuccess(mode[0]);
            }
        });
        dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                activity.recreate();
            }
        });
        dialog.show();
    }

    public static View.OnLongClickListener UserFileOnLongClickListener(Context context, File file){

        return new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                PopupMenu popupMenu = new PopupMenu(context,view);
                popupMenu.getMenu().add("Share");
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        //the destination that the file will be moved to
                        File normalDst = new File(context.getFilesDir() + File.separator +
                                "Normal Files" + File.separator + Uri.fromFile(file).getLastPathSegment());

                        File stripDst = new File(context.getFilesDir() + File.separator +
                                "Stripped Files" + File.separator + Uri.fromFile(file).getLastPathSegment());

                        SelectMode((Activity) context,new IAction()  {
                            //copy the file from original directory to group directory
                            @RequiresApi(api = Build.VERSION_CODES.Q)
                            @Override
                            public void onSuccess(Object object) {
                                FirebaseDatabaseManager.getInstance().setMode((byte) object);
                                byte mode = (byte) object;
                                if (mode == FileManager.NORMAL) {
                                    try {
                                        FileManager.getInstance().CopyFile(file.toPath(), normalDst.toPath(), mode);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        Toast.makeText(context.getApplicationContext(), "The Group is not exist", Toast.LENGTH_LONG).show();
                                    }
                                }
                                if (mode == FileManager.STRIP) {
                                    try {
                                        FileManager.getInstance().CopyFile(file.toPath(), stripDst.toPath(), mode);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        Toast.makeText(context.getApplicationContext(), "The Group is not exist", Toast.LENGTH_LONG).show();
                                    }
                                }
                            }
                        });
                        return true;
                    }

            });
                popupMenu.show();
                return true;
        };
    };
    }

}
