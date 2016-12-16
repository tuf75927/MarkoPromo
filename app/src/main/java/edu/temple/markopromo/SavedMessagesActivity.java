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
import android.support.annotation.NonNull;
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
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SavedMessagesActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    private BluetoothAdapter mBluetoothAdapter;
    private static GoogleApiClient mGoogleApiClient;

    private final static int REQUEST_ENABLE_BT = 1;
    private final static int REQUEST_ENABLE_LOC = 2;
    public static final int DELETE_RESULT = 3;

    private static final String bucket = "mpmsg";
    public static final String NEW_MSG_LIST_KEY = "new_msg_list_key";
    public static final String NEW_PM_LIST_KEY = "new_pm_list_key";
    public static final String LOC_MAP_KEY = "loc_map_key";
    public static final String TIME_MAP_KEY = "time_map_key";

    protected static ArrayList<String> newMessageList;
    protected static ArrayList<String> savedMessageList;
    protected static ArrayList<String> toBeDeletedNewList;
    protected static ArrayList<String> toBeDeletedSavedList;

    //protected ArrayList<PromoMessage> promoMessageList;

    private static HashMap<String, String> locationMap;
    private static HashMap<String, String> timestampMap;

    public static File directory;
    protected static String tempLocation;

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

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();

        //getLocation(this);

        IntentFilter ifilter = new IntentFilter();
        ifilter.addAction("ACTION_BROADCAST_FILENAME");
        registerReceiver(receiver, ifilter);

        directory = getFilesDir();

        newMessageList = new ArrayList<>();
        savedMessageList = new ArrayList<>();
        toBeDeletedNewList = new ArrayList<>();
        toBeDeletedSavedList = new ArrayList<>();

        //promoMessageList = new ArrayList<PromoMessage>();
        locationMap = new HashMap<>();
        timestampMap = new HashMap<>();

        if (savedInstanceState != null) {
            //promoMessageList = (ArrayList<PromoMessage>) savedInstanceState.getSerializable(NEW_PM_LIST_KEY);
            newMessageList = savedInstanceState.getStringArrayList(NEW_MSG_LIST_KEY);
            locationMap = (HashMap) savedInstanceState.getSerializable(LOC_MAP_KEY);
            timestampMap = (HashMap) savedInstanceState.getSerializable(TIME_MAP_KEY);
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
                if (toBeDeletedNewList != null) {
                    for (String s : toBeDeletedNewList) {
                        int i = 0;
                        for (String t : newMessageList) {
                            if (s.equalsIgnoreCase(t)) {
                                newMessageList.remove(i);
                                locationMap.remove(t);
                                timestampMap.remove(t);
                                break;
                            }
                            i++;
                        }
                    }

                    //newMsgCheckboxAdapter.notifyDataSetChanged();
                    //recreate();
                }

                if (toBeDeletedSavedList != null) {
                    for (String s : toBeDeletedSavedList) {
                        deleteFile(s);
                        locationMap.remove(s);
                        timestampMap.remove(s);
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
        //getHashMapPref(this, LOC_MAP_KEY);
        //getHashMapPref(this, TIME_MAP_KEY);

        Log.i("initGrid", "onResume");
        initGridviews(newMessageList, savedMessageList);
    }

    @Override
    protected void onPause() {
        super.onPause();

        setStringArrayPref(this, NEW_MSG_LIST_KEY, newMessageList);
        //setHashMapPref(this, LOC_MAP_KEY, locationMap);
        //setHashMapPref(this, TIME_MAP_KEY, timestampMap);
        //setPromoMessageArrayPref(this, NEW_PM_LIST_KEY, promoMessageList);
        //setHashMapPref(this, LOC_MAP_KEY, locationMap);

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
        for (String s : getStringArrayPref(this, NEW_MSG_LIST_KEY)) {
            if (isNewPromo(s))
                newMessageList.add(s);
        }
    }

    private void populateSavedMessageList() {
        savedMessageList.clear();

        String[] fileList = this.fileList();

        for (String s : fileList) {
            if (!s.equals("instant-run"))
                savedMessageList.add(s);
        }
    }

    private void deleteAllMessages() {
        String[] fileList = this.fileList();

        for (String s : fileList) {
            deleteFile(s);
        }
    }

    private void handleNewMessage(String filename, String datetime) {

        newMessageList.add(filename);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        final String f = filename;
        final String t = datetime;

        PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi.getCurrentPlace(mGoogleApiClient, null);
        result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
            @Override
            public void onResult(PlaceLikelihoodBuffer likelyPlaces) {
                String location = "Unknown location";
                double maxLikelihood = 0;
                for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                    Log.i("LOCDEBUG", String.format("Place '%s' has likelihood: %g",
                            placeLikelihood.getPlace().getName(),
                            placeLikelihood.getLikelihood()));
                    if(placeLikelihood.getLikelihood() >= maxLikelihood) {
                        maxLikelihood = placeLikelihood.getLikelihood();
                        location = placeLikelihood.getPlace().getName().toString();
                    }
                }
                likelyPlaces.release();
                Log.i("LOCDEBUG", "most likely location = " + location);

                //PromoMessage pm = new PromoMessage(f, t, location);
                //Log.i("LOCDEBUG", "new PM: " + f + ", " + t + ", " + location);
                //promoMessageList.add(pm);

                locationMap.put(f, location);
                timestampMap.put(f, t);

                Log.i("initGrid", "handleNewMessage");
                initGridviews(newMessageList, savedMessageList);

                setStringArrayPref(getApplicationContext(), NEW_MSG_LIST_KEY, newMessageList);
            }
        });
    }

    protected static boolean isNewPromo(String filename) {
        boolean result = true;

        for (String s : newMessageList) {
            if (s.equalsIgnoreCase(filename))
                result = false;
        }

        for (String s : savedMessageList) {
            if (s.equalsIgnoreCase(filename))
                result = false;
        }

        return result;
    }

    protected static String getLocation(Context context) {
        String f;
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return null;
        }

        PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi.getCurrentPlace(mGoogleApiClient, null);
        result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
            @Override
            public void onResult(PlaceLikelihoodBuffer likelyPlaces) {
                String location = "Unknown location";
                double maxLikelihood = 0;
                for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                    Log.i("LOCDEBUG", String.format("Place '%s' has likelihood: %g",
                            placeLikelihood.getPlace().getName(),
                            placeLikelihood.getLikelihood()));
                    if(placeLikelihood.getLikelihood() >= maxLikelihood) {
                        maxLikelihood = placeLikelihood.getLikelihood();
                        location = placeLikelihood.getPlace().getName().toString();
                    }
                }
                likelyPlaces.release();
                Log.i("LOCDEBUG", "most likely location = " + location);
                tempLocation = location;
            }
        });

        return tempLocation;
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
                locationMap.remove(fileToDelete);
                timestampMap.remove(fileToDelete);
                //savedMsgCheckboxAdapter.notifyDataSetChanged();
                Log.i("initGrid", "onActivityResult");
                initGridviews(newMessageList, savedMessageList);
            }
        }
    }

    /*
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
    } */

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("ACTION_BROADCAST_FILENAME")){
                String fn = intent.getStringExtra("filename");
                String ts = intent.getStringExtra("timestamp");

                Log.i("LOCDEBUG", "timestamp = " + ts);

                if(isNewPromo(fn)) {
                    handleNewMessage(fn, ts);
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

        for(int i = 0; i < values.size(); i++) {
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

    private static void setHashMapPref(Context context, String key, HashMap map) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();

        Gson gson = new Gson();
        String json = gson.toJson(map);

        editor.putString(key, json);
        editor.commit();

        Log.i("DEBUGMAP", "JSON string: " + json);
    }

    private static void getHashMapPref(Context context, String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        Gson gson = new Gson();
        String json = prefs.getString(key, null);
        if(json != null) {
            if (key.equals(LOC_MAP_KEY)) {
                locationMap.clear();
                locationMap = gson.fromJson(json, HashMap.class);
            } else if (key.equals(TIME_MAP_KEY)) {
                timestampMap.clear();
                timestampMap = gson.fromJson(json, HashMap.class);
            }
        }

    }

    @Override
    protected void onSaveInstanceState (Bundle outState) {
        outState.putStringArrayList(NEW_MSG_LIST_KEY, newMessageList);
        //outState.putSerializable(NEW_MSG_LIST_KEY, newPromoMessageList);
        outState.putSerializable(LOC_MAP_KEY, locationMap);
        outState.putSerializable(TIME_MAP_KEY, timestampMap);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i("LOCDEBUG", "connection failed");
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

            final String f = list[position].toString();
            textView.setText(getDisplayName(f));
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
                    launchDisplayMessageActivity(f);
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

            final String f = list[position].toString();
            textView.setText(getDisplayName(f));
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
                    downloadMessage(f);
                }
            });

            myTextViews.add(textView);

            Log.i("checkbox pos", ""+position);

            linearLayout.addView(checkBox);
            linearLayout.addView(textView);

            return linearLayout;
        }
    }

    private String getDisplayName(String filename) {
        String result = "Metadata not found";

        if(locationMap.containsKey(filename)) {
            result = locationMap.get(filename) + ", " + timestampMap.get(filename);
        }

        return result;

        /*
        for(PromoMessage pm : promoMessageList) {
            if(pm.getFilename().equalsIgnoreCase(filename)) {
                result = pm.getLocation() + ", " + pm.getDatetime();
            }
        }
        return result;*/
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