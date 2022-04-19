package com.example.privatecloudstorage.controller;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.example.privatecloudstorage.R;

public class Custom_Dialog1 extends AppCompatDialogFragment {
    EditText userName;
    ExampleDialogListener1 listener;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder=new AlertDialog.Builder(getContext());
        LayoutInflater inflater=getActivity().getLayoutInflater();

        View view =inflater.inflate(R.layout.user_name_layout,null);

        builder.setView(view).setTitle(" ")
                .setNegativeButton("cansel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {


                    }
                })
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String username=userName.getText().toString();
                        listener.ApplyTexts1(username);
                    }
                });

        userName = view.findViewById(R.id.name_layout);
        return builder.create();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener=(ExampleDialogListener1) context;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface ExampleDialogListener1{
        void ApplyTexts1(String s);
    }
}
