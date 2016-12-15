package edu.temple.markopromo;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
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
import org.w3c.dom.Text;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class SavedMessagesActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private NewMsgCheckboxAdapter newMsgCheckboxAdapter;
    private SavedMsgCheckboxAdapter savedMsgCheckboxAdapter;

    private final static int REQUEST_ENABLE_BT = 1;
    private final static int REQUEST_ENABLE_LOC = 2;

    protected ArrayList<String> newMessageList;
    protected ArrayList<String> savedMessageList;
    protected ArrayList<String> toBeDeletedNewList;
    protected ArrayList<String> toBeDeletedSavedList;
    public static File directory;

    protected GridView newMsgGridView;
    protected GridView savedMsgGridView;

    private static final String bucket = "mpmsg";
    public static final int DELETE_RESULT = 1;
    public static final String NEW_MSG_LIST_KEY = "new_msg_list_key";

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

        // Turns on Bluetooth
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_ENABLE_LOC);

        IntentFilter ifilter = new IntentFilter();
        ifilter.addAction("ACTION_BROADCAST_FILENAME");
        registerReceiver(receiver, ifilter);

        Intent serviceIntent = new Intent(this, BluetoothScanService.class);
        startService(serviceIntent);

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

        newMsgGridView = (GridView) findViewById(R.id.new_msg_gridview);

        newMsgCheckboxAdapter = new NewMsgCheckboxAdapter(this, android.R.layout.simple_list_item_1, newMessageList.toArray());
        newMsgGridView.setAdapter(newMsgCheckboxAdapter);

        savedMsgGridView = (GridView) findViewById(R.id.saved_msg_gridview);

        savedMsgCheckboxAdapter = new SavedMsgCheckboxAdapter(this, android.R.layout.simple_list_item_1, savedMessageList.toArray());
        savedMsgGridView.setAdapter(savedMsgCheckboxAdapter);

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

                    Log.i("deleteButtonNew", newMessageList.toString());
                    newMsgCheckboxAdapter.notifyDataSetChanged();
                    recreate();
                }

                if(toBeDeletedSavedList != null) {
                    for(String s : toBeDeletedSavedList) {
                        deleteFile(s);

                    }
                    populateSavedMessageList();
                    Log.i("deleteButtonSaved", savedMessageList.toString());
                    savedMsgCheckboxAdapter.notifyDataSetChanged();
                    recreate();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        populateNewMessageList();
        newMsgCheckboxAdapter.notifyDataSetChanged();
        //recreate();
        populateSavedMessageList();
        savedMsgCheckboxAdapter.notifyDataSetChanged();
        //recreate();
    }

    @Override
    protected void onPause() {
        super.onPause();

        setStringArrayPref(this, NEW_MSG_LIST_KEY, newMessageList);
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
        newMsgCheckboxAdapter.notifyDataSetChanged();
        recreate();
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

        if (requestCode == DELETE_RESULT) {
            if(resultCode == DELETE_RESULT){
                String fileToDelete = data.getStringExtra("fileToDelete");
                savedMessageList.remove(fileToDelete);
                savedMsgCheckboxAdapter.notifyDataSetChanged();
            }
        }
        recreate();
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("ACTION_BROADCAST_FILENAME")){
                String filename = intent.getStringExtra("filename");
                if(isNewPromo(filename))
                    handleNewMessage(filename);
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
}