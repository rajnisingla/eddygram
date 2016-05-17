package com.example.e01200.eddystoneadvertiser;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    ToggleButton btnAdvertise , btnLock , btnConnectable ;
    TextView tvStatus , tvDevices , tvData ;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final String TAG = MainActivity.class.getCanonicalName();
    private static final byte FRAME_TYPE_UID = 0x01;
    private static byte URL_SCHEME = 0x03;

    private String mUrlAdvertise = "goo.gl/Vfcj";
    byte txPower = (byte) -26;

    private static final UUID CLIENT_CHARACTERISTIC_CONFIGURATION_UUID = UUID
            .fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final ParcelUuid SERVICE_UUID =
            ParcelUuid.fromString("0000fed8-0000-1000-8000-00805f9b34fb");

    private static final UUID CONFIGURATION_SERVICE_UUID =
            UUID.fromString("ee0c2080-8786-40ba-ab96-99b91ac981d8");

    private static final UUID LOCK_STATE_UUID = UUID
            .fromString("ee0c2081-8786-40ba-ab96-99b91ac981d8");

    private static final UUID LOCK_UUID = UUID
            .fromString("ee0c2082-8786-40ba-ab96-99b91ac981d8");

    private static final UUID UNLOCK_UUID = UUID
            .fromString("ee0c2083-8786-40ba-ab96-99b91ac981d8");

    private static final UUID URI_DATA_UUID = UUID
            .fromString("ee0c2084-8786-40ba-ab96-99b91ac981d8");


    private static final UUID URI_FLAGS_UUID = UUID
            .fromString("ee0c2085-8786-40ba-ab96-99b91ac981d8");


    private static final UUID Tx_POWER_LEVEL_UUID = UUID
            .fromString("ee0c2086-8786-40ba-ab96-99b91ac981d8");

    private static final UUID Tx_POWER_MODE_UUID = UUID
            .fromString("ee0c2087-8786-40ba-ab96-99b91ac981d8");

    private static final UUID BEACON_PERIOD_UUID = UUID
            .fromString("ee0c2088-8786-40ba-ab96-99b91ac981d8");

    private static final UUID RESET_UUID = UUID
            .fromString("ee0c2089-8786-40ba-ab96-99b91ac981d8");


    // GATT
    private BluetoothGattService mConfigurationService, mMainService;
    private BluetoothGattCharacteristic mLockStateCharacteristic , mLockCharacteristic , mUnLockCharacteristic , mUriDataCharacteristic ,
            mUriFlagsCharacteristic , mPowerLevelCharacteristic , mPowerModeCharacteristic , mBeaconPeriodCharacteristic , mResetCharacteristic ;

    private HashSet<BluetoothDevice> mBluetoothDevices;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private AdvertiseData mAdvData;
    private AdvertiseSettings mAdvSettings;
    private BluetoothLeAdvertiser mAdvertiser;

    private boolean isLocked = false;
    private boolean isConnectable = false;
    private boolean isAdvertising = false;

    private final AdvertiseCallback mAdvCallback = new AdvertiseCallback() {


        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.e(TAG, "Not broadcasting: " + errorCode);
            int statusText;
            switch (errorCode) {
                case ADVERTISE_FAILED_ALREADY_STARTED:
                    statusText = R.string.status_advertising;
                    Log.w(TAG, "App was already advertising");
                    break;
                case ADVERTISE_FAILED_DATA_TOO_LARGE:
                    statusText = R.string.status_advDataTooLarge;
                    break;
                case ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    statusText = R.string.status_advFeatureUnsupported;
                    break;
                case ADVERTISE_FAILED_INTERNAL_ERROR:
                    statusText = R.string.status_advInternalError;
                    break;
                case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    statusText = R.string.status_advTooManyAdvertisers;
                    break;
                default:
                    statusText = R.string.status_notAdvertising;
                    Log.wtf(TAG, "Unhandled error: " + errorCode);
            }

            isAdvertising = false;

            btnAdvertise.setChecked(false);

            tvStatus.setText(statusText);
        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.v(TAG, "Broadcasting");
            isAdvertising = true;
            btnAdvertise.setChecked(true);
            tvStatus.setText(R.string.status_advertising);
        }
    };

    private BluetoothGattServer mGattServer;
    private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, final int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    mBluetoothDevices.add(device);
                    updateConnectedDevicesStatus();
                    Log.v(TAG, "Connected to device: " + device.getAddress());
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    mBluetoothDevices.remove(device);
                    updateConnectedDevicesStatus();
                    Log.v(TAG, "Disconnected from device");
                }
            } else {
                mBluetoothDevices.remove(device);
                updateConnectedDevicesStatus();
                // There are too many gatt errors (some of them not even in the documentation) so we just
                // show the error to the user.
                final String errorMessage = getString(R.string.status_errorWhenConnecting) + ": " + status;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
                Log.e(TAG, "Error when connecting: " + status);
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            /*Log.d(TAG, "Device tried to read characteristic: " + characteristic.getUuid());
            Log.d(TAG, "Value: " + Arrays.toString(characteristic.getValue()));
            if (offset != 0) {
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset,
            *//* value (optional) *//* null);
                return;
            }*/

            byte[] value = new byte[0];

            if(characteristic.getUuid().equals(mLockStateCharacteristic.getUuid())){

                value = new byte[]{(byte)(isLocked?1:0)};


            }else if(characteristic.getUuid().equals(mUriDataCharacteristic.getUuid())){

                byte[] url = mUrlAdvertise.getBytes();//"localhost:8888".getBytes();//
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                try {
                    os.write(new byte[]{URL_SCHEME});
                    os.write(url);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                value =  os.toByteArray();

            }else if(characteristic.getUuid().equals(mUriFlagsCharacteristic.getUuid())){



            }else if(characteristic.getUuid().equals(mPowerLevelCharacteristic.getUuid())){



            }else if(characteristic.getUuid().equals(mPowerModeCharacteristic.getUuid())){



            }else if(characteristic.getUuid().equals(mBeaconPeriodCharacteristic.getUuid())){



            }else if(characteristic.getUuid().equals(mResetCharacteristic.getUuid())){



            }

            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                    offset, value);
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
            Log.v(TAG, "Notification sent. Status: " + status);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                                 final BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded,
                                                 final int offset, final byte[] value) {

            int status = 0;

            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite,
                    true, offset, value);
            Log.v(TAG, "Characteristic Write request: " + Arrays.toString(value));

            if(characteristic.getUuid().equals(mUnLockCharacteristic.getUuid())){

                if(!isLocked){

                }else{

                }

            }else if(!isLocked&&value.length>0) {


                if (characteristic.getUuid().equals(mLockCharacteristic.getUuid())) {


                } else if (characteristic.getUuid().equals(mUriDataCharacteristic.getUuid())) {

                    byte scheme = value[0];

                    switch(scheme){

                        case 0x00 :
                            URL_SCHEME = scheme ;
                            break;
                        case 0x01 :
                            URL_SCHEME = scheme ;
                            break;
                        case 0x02 :
                            URL_SCHEME = scheme ;
                            break;
                        case 0x03 :
                            URL_SCHEME = scheme ;
                            break;
                        default :
                            URL_SCHEME = 0x02 ;
                            break;
                    }

                    try {
                        mUrlAdvertise = new String(Arrays.copyOfRange(value,1 , value.length), "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    byte[] serviceData = null;
                    try {
                        serviceData = buildServiceData();
                    } catch (IOException e) {
                        Log.e(TAG, e.toString());
                        Toast.makeText(MainActivity.this, "failed to build service data", Toast.LENGTH_SHORT).show();
                    }



                    mAdvData = new AdvertiseData.Builder()
                            .addServiceData(SERVICE_UUID, serviceData)
                            .addServiceUuid(SERVICE_UUID)
                            .setIncludeTxPowerLevel(false)
                            .setIncludeDeviceName(false)
                            .build();

                    if(isAdvertising){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                stopAdvertising();
                                startAdvertising();
                            }
                        });


                    }


                } else if (characteristic.getUuid().equals(mUriFlagsCharacteristic.getUuid())) {


                } else if (characteristic.getUuid().equals(mPowerLevelCharacteristic.getUuid())) {


                } else if (characteristic.getUuid().equals(mPowerModeCharacteristic.getUuid())) {


                } else if (characteristic.getUuid().equals(mBeaconPeriodCharacteristic.getUuid())) {


                } else if (characteristic.getUuid().equals(mResetCharacteristic.getUuid())) {


                }

            }else{

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,"First unlock the device then try!",Toast.LENGTH_SHORT).show();
                    }
                });
            }
            // if (responseNeeded) {
            mGattServer.sendResponse(device, requestId, status,
            /* No need to respond with an offset */ 0,
            /* No need to respond with a value */ null);
            // }
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded,
                                             int offset,
                                             byte[] value) {
            Log.v(TAG, "Descriptor Write Request " + descriptor.getUuid() + " " + Arrays.toString(value));
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded,
                    offset, value);
            if(responseNeeded) {
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
            /* No need to respond with offset */ 0,
            /* No need to respond with a value */ null);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        tvStatus = (TextView) findViewById(R.id.tvStaus);
        tvDevices = (TextView) findViewById(R.id.tvDevices);
        tvData = (TextView) findViewById(R.id.tvData);
        btnAdvertise = (ToggleButton) findViewById(R.id.tbAdvertise);
        btnConnectable = (ToggleButton) findViewById(R.id.tbConnectable);
        btnLock = (ToggleButton) findViewById(R.id.tbLock);

        mBluetoothDevices = new HashSet<>();
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        initBLEData();

        btnAdvertise.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                isAdvertising = isChecked;

                if(isChecked){

                    startAdvertising();
                }else{

                    stopAdvertising();
                }

            }
        });

        btnConnectable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                isConnectable = isChecked;

                mAdvSettings = new AdvertiseSettings.Builder()
                        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                        .setConnectable(isConnectable)
                        .build();

                if(isAdvertising) {
                    stopAdvertising();
                    startAdvertising();
                }
            }
        });

        btnLock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isLocked = isChecked;
            }
        });

        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    void initBLEData(){
        mLockStateCharacteristic = new BluetoothGattCharacteristic(LOCK_STATE_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ);

        mLockCharacteristic = new BluetoothGattCharacteristic(LOCK_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);

        mUnLockCharacteristic = new BluetoothGattCharacteristic(UNLOCK_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);

        mUriDataCharacteristic = new BluetoothGattCharacteristic(URI_DATA_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ|BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ|BluetoothGattCharacteristic.PERMISSION_WRITE);

        mUriFlagsCharacteristic = new BluetoothGattCharacteristic(URI_FLAGS_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ|BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ|BluetoothGattCharacteristic.PERMISSION_WRITE);

        mPowerLevelCharacteristic = new BluetoothGattCharacteristic(Tx_POWER_LEVEL_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ|BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ|BluetoothGattCharacteristic.PERMISSION_WRITE);

        mPowerModeCharacteristic = new BluetoothGattCharacteristic(Tx_POWER_MODE_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ|BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ|BluetoothGattCharacteristic.PERMISSION_WRITE);

        mBeaconPeriodCharacteristic = new BluetoothGattCharacteristic(BEACON_PERIOD_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ|BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ|BluetoothGattCharacteristic.PERMISSION_WRITE);

        mResetCharacteristic = new BluetoothGattCharacteristic(RESET_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);



        mConfigurationService = new BluetoothGattService(CONFIGURATION_SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        mConfigurationService.addCharacteristic(mLockStateCharacteristic);
        mConfigurationService.addCharacteristic(mLockCharacteristic);
        mConfigurationService.addCharacteristic(mUnLockCharacteristic);
        mConfigurationService.addCharacteristic(mUriDataCharacteristic);
        mConfigurationService.addCharacteristic(mUriFlagsCharacteristic);
        mConfigurationService.addCharacteristic(mPowerLevelCharacteristic);
        mConfigurationService.addCharacteristic(mPowerModeCharacteristic);
        mConfigurationService.addCharacteristic(mResetCharacteristic);

        mMainService = new BluetoothGattService(UUID.fromString(SERVICE_UUID.toString()),
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        mAdvSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(false)
                .build();

        byte[] serviceData = null;
        try {
            serviceData = buildServiceData();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            Toast.makeText(this, "failed to build service data", Toast.LENGTH_SHORT).show();
        }



        mAdvData = new AdvertiseData.Builder()
                .addServiceData(SERVICE_UUID, serviceData)
                .addServiceUuid(SERVICE_UUID)
                .setIncludeTxPowerLevel(false)
                .setIncludeDeviceName(false)
                .build();


    /*mAdvData = new AdvertiseData.Builder()
        .setIncludeDeviceName(true)
        .setIncludeTxPowerLevel(true)
        .addServiceUuid(mCurrentServiceFragment.getServiceUUID())
        .build();*/
    }




    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive (Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                if(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                        == BluetoothAdapter.STATE_OFF) {

                    resetState();

                }
                }
            }



    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
                    Toast.makeText(this, R.string.bluetoothAdvertisingNotSupported, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Advertising not supported");
                }
                onStart();
            } else {
                //TODO(g-ortuno): UX for asking the user to activate bt
                Toast.makeText(this, R.string.bluetoothNotEnabled, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Bluetooth not enabled");
                finish();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        resetState();
        // If the user disabled Bluetooth when the app was in the background,
        // openGattServer() will return null.
        mGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);
        if (mGattServer == null) {
            ensureBleFeaturesAvailable();
            return;
        }
        // Add a service for a total of three services (Generic Attribute and Generic Access
        // are present by default).
        mGattServer.addService(mMainService);
        mGattServer.addService(mConfigurationService);



    }


    private void startAdvertising(){

        if(mBluetoothAdapter.isEnabled()) {

            if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {
                mAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
                tvStatus.setText(R.string.status_advStarted);
                if (mAdvertiser != null)
                    mAdvertiser.startAdvertising(mAdvSettings, mAdvData, mAdvCallback);

                tvData.setText(mUrlAdvertise);

            } else {
                tvStatus.setText(R.string.status_noLeAdv);
            }
        }else{

            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

    }


    private void stopAdvertising(){

        if (mBluetoothAdapter.isEnabled() && mAdvertiser != null) {
            // If stopAdvertising() gets called before close() a null
            // pointer exception is raised.
            mAdvertiser.stopAdvertising(mAdvCallback);
            tvStatus.setText("Not Advertising");

        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGattServer != null) {
            mGattServer.close();
        }
        stopAdvertising();
        resetState();
    }

    @Override
    protected void onDestroy() {

        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    private void resetState() {

        tvStatus.setText(R.string.status_notAdvertising);
        btnAdvertise.setChecked(false);
        isAdvertising = false;
        btnConnectable.setChecked(false);
        isConnectable = false;
        btnLock.setChecked(false);
        isLocked = false;
        updateConnectedDevicesStatus();

    }

    private void updateConnectedDevicesStatus() {
        final String message = getString(R.string.status_devicesConnected) + " "
                + mBluetoothManager.getConnectedDevices(BluetoothGattServer.GATT).size();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvDevices.setText(message);
            }
        });
    }

    private byte[] buildServiceData() throws IOException {

        byte[] url = mUrlAdvertise.getBytes();//"localhost:8888".getBytes();//
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write(new byte[]{FRAME_TYPE_UID, txPower , URL_SCHEME});
        os.write(url);
        Log.d(TAG, "buildServiceData: length => "+os.size());
        return os.toByteArray();
    }

    private void ensureBleFeaturesAvailable() {
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.bluetoothNotSupported, Toast.LENGTH_LONG).show();
            Log.e(TAG, "Bluetooth not supported");
            finish();
        } else if (!mBluetoothAdapter.isEnabled()) {
            // Make sure bluetooth is enabled.
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }
    private void disconnectFromDevices() {
        Log.d(TAG, "Disconnecting devices...");
        for (BluetoothDevice device : mBluetoothManager.getConnectedDevices(
                BluetoothGattServer.GATT)) {
            Log.d(TAG, "Devices: " + device.getAddress() + " " + device.getName());
            mGattServer.cancelConnection(device);
        }
    }
}
