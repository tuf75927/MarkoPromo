package edu.temple.markopromo;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class SavedMessagesActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;

    private final static int REQUEST_ENABLE_BT = 1;
    private final static int REQUEST_ENABLE_LOC = 2;
    public static final int DELETE_RESULT = 3;

    private static final String bucket = "mpmsg";
    public static final String NEW_MSG_LIST_KEY = "new_msg_list_key";

    protected ArrayList<String> newMessageList;
    protected ArrayList<String> savedMessageList;
    protected ArrayList<String> toBeDeletedNewList;
    protected ArrayList<String> toBeDeletedSavedList;
    public static File directory;

    //protected GridView newMsgGridView;
    //protected GridView savedMsgGridView;
    //private NewMsgCheckboxAdapter newMsgCheckboxAdapter;
    //private SavedMsgCheckboxAdapter savedMsgCheckboxAdapter;

    private boolean isActivityRunning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_saved_messages);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // checks if Bluetooth Low Energy supported by device
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported!", Toast.LENGTH_SHORT).show();
            finish();
        }

        IntentFilter ifilter = new IntentFilter();
        ifilter.addAction("ACTION_BROADCAST_FILENAME");
        registerReceiver(receiver, ifilter);

        directory = getFilesDir();

        newMessageList = new ArrayList<String>();
        savedMessageList = new ArrayList<String>();
        toBeDeletedNewList = new ArrayList<String>();
        toBeDeletedSavedList = new ArrayList<String>();

        if (savedInstanceState != null) {
            newMessageList = savedInstanceState.getStringArrayList(NEW_MSG_LIST_KEY);
        } else {
            populateNewMessageList();
        }

        populateSavedMessageList();

        Log.i("initGrid", "onCreate");
        initGridviews(newMessageList, savedMessageList);

        Button deleteButton = (Button) findViewById(R.id.delete_button);

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(toBeDeletedNewList != null) {
                    for(String s: toBeDeletedNewList) {
                        int i = 0;
                        for(String t : newMessageList) {
                            if(s.equalsIgnoreCase(t)) {
                                newMessageList.remove(i);
                                break;
                            }
                            i++;
                        }
                    }

                    //newMsgCheckboxAdapter.notifyDataSetChanged();
                    //recreate();
                }

                if(toBeDeletedSavedList != null) {
                    for(String s : toBeDeletedSavedList) {
                        deleteFile(s);

                    }
                    populateSavedMessageList();
                    Log.i("deleteButtonSaved", savedMessageList.toString());
                    //savedMsgCheckboxAdapter.notifyDataSetChanged();
                    //recreate();
                }

                Log.i("initGrid", "delete button listener");
                initGridviews(newMessageList, savedMessageList);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        isActivityRunning = true;

        // Turns on Bluetooth
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            Intent serviceIntent = new Intent(this, BluetoothScanService.class);
            startService(serviceIntent);
        }

        //showLocationStatePermission();

        //requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_ENABLE_LOC);

        populateNewMessageList();
        //newMsgCheckboxAdapter.notifyDataSetChanged();
        populateSavedMessageList();
        //savedMsgCheckboxAdapter.notifyDataSetChanged();
        Log.i("initGrid", "onResume");
        initGridviews(newMessageList, savedMessageList);
    }

    @Override
    protected void onPause() {
        super.onPause();

        setStringArrayPref(this, NEW_MSG_LIST_KEY, newMessageList);

        Log.i("initGrid", "setStringArrayPref " + newMessageList.toString());

        isActivityRunning = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(receiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_saved_messages, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void populateNewMessageList() {
        for(String s : getStringArrayPref(this, NEW_MSG_LIST_KEY)) {
            if(isNewPromo(s))
                newMessageList.add(s);
        }
    }

    private void populateSavedMessageList() {
        savedMessageList.clear();

        String[] fileList = this.fileList();

        for(String s : fileList) {
            if (!s.equals("instant-run"))
                savedMessageList.add(s);
        }
    }

    private void deleteAllMessages() {
        String[] fileList = this.fileList();

        for(String s : fileList) {
            deleteFile(s);
        }
    }

    private void handleNewMessage(String filename) {
        newMessageList.add(filename);
        //newMsgCheckboxAdapter.notifyDataSetChanged();
        Log.i("initGrid", "handleNewMessage");
        initGridviews(newMessageList, savedMessageList);

        setStringArrayPref(this, NEW_MSG_LIST_KEY, newMessageList);
    }

    protected boolean isNewPromo(String filename) {
        boolean result = true;

        for(String s : newMessageList) {
            if(s.equalsIgnoreCase(filename))
                result = false;
        }

        for(String s : savedMessageList) {
            if(s.equalsIgnoreCase(filename))
                result = false;
        }

        return result;
    }

    private void downloadMessage(final String filename) {
        Thread downloadMessageFile = new Thread() {
            @Override
            public void run() {

                if (isNetworkActive()) {
                    AmazonS3 s3 = new AmazonS3Client(new AnonymousAWSCredentials());

                    TransferUtility transferUtility = new TransferUtility(s3, getApplicationContext());

                    File message = new File(directory, filename);

                    TransferObserver observer = transferUtility.download(bucket, filename, message);

                    observer.setTransferListener(new TransferListener(){

                        @Override
                        public void onStateChanged(int id, TransferState state) {
                            Log.i("TransferState", state.toString());
                            if(state.toString() == "COMPLETED") {
                                savedMessageList.add(filename);
                                newMessageList.remove(filename);

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //savedGridViewArrayAdapter.notifyDataSetChanged();
                                        //newGridViewArrayAdapter.notifyDataSetChanged();

                                        launchDisplayMessageActivity(filename);
                                    }
                                });
                            }
                            if(state.toString() =="FAILED") {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), "Error downloading " + filename, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }

                        @Override
                        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                        }

                        @Override
                        public void onError(int id, Exception ex) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "Error downloading " + filename, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Please connect to a network!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        };

        downloadMessageFile.start();
    }

    private boolean isNetworkActive() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private void launchDisplayMessageActivity(String filename) {
        Intent displayIntent = new Intent(getApplicationContext(), DisplayMessageActivity.class);
        displayIntent.putExtra("filename", filename);
        displayIntent.putExtra("filelist", savedMessageList);
        startActivityForResult(displayIntent, DELETE_RESULT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(requestCode == REQUEST_ENABLE_BT) {
            if(resultCode == RESULT_OK) {
                Intent serviceIntent = new Intent(this, BluetoothScanService.class);
                startService(serviceIntent);
            }
        } else if (requestCode == DELETE_RESULT) {
            if(resultCode == DELETE_RESULT){
                String fileToDelete = data.getStringExtra("fileToDelete");
                savedMessageList.remove(fileToDelete);
                //savedMsgCheckboxAdapter.notifyDataSetChanged();
                Log.i("initGrid", "onActivityResult");
                initGridviews(newMessageList, savedMessageList);
            }
        }
    }

    private void showLocationStatePermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                showExplanation("Permission Needed", "Rationale", Manifest.permission.ACCESS_FINE_LOCATION, REQUEST_ENABLE_LOC);
            } else {
                requestPermission(Manifest.permission.ACCESS_FINE_LOCATION, REQUEST_ENABLE_LOC);
            }
        } else {
            Toast.makeText(this, "Permission (already) Granted!", Toast.LENGTH_SHORT).show();
        }
    }

    private void showExplanation(String title,
                                 String message,
                                 final String permission,
                                 final int permissionRequestCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        requestPermission(permission, permissionRequestCode);
                    }
                });
        builder.create().show();
    }

    private void requestPermission(String permissionName, int permissionRequestCode) {
        ActivityCompat.requestPermissions(this,
                new String[]{permissionName}, permissionRequestCode);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String permissions[],
            int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ENABLE_LOC:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission Granted!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                }
        }
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("ACTION_BROADCAST_FILENAME")){
                String filename = intent.getStringExtra("filename");
                if(isNewPromo(filename)) {
                    handleNewMessage(filename);
                    if(!isActivityRunning)
                        issueNotification();
                }
            }
        }
    };

    private static void setStringArrayPref(Context context, String key, ArrayList<String> values) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        JSONArray a = new JSONArray();
        for (int i = 0; i < values.size(); i++) {
            a.put(values.get(i));
        }
        if (!values.isEmpty()) {
            editor.putString(key, a.toString());
        } else {
            editor.putString(key, null);
        }
        editor.commit();

        Log.i("SharedPrefs","saved newMessageList");
        Log.i("JSONvalues", a.toString());
    }

    private static ArrayList<String> getStringArrayPref(Context context, String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String json = prefs.getString(key, null);
        ArrayList<String> urls = new ArrayList<String>();
        if (json != null) {
            try {
                JSONArray a = new JSONArray(json);
                for (int i = 0; i < a.length(); i++) {
                    String url = a.optString(i);
                    urls.add(url);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        Log.i("SharedPrefs","loaded newMessageList");
        Log.i("JSONvalues", urls.toString());

        return urls;
    }

    @Override
    protected void onSaveInstanceState (Bundle outState) {
        outState.putStringArrayList(NEW_MSG_LIST_KEY, newMessageList);
        super.onSaveInstanceState(outState);

        Log.i("StateSaved", newMessageList.toString());
    }

    public class SavedMsgCheckboxAdapter extends ArrayAdapter {
        Context context;
        Object[] list;

        public SavedMsgCheckboxAdapter(Context context, int resource, Object[] list) {
            super(context, resource, list);
            this.context = context;
            this.list = list;
            Log.i("checkbox created", "true");
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            linearLayout.setOrientation(LinearLayout.HORIZONTAL);

            final CheckBox checkBox = new CheckBox(context);

            checkBox.setText("   ");
            checkBox.setId(position);
            checkBox.setClickable(true);

            checkBox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                    int i = compoundButton.getId();
                    String str = list[i].toString();
                    if(isChecked) {
                        toBeDeletedSavedList.add(str);
                        Log.i("toBeDeleted S", toBeDeletedSavedList.toString());
                    } else {
                        int j = 0;
                        for(String s : toBeDeletedSavedList) {
                            if(s.equalsIgnoreCase(str)) {
                                toBeDeletedSavedList.remove(j);
                                Log.i("toBeDeleted S", toBeDeletedSavedList.toString());
                                break;
                            }
                            j++;
                        }
                    }
                }
            });

            final TextView textView = new TextView(context);

            textView.setText(list[position].toString());
            textView.setTextSize(20);
            //textView.setId(position);
            textView.setClickable(true);
            textView.setFocusable(true);
            textView.setSelectAllOnFocus(true);
            int h = textView.getLineHeight();
            textView.setMinHeight(h*2);

            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toBeDeletedSavedList.clear();
                    toBeDeletedNewList.clear();
                    launchDisplayMessageActivity(textView.getText().toString());
                }
            });

            Log.i("checkbox pos", ""+position);

            linearLayout.addView(checkBox);
            linearLayout.addView(textView);

            return linearLayout;
        }
    }

    public class NewMsgCheckboxAdapter extends ArrayAdapter {
        Context context;
        Object[] list;
        ArrayList<TextView> myTextViews;

        public NewMsgCheckboxAdapter(Context context, int resource, Object[] list) {
            super(context, resource, list);
            this.context = context;
            this.list = list;
            myTextViews = new ArrayList<TextView>();
            Log.i("checkbox created", "true");
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            linearLayout.setOrientation(LinearLayout.HORIZONTAL);

            final CheckBox checkBox = new CheckBox(context);

            checkBox.setText("   ");
            checkBox.setId(position);
            checkBox.setClickable(true);

            checkBox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                    int i = compoundButton.getId();
                    String str = list[i].toString();
                    if(isChecked) {
                        toBeDeletedNewList.add(str);
                        Log.i("toBeDeleted N", toBeDeletedNewList.toString());
                    } else {
                        int j = 0;
                        for(String s : toBeDeletedNewList) {
                            if(s.equalsIgnoreCase(str)) {
                                toBeDeletedNewList.remove(j);
                                Log.i("toBeDeleted N", toBeDeletedNewList.toString());
                                break;
                            }
                            j++;
                        }
                    }
                }
            });

            final TextView textView = new TextView(context);

            textView.setText(list[position].toString());
            textView.setTextSize(20);
            textView.setId(position);
            textView.setClickable(true);
            textView.setFocusable(true);
            textView.setSelectAllOnFocus(true);
            int h = textView.getLineHeight();
            textView.setMinHeight(h*2);

            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toBeDeletedSavedList.clear();
                    toBeDeletedNewList.clear();
                    downloadMessage(textView.getText().toString());
                }
            });

            myTextViews.add(textView);

            Log.i("checkbox pos", ""+position);

            linearLayout.addView(checkBox);
            linearLayout.addView(textView);

            return linearLayout;
        }
    }

    private void issueNotification() {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.notification_icon)
                        .setContentTitle("MarkoPromo")
                        .setContentText("You have received a new promo!")
                        .setAutoCancel(true);

        Intent resultIntent = new Intent(this, SavedMessagesActivity.class);

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        getApplicationContext(),
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        mBuilder.setContentIntent(resultPendingIntent);

        int mNotificationId = 1;
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        mNotifyMgr.notify(mNotificationId, mBuilder.build());
        Log.i("Notification", "sent");
        finish();
    }

    private void initGridviews(ArrayList<String> newMsgs, ArrayList<String> savedMsgs) {
        GridView newMsgGridView = (GridView) findViewById(R.id.new_msg_gridview);

        NewMsgCheckboxAdapter newMsgCheckboxAdapter = new NewMsgCheckboxAdapter(this, android.R.layout.simple_list_item_1, newMsgs.toArray());
        newMsgGridView.setAdapter(newMsgCheckboxAdapter);

        GridView savedMsgGridView = (GridView) findViewById(R.id.saved_msg_gridview);

        SavedMsgCheckboxAdapter savedMsgCheckboxAdapter = new SavedMsgCheckboxAdapter(this, android.R.layout.simple_list_item_1, savedMsgs.toArray());
        savedMsgGridView.setAdapter(savedMsgCheckboxAdapter);

        Log.i("initGrid N", newMsgs.toString());
        Log.i("initGrid S", savedMsgs.toString());
    }
}