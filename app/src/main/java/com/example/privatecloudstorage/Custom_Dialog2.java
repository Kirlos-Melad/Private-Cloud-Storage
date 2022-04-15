package com.example.privatecloudstorage;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

public class Custom_Dialog2 extends AppCompatDialogFragment {
    EditText about_layout;
    ExampleDialogListener2 listener;
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder=new AlertDialog.Builder(getContext());
        LayoutInflater inflater=getActivity().getLayoutInflater();

        View view =inflater.inflate(R.layout.user_about_layout,null);

        builder.setView(view).setTitle(" ")
                .setNegativeButton("cansel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String userabout=about_layout.getText().toString();
                        listener.ApplyTexts2(userabout);
                    }
                });

        about_layout=view.findViewById(R.id.about_layout);
        return builder.create();
    }
    public interface ExampleDialogListener2{
        void ApplyTexts2(String s);
    }
}
