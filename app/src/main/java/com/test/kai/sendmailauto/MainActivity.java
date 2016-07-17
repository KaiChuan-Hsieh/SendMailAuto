package com.test.kai.sendmailauto;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity {
    final private String TAG = "SendMailAuto";
    private EditText mUsername;
    private EditText mPassword;
    private EditText mRecipient;
    private EditText mSubject;
    private EditText mContent;
    private ToggleButton startBtn;
    private GmailSender gmailSender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUsername = (EditText) findViewById(R.id.username);
        mPassword = (EditText) findViewById(R.id.password);
        mRecipient = (EditText) findViewById(R.id.recipient);
        mSubject = (EditText) findViewById(R.id.subject);
        mContent = (EditText) findViewById(R.id.content);
        startBtn = (ToggleButton) findViewById(R.id.startButton);

        startBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    String username = String.valueOf(mUsername.getText());
                    String password = String.valueOf(mPassword.getText());
                    String recipient = String.valueOf(mRecipient.getText());
                    String subject = String.valueOf(mSubject.getText());
                    String content = String.valueOf(mContent.getText());
                    gmailSender = new GmailSender(username, password, recipient, subject, content);
                    Thread thread = new Thread(sendMailTask);
                    thread.start();
                }
            }
        });
    }

    Runnable sendMailTask = new Runnable() {
        @Override
        public void run() {
            Log.i(TAG, "start tsk");
            gmailSender.sendMailViaTLS();
            gmailSender.sendMailViaSSL();
            Log.i(TAG, "done tsk");
        }
    };
}
