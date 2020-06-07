package com.distributedsystems.listenbee;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

public class IPDialog  extends AppCompatDialogFragment {
    private static String serverIP = "192.168.1.4";

    private String defaultIP = "192.168.1.4";
    private EditText ip_text;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.ip_fragment, null);

        builder.setView(view).setTitle("Change your main server IP")
                .setNegativeButton("Default", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        serverIP = defaultIP;
                    }
                }).setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(!TextUtils.isEmpty(ip_text.getText())){
                    serverIP = ip_text.getText().toString();
                }
            }
        });

        ip_text = view.findViewById(R.id.ip_text);
        ip_text.setText(serverIP());

        return builder.create();
    }

    public static String serverIP(){
        return serverIP;
    }
}
