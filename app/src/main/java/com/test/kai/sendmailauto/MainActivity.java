package com.test.kai.sendmailauto;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.PowerManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "SendMailAuto";
    private final String ACTION_PROCESS_TASK = "action.process.task";
    private TaskManager mTaskManager;
    private ListView mListTasks;
    private PowerManager pm;
    private PowerManager.WakeLock wakeLock;
    private IntentFilter alarmFilter;
    private IntentFilter networkChangeFilter;
    private boolean isConnected;
    private boolean isReceiverRegistered = false;

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
                handleTasks();
            }
        }
    };

    private final BroadcastReceiver NetworkStateChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "receive connectivity change");
            if (isNetworkConnected()) {
                Log.i(TAG, "network is connected");
                handleTasks();
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
        alarmFilter = new IntentFilter(ACTION_PROCESS_TASK);
        networkChangeFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        if (!isReceiverRegistered) {
            registerReceiver(mAlarmReceiver, alarmFilter);
            registerReceiver(NetworkStateChangeReceiver, networkChangeFilter);
            isReceiverRegistered = true;
        }
        handleTasks();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG,"onStart()............");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG,"onStop()............");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause()............");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()............");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isReceiverRegistered) {
            unregisterReceiver(mAlarmReceiver);
            unregisterReceiver(NetworkStateChangeReceiver);
            isReceiverRegistered = false;
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
                showDialog(null);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public interface DialogDismissedListener {
        public void handleDialogDismissed();
    }

    DialogDismissedListener dialogDismissedListener = new DialogDismissedListener() {
        @Override
        public void handleDialogDismissed() {
            Log.i(TAG, "dialog dismissed");
            handleTasks();
        }
    };

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

    public void handleTasks() {
        synchronized (this) {
            if (!wakeLock.isHeld())
                wakeLock.acquire();
            Log.i(TAG, "wakelock acquired");
            if (isNetworkConnected()) {
                mTaskManager.flushAllTimePassedTask();
                Calendar nextTime = mTaskManager.getNextTaskTime();
                setAlarm(nextTime);
            }
            updateTaskList();
            mTaskManager.saveTaskConfigurations();
            if (wakeLock.isHeld())
                wakeLock.release();
            Log.i(TAG, "wakelock released");
        }
    }

    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        isConnected = (activeNetwork != null && activeNetwork.isConnectedOrConnecting());
        return isConnected;
    }

    public void updateTaskList() {
        final ArrayList<TaskConfiguration> taskConfigurations = mTaskManager.getTaskConfigurations();
        ArrayAdapter<TaskConfiguration> arrayAdapter = new ArrayAdapter<TaskConfiguration>(MainActivity.this,
                R.layout.list_item, taskConfigurations);
        mListTasks.setAdapter(arrayAdapter);
        mListTasks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                TaskConfiguration task = taskConfigurations.get(i);
                showDialog(task);
            }
        });
    }

    public void setAlarm(Calendar cal) {
        Calendar current = Calendar.getInstance();
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent();
        intent.setAction(ACTION_PROCESS_TASK);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0,
                intent, PendingIntent.FLAG_CANCEL_CURRENT);
        if (cal.compareTo(current)<=0) {
            Log.i(TAG, "time passed");
            ArrayList<TaskConfiguration> taskConfigurations = mTaskManager.getTaskConfigurations();
            if (!taskConfigurations.isEmpty())
                handleTasks();
            else
                alarmManager.cancel(pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
        }
    }

    public void showDialog(TaskConfiguration taskConfiguration) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        TaskConifgDialogFragment taskConifgDialogFragment = new TaskConifgDialogFragment();
        taskConifgDialogFragment.setData(mTaskManager, taskConfiguration);
        taskConifgDialogFragment.setDismissListener(dialogDismissedListener);
        taskConifgDialogFragment.show(fragmentManager, "TaskConfigDialogFragment");
    }
}
