package edu.temple.markopromo;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SavedMessagesActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private List<ScanFilter> filters;
    private ScanSettings settings;
    private BluetoothGatt mGatt;
    private BluetoothLeScanner mLEScanner;
    private LocationManager locationManager;
    private ArrayAdapter<String> gridViewArrayAdapter;

    private final static int REQUEST_ENABLE_BT = 1;
    private final static int REQUEST_ENABLE_LOC = 2;
    private static final long SCAN_PERIOD = 60000; // 60 seconds

    private ArrayList<String> messageList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_messages);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        }); */

        // checks if Bluetooth Low Energy supported by device
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported!", Toast.LENGTH_SHORT).show();
            finish();
        }

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mHandler = new Handler();
        settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
                .build();
        filters = new ArrayList<ScanFilter>();
        ScanFilter filter = new ScanFilter.Builder().setDeviceName("mpromo").build();
        filters.add(filter);

        // Turns on Bluetooth
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        /*
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Criteria locationCriteria = new Criteria();
        locationCriteria.setAccuracy(Criteria.ACCURACY_FINE);
        String providerName = locationManager.getBestProvider(locationCriteria, true);

        // Turns on location (GPS)
        if (locationManager == null || !locationManager.isProviderEnabled(providerName)) {
            Intent enableLocIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(enableLocIntent, REQUEST_ENABLE_LOC);
        } */

        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_ENABLE_LOC);
        //Intent enableLocIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        //startActivityForResult(enableLocIntent, REQUEST_ENABLE_LOC);

        demo();

        messageList = new ArrayList<String>();
        populateMessageList();

        GridView gridView = (GridView) findViewById(R.id.message_gridview);
        gridViewArrayAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_list_item_1, messageList);
        gridView.setAdapter(gridViewArrayAdapter);

        //String[] mL = new String[messageList.size()];
        //for(int i = 0; i < messageList.size(); i++) {
        //    mL[i] = messageList.get(i);
        //}

        //MessageGridViewAdapter mgva = new MessageGridViewAdapter(this, android.R.layout.simple_list_item_1, mL);

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();

                Toast toast = Toast.makeText(getApplicationContext(), selectedItem, Toast.LENGTH_SHORT);
                toast.show();
            }
        });

        Button scanButton = (Button) findViewById(R.id.scan_button);

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button button = (Button) view;
                if(button.getText().equals("Scan")) {
                    scanLeDevice(true);
                    button.setText("Stop");
                } else {
                    scanLeDevice(false);
                    button.setText("Scan");
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
        } else {
            mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            filters = new ArrayList<ScanFilter>();
            //scanLeDevice(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mGatt == null) {
            return;
        }
        mGatt.close();
        mGatt = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_saved_messages, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void populateMessageList() {
        messageList.clear();
        String[] fileList = this.fileList();

        for(String s : fileList) {
            if (!s.equals("instant-run"))
                messageList.add(s);
        }
    }

    private void addMessage(PromoMessage message) {
        message.save();
    }

    private void deleteAllMessages() {
        String[] fileList = this.fileList();

        for(String s : fileList) {
            deleteFile(s);
        }
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            //mHandler.postDelayed(new Runnable() {
            //    @Override
            //    public void run() {
            //        mScanning = false;
            //        mLEScanner.stopScan(mScanCallback);
            //    }
            //}, SCAN_PERIOD);

            mScanning = true;
            mLEScanner.startScan(filters, settings, mScanCallback);
            Log.i("scanLeDevice", "enable");
            //Toast toast = Toast.makeText(getApplicationContext(), "scanLeDevice", Toast.LENGTH_SHORT);
            //toast.show();
        } else {
            mScanning = false;
            mLEScanner.stopScan(mScanCallback);
            Log.i("scanLeDevice", "disable");
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            //Toast toast = Toast.makeText(getApplicationContext(), "mScanCallback", Toast.LENGTH_SHORT);
            //toast.show();

            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            BluetoothDevice btDevice = result.getDevice();

            ScanRecord record = result.getScanRecord();
            String devName = record.getDeviceName();
            //String devName = result.getDevice().getName();

            //devName = "." + devName + ".";
            if(devName != null) {
                Log.i("deviceName", devName);
            } else {
                Log.i("deviceName", "null");
            }

            byte[] byteArray = record.getBytes();
            //Log.i("Arrays.toString", Arrays.toString(byteArray));
            //String s = new String(byteArray);
            //Log.i("recordByteArrayString", s);

            Log.i("bytesToHex", bytesToHex(byteArray));

            ScanFilter filter = new ScanFilter.Builder().setDeviceName("mpromo").build();

            if(filter.matches(result)) {
                Log.i("matches", "true");
                PromoMessage message = new PromoMessage(bytesToHex(byteArray), getApplicationContext());
                addMessage(message);
                populateMessageList();
                GridView gridView = (GridView) findViewById(R.id.message_gridview);
                gridView.setAdapter(gridViewArrayAdapter);
                gridViewArrayAdapter.notifyDataSetChanged();
            }

            //connectToDevice(btDevice);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

    public void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            mGatt = device.connectGatt(this, false, gattCallback);
            scanLeDevice(false); // will stop after first device detection
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            Log.i("onServicesDiscovered", services.toString());
            gatt.readCharacteristic(services.get(1).getCharacteristics().get
                    (0));
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            Log.i("onCharacteristicRead", characteristic.toString());
            gatt.disconnect();
        }
    };

    private String bytesToHex(byte[] bytes) {
        char[] hexArray = "0123456789ABCDEF".toCharArray();

        char[] hexChars = new char[bytes.length * 2];

        for(int i = 0; i < bytes.length; i++) {
            int x = bytes[i] & 0xFF;
            hexChars[i * 2] = hexArray[x >>> 4];
            hexChars[i * 2 + 1] = hexArray[x & 0x0F];
        }

        return new String(hexChars);
    }

    private void demo() {
        deleteAllMessages();
        PromoMessage testMessage = new PromoMessage("test_message.txt", getApplicationContext());
        addMessage(testMessage);
        Log.i("demoRun", "true");
    }
}
