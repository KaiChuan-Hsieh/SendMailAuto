package com.test.kai.sendmailauto;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    final private String TAG = "SendMailAuto";
    private TextView addItem;
    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        verifyStoragePermissions(MainActivity.this);
        addItem = (TextView) findViewById(R.id.additem);
        addItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "show dialog");
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                LayoutInflater inflater = getLayoutInflater();

                builder.setTitle(R.string.dialog_title);
                builder.setCancelable(false);
                builder.setView(inflater.inflate(R.layout.dialog_senddraft, null));
                builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Log.i(TAG, "save selected");
                        EditText mUsername = (EditText)((AlertDialog)dialog).findViewById(R.id.username);
                        EditText mPassword = (EditText)((AlertDialog)dialog).findViewById(R.id.password);
                        EditText mRecipient = (EditText)((AlertDialog)dialog).findViewById(R.id.recipient);
                        EditText mSubject = (EditText)((AlertDialog)dialog).findViewById(R.id.subject);
                        EditText mMessage = (EditText)((AlertDialog)dialog).findViewById(R.id.message);
                        DatePicker datePicker = (DatePicker)((AlertDialog)dialog).findViewById(R.id.datepicker);
                        TimePicker timePicker = (TimePicker)((AlertDialog)dialog).findViewById(R.id.timepicker);
                        String username = String.valueOf(mUsername.getText());
                        String password = String.valueOf(mPassword.getText());
                        String recipient = String.valueOf(mRecipient.getText());
                        String subject = String.valueOf(mSubject.getText());
                        String message = String.valueOf(mMessage.getText());
                        int year = datePicker.getYear();
                        int month = datePicker.getMonth();
                        int day = datePicker.getDayOfMonth();
                        int hour = timePicker.getHour();
                        int minute = timePicker.getMinute();
                        Log.i(TAG, "username=" + username + "\n" +
                                "password=" + password + "\n" +
                                "recipient" + recipient + "\n" +
                                "subject" + subject + "\n" +
                                "message" + message + "\n" +
                                year + "-" + month + "-" + day + "\n" +
                                hour + ":" + minute);
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Log.i(TAG, "cancel selected");
                        dialog.cancel();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }
}
