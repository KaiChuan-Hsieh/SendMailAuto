package com.test.kai.sendmailauto;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;

/**
 * Created by kirt-server on 2016/7/20.
 */
public class TaskConifgDialogFragment extends DialogFragment {
    private final String TAG = "TaskConfigDialog";
    private EditText mUsername;
    private EditText mPassword;
    private EditText mRecipient;
    private EditText mSubject;
    private EditText mMessage;
    private DatePicker datePicker;
    private TimePicker timePicker;
    private TaskManager mTaskManager;
    private TaskConfiguration mTaskConfiguration;
    MainActivity.DialogDismissedListener mDismissListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_senddraft, null);
        builder.setView(dialogView);
        builder.setTitle("Configuration");
        mUsername = (EditText)dialogView.findViewById(R.id.username);
        mPassword = (EditText)dialogView.findViewById(R.id.password);
        mRecipient = (EditText)dialogView.findViewById(R.id.recipient);
        mSubject = (EditText)dialogView.findViewById(R.id.subject);
        mMessage = (EditText)dialogView.findViewById(R.id.message);
        datePicker = (DatePicker)dialogView.findViewById(R.id.datepicker);
        timePicker = (TimePicker)dialogView.findViewById(R.id.timepicker);
        if (mTaskConfiguration!=null) {
            mUsername.setText(mTaskConfiguration.getUsername());
            mPassword.setText(mTaskConfiguration.getPassword());
            mRecipient.setText(mTaskConfiguration.getRecipient());
            mSubject.setText(mTaskConfiguration.getSubject());
            mMessage.setText(mTaskConfiguration.getMessage());
            datePicker.updateDate(mTaskConfiguration.getYear(), mTaskConfiguration.getMonth(), mTaskConfiguration.getDay());
            timePicker.setHour(mTaskConfiguration.getHour());
            timePicker.setMinute(mTaskConfiguration.getMinute());
            builder.setPositiveButton(R.string.save, saveClickListener);
            builder.setNeutralButton(R.string.delete, deleteClickListener);
            builder.setNegativeButton(R.string.cancel, cancelClickListener);
        } else {
            builder.setPositiveButton(R.string.save, saveClickListener);
            builder.setNegativeButton(R.string.cancel, cancelClickListener);
        }
        return builder.create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mDismissListener!=null) {
            mDismissListener.handleDialogDismissed();
        }
    }

    public void setData(TaskManager taskManager, TaskConfiguration taskConfiguration) {
        mTaskManager = taskManager;
        mTaskConfiguration = taskConfiguration;
    }

    DialogInterface.OnClickListener saveClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            Log.i(TAG,"add button selected");
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
            TaskConfiguration taskConfiguration =
                    mTaskManager.getTaskConfiguration(year, month, day, hour, minute,
                            username, password, recipient, subject, message);
            Log.i(TAG, taskConfiguration.toString());
            mTaskManager.addTaskConfiguration(taskConfiguration);
        }
    };

    DialogInterface.OnClickListener deleteClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            Log.i(TAG, "delete button selected");
            mTaskManager.removeTaskConfiguration(mTaskConfiguration);
        }
    };

    DialogInterface.OnClickListener cancelClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            Log.i(TAG, "cancel button selected");
            dialogInterface.cancel();
        }
    };

    public void setDismissListener(MainActivity.DialogDismissedListener dismissListener) {
        mDismissListener = dismissListener;
    }
}
