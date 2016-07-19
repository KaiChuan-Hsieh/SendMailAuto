package com.test.kai.sendmailauto;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.PowerManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TimePicker;

import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    final private String TAG = "SendMailAuto";
    final private String ACTION_PROCESS_TASK = "action.process.task";
    private TaskManager mTaskManager;
    private ListView mListTasks;
    private PowerManager pm;
    private PowerManager.WakeLock wakeLock;
    private IntentFilter intentFilter;

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private final BroadcastReceiver mAlarmReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_PROCESS_TASK.equals(action)) {
                Log.i(TAG, "recieve " + action);
                handleTask();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        verifyStoragePermissions(MainActivity.this);
        mTaskManager = new TaskManager(getApplicationContext());
        mListTasks = (ListView)findViewById(R.id.tasklist);
        intentFilter = new IntentFilter(ACTION_PROCESS_TASK);
        pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        registerReceiver(mAlarmReceiver, intentFilter);
        handleTask();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_add:
                showTaskConfigureDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void showTaskConfigureDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        builder.setTitle(R.string.dialog_title);
        builder.setCancelable(false);
        View mView = inflater.inflate(R.layout.dialog_senddraft, null);
        final EditText mUsername = (EditText)mView.findViewById(R.id.username);
        final EditText mPassword = (EditText)mView.findViewById(R.id.password);
        final EditText mRecipient = (EditText)mView.findViewById(R.id.recipient);
        final EditText mSubject = (EditText)mView.findViewById(R.id.subject);
        final EditText mMessage = (EditText)mView.findViewById(R.id.message);
        final DatePicker datePicker = (DatePicker)mView.findViewById(R.id.datepicker);
        final TimePicker timePicker = (TimePicker)mView.findViewById(R.id.timepicker);
        builder.setView(mView);
        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Log.i(TAG, "save selected");
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
                mTaskManager.saveTaskConfigurations();
                handleTask();
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

    public void updateTaskList() {
        final ArrayList<TaskConfiguration> taskConfigurations = mTaskManager.getTaskConfigurations();
        ArrayAdapter<TaskConfiguration> arrayAdapter = new ArrayAdapter<TaskConfiguration>(MainActivity.this,
                R.layout.list_item, taskConfigurations);
        mListTasks.setAdapter(arrayAdapter);
        mListTasks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                showTaskDialog(i);
            }
        });
    }

    public void showTaskDialog(int taskId) {
        final int id = taskId;
        ArrayList<TaskConfiguration> taskConfigurations = mTaskManager.getTaskConfigurations();
        TaskConfiguration task = taskConfigurations.get(id);
        Log.i(TAG, task.toString());
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        builder.setTitle(R.string.dialog_title);
        builder.setCancelable(false);
        View mView = inflater.inflate(R.layout.dialog_senddraft, null);
        EditText mUsername = (EditText)mView.findViewById(R.id.username);
        EditText mPassword = (EditText)mView.findViewById(R.id.password);
        EditText mRecipient = (EditText)mView.findViewById(R.id.recipient);
        EditText mSubject = (EditText)mView.findViewById(R.id.subject);
        EditText mMessage = (EditText)mView.findViewById(R.id.message);
        DatePicker datePicker = (DatePicker)mView.findViewById(R.id.datepicker);
        TimePicker timePicker = (TimePicker)mView.findViewById(R.id.timepicker);
        mUsername.setText(task.getUsername());
        mPassword.setText(task.getPassword());
        mRecipient.setText(task.getRecipient());
        mSubject.setText(task.getSubject());
        mMessage.setText(task.getMessage());
        datePicker.updateDate(task.getYear(), task.getMonth(), task.getDay());
        timePicker.setHour(task.getHour());
        timePicker.setMinute(task.getMinute());
        builder.setView(mView);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Log.i(TAG, "delete selected");
                mTaskManager.removeTaskConfiguration(id);
                mTaskManager.saveTaskConfigurations();
                handleTask();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Log.i(TAG, "cancel selected");
                dialogInterface.cancel();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void setAlarm(Calendar cal) {
        Calendar current = Calendar.getInstance();
        if (cal.compareTo(current)<=0) {
            Log.i(TAG, "time passed");
            ArrayList<TaskConfiguration> taskConfigurations = mTaskManager.getTaskConfigurations();
            if (!taskConfigurations.isEmpty())
                handleTask();
        } else {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            Intent intent = new Intent();
            intent.setAction(ACTION_PROCESS_TASK);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0,
                    intent, PendingIntent.FLAG_CANCEL_CURRENT);
            alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
        }
    }

    public void handleTask() {
        synchronized (this) {
            if (!wakeLock.isHeld())
                wakeLock.acquire();
            Log.i(TAG, "wakelock acquired");
            mTaskManager.flushAllTimePassedTask();
            mTaskManager.saveTaskConfigurations();
            Calendar nextTime = mTaskManager.getNextTaskTime();
            setAlarm(nextTime);
            updateTaskList();
            Log.i(TAG, "wakelock released");
            if (wakeLock.isHeld())
                wakeLock.release();
        }
    }
}
