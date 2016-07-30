package com.test.kai.sendmailauto;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
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
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.GmailScopes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity
        implements EasyPermissions.PermissionCallbacks {

    private static final int REQUEST_EXTERNAL_STORAGE = 1000;
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 1001;
    private static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1002;
    private static final int REQUEST_ACCOUNT_PICKER = 1003;
    private static final int REQUEST_AUTHORIZATION = 1004;
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String PREF_FIRST_USED = "firstUsed";
    private static final String[] SCOPES = {
            GmailScopes.GMAIL_COMPOSE,
            GmailScopes.GMAIL_SEND
    };
    // Storage Permissions
    private static final String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private final String TAG = "SendMailAuto";
    private final String ACTION_PROCESS_TASK = "action.process.task";
    GoogleAccountCredential mCredential;
    private TextView mOutputText;
    private TaskManager mTaskManager;
    private ListView mListTasks;
    private PowerManager pm;
    private PowerManager.WakeLock wakeLock;
    DialogDismissedListener dialogDismissedListener = new DialogDismissedListener() {
        @Override
        public void handleDialogDismissed() {
            Log.i(TAG, "dialog dismissed");
            handleTasks();
        }
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
            if (isDeviceOnline()) {
                Log.i(TAG, "network is connected");
                handleTasks();
            }
        }
    };
    private PowerManager.WakeLock twakeLock;
    private IntentFilter alarmFilter;
    private IntentFilter networkChangeFilter;
    private boolean isReceiverRegistered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

        mOutputText = (TextView) findViewById(R.id.description);

        mTaskManager = new TaskManager(MainActivity.this);
        mListTasks = (ListView)findViewById(R.id.tasklist);

        alarmFilter = new IntentFilter(ACTION_PROCESS_TASK);
        networkChangeFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        if (!isReceiverRegistered) {
            registerReceiver(mAlarmReceiver, alarmFilter);
            registerReceiver(NetworkStateChangeReceiver, networkChangeFilter);
            isReceiverRegistered = true;
        }

        pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        twakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG+"-"+"thread");

        handleTasks();
        if (isFirstTimeOpen()) {
            Log.i(TAG, "The first time app start");
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            String subject = getResources().getString(R.string.first_subject);
            String message = getResources().getString(R.string.first_content);
            Calendar current = Calendar.getInstance();
            TaskConfiguration taskConfiguration = mTaskManager.getTaskConfiguration(
                    current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH),
                    current.get(Calendar.HOUR), current.get(Calendar.MINUTE), accountName,
                    subject, message);
            mTaskManager.addTaskConfiguration(taskConfiguration);
            handleTasks();
        }
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

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     *     activity result.
     * @param data Intent (containing result data) returned by incoming
     *     activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    mOutputText.setText(R.string.no_google_play_service);
                    mOutputText.setVisibility(View.VISIBLE);
                } else {
                    handleTasks();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                Log.i(TAG, "acivity result account picker");
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        handleTasks();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                Log.i(TAG, "activity result resuest authorization");
                if (resultCode == RESULT_OK) {
                    handleTasks();
                }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult..." + "," + "requestCode" + requestCode);
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "external storage granted");
                    handleTasks();
                } else {
                    Log.i(TAG, "external storage denied");
                    mOutputText.setText(R.string.no_external_storage_permission);
                    mOutputText.setVisibility(View.VISIBLE);
                }
                break;
            case REQUEST_PERMISSION_GET_ACCOUNTS:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "permission get account granted");
                    handleTasks();
                } else {
                    Log.i(TAG, "permission get account denied");
                    mOutputText.setText(R.string.no_get_account_permission);
                    mOutputText.setVisibility(View.VISIBLE);
                }
                break;
        }
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
        Log.i(TAG, "onPermissionsGranted...");
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
        Log.i(TAG, "onPermissionsDenied...");
    }

    private void handleTasks() {
        Log.i(TAG, "handleTasks in");
        if (!wakeLock.isHeld()) wakeLock.acquire();
        Log.i(TAG, "wakelock acquired");
        mOutputText.setVisibility(View.INVISIBLE);
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (!isDeviceOnline()) {
            mOutputText.setText(R.string.no_network_available);
            mOutputText.setVisibility(View.VISIBLE);
        } else if (!isExternalStorageAvailable() && Build.VERSION.SDK_INT >= 23) {
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        } else {
            Log.i(TAG, "send time passed tasks");
            synchronized (this) {
                ArrayList<TaskConfiguration> timePassedTaskList =
                        mTaskManager.getTimePassedTaskList();
                for (TaskConfiguration t : timePassedTaskList) {
                    new MakeRequestTask(mCredential).execute(t);
                }
            }
            Calendar nextTime = mTaskManager.getNextTaskTime();
            setAlarm(nextTime);
            updateTaskList();
            mTaskManager.saveTaskConfigurations();
        }
        if (wakeLock.isHeld()) wakeLock.release();
        Log.i(TAG, "wakelock released");
    }

    private void updateTaskList() {
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

    private void setAlarm(Calendar cal) {
        Log.i(TAG, "setAlarm in");
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
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
        }
    }

    private void showDialog(TaskConfiguration taskConfiguration) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        TaskConifgDialogFragment taskConifgDialogFragment = new TaskConifgDialogFragment();
        taskConifgDialogFragment.setData(mTaskManager, taskConfiguration);
        taskConifgDialogFragment.setDismissListener(dialogDismissedListener);
        taskConifgDialogFragment.show(fragmentManager, "TaskConfigDialogFragment");
    }

    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private boolean isExternalStorageAvailable() {
        int permission = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        return permission != PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                handleTasks();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    public interface DialogDismissedListener {
        public void handleDialogDismissed();
    }

    /**
     * An asynchronous task that handles the Gmail API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<TaskConfiguration, Void, TaskConfiguration> {
        private com.google.api.services.gmail.Gmail mService = null;
        private Exception mLastError = null;
        private ProgressDialog mProgress;
        private TaskConfiguration tempTask;

        public MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.gmail.Gmail.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("SendMailAuto")
                    .build();
            mProgress = new ProgressDialog(MainActivity.this);
        }

        /**
         * Background task to call Gmail API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected TaskConfiguration doInBackground(TaskConfiguration... params) {
            TaskConfiguration t = params[0];
            try {
                String accountName = getPreferences(Context.MODE_PRIVATE).
                        getString(PREF_ACCOUNT_NAME, null);
                tempTask = t;
                MimeMessage mimeMessage = GmailSender.createEmail(t.getRecipient(),
                        accountName, t.getSubject(), t.getMessage());
                GmailSender.sendMessage(mService, "me", mimeMessage);
            } catch (MessagingException e) {
                e.printStackTrace();
            } catch (UserRecoverableAuthIOException userRecoverableException) {
                MainActivity.this.startActivityForResult(
                        userRecoverableException.getIntent(), REQUEST_AUTHORIZATION);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return t;
        }

        @Override
        protected void onPreExecute() {
            mOutputText.setText("");
            mProgress.show();
            if (!twakeLock.isHeld()) twakeLock.acquire();
        }

        @Override
        protected void onPostExecute(TaskConfiguration t) {
            super.onPostExecute(t);
            mProgress.hide();
            if (twakeLock.isHeld()) twakeLock.release();
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
            mTaskManager.addTaskConfiguration(tempTask);
            if (twakeLock.isHeld()) twakeLock.release();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    mOutputText.setText("The following error occurred:\n"
                            + mLastError.getMessage());
                }
            } else {
                mOutputText.setText("Request cancelled.");
            }
        }
    }

    private boolean isFirstTimeOpen() {
        Boolean firstUsed = getPreferences(Context.MODE_PRIVATE)
                .getBoolean(PREF_FIRST_USED, true);

        if (firstUsed) {
            SharedPreferences settings =
                    getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(PREF_FIRST_USED, false);
            editor.apply();
        }

        return firstUsed;
    }
}
