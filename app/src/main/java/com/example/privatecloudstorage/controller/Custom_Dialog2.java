package com.example.privatecloudstorage.controller;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.example.privatecloudstorage.R;

public class Custom_Dialog2 extends AppCompatDialogFragment {
    EditText about_layout;
    ExampleDialogListener2 listener;
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder=new AlertDialog.Builder(getActivity());
        LayoutInflater inflater=getActivity().getLayoutInflater();

        View view =inflater.inflate(R.layout.user_about_layout,null);
        about_layout=view.findViewById(R.id.about_layout);

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
        return builder.create();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener=(ExampleDialogListener2) context;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface ExampleDialogListener2{
        void ApplyTexts2(String s);
    }
}
