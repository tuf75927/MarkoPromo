package edu.temple.markopromo;

import android.Manifest;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BluetoothScanService extends IntentService {
    //private boolean mScanning;
    //private Handler mHandler;
    private List<ScanFilter> filters;
    private ScanSettings settings;
    private BluetoothLeScanner mLEScanner;
    private final IBinder mBinder = new LocalBinder();
    private static final String deviceName = "mpromo";
    private String filename;

    public BluetoothScanService() {
        super("BluetoothScanService");
    }

    public class LocalBinder extends Binder {
        BluetoothScanService getService() {
            return BluetoothScanService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();
            mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
            //mHandler = new Handler();
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
                    .build();
            filters = new ArrayList<ScanFilter>();
            ScanFilter filter = new ScanFilter.Builder().setDeviceName(deviceName).build();
            filters.add(filter);

            scanLeDevice(true);
        }
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            //mScanning = true;
            mLEScanner.startScan(filters, settings, mScanCallback);
            Log.i("scanLeDevice", "enable");
            //Toast toast = Toast.makeText(getApplicationContext(), "scanLeDevice", Toast.LENGTH_SHORT);
            //toast.show();
        } else {
            //mScanning = false;
            mLEScanner.stopScan(mScanCallback);
            Log.i("scanLeDevice", "disable");
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
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

            ScanFilter filter = new ScanFilter.Builder().setDeviceName(deviceName).build();

            if(filter.matches(result)) {
                Log.i("matches", "true");
                filename = parseFileName(byteArray);

                if(SavedMessagesActivity.isNewPromo(filename)) {
                    Timestamp stamp = new Timestamp(System.currentTimeMillis());
                    Date date = new Date(stamp.getTime());
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm");
                    String formattedDate = dateFormat.format(date);
                    //String location = SavedMessagesActivity.getLocation(getApplicationContext());

                    Intent broadcastIntent = new Intent();
                    broadcastIntent.setAction("ACTION_BROADCAST_FILENAME");
                    broadcastIntent.putExtra("filename", filename);
                    broadcastIntent.putExtra("timestamp", formattedDate);
                    //broadcastIntent.putExtra("location", location);
                    sendBroadcast(broadcastIntent);
                }
            }
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

    private String parseFileName(byte[] bytes) {
        String str = new String(bytes);
        //String hex = bytesToHex(bytes);
        String[] tokens = str.split(":");
        String name = tokens[1];
        Log.i("STR", str);
        //Log.i("HEX", hex);
        Log.i("tokens[0]", tokens[0]);
        Log.i("tokens[1]", tokens[1]);
        Log.i("name", name);

        int periodLocation = 0;
        for(int i = 0; i < name.length(); i++) {
            if (name.charAt(i) == '.')
                periodLocation = i;
        }
        Log.i("periodLocation", "" + periodLocation);
        Log.i("nameTruncated", "*" + name.substring(0, periodLocation + 4) + "*");

        return name.substring(0, periodLocation + 4);
    }

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
}
