package com.example.dani.biketracker;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.wahoofitness.connector.HardwareConnectorEnums;
import com.wahoofitness.connector.capabilities.Capability;
import com.wahoofitness.connector.capabilities.RunSpeed;
import com.wahoofitness.connector.conn.connections.SensorConnection;
import com.wahoofitness.connector.conn.connections.params.ConnectionParams;

import java.util.Set;

import static android.provider.Settings.Global.DEVICE_NAME;
import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class RecordingActivity extends AppCompatActivity implements LocationListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, WahooConnectorService.Listener, WahooConnectorServiceConnection.Listener {
    private Button trigger_session;
    private Logic logic;

    private LocationRequest mLocationRequest;
    private long UPDATE_INTERVAL = 2000;
    private long FASTEST_INTERVAL = 1000;
    private final int LOCATION_PERMISSION_REQUEST_CODE = 1252;

    private static String TAG = "DEBUGGING";

    protected static final int REQUEST_ENABLE_BT = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);

        logic = Logic.getInstance();
        logic.firestoreSetup();

        // Location management methods
        getLastLocation();
        startLocationUpdates();


        // Bluetooth management methods
        //findBluetoothDevice();

        //Buttons management methods
        initButton();

    }

    protected void initButton() {
        this.trigger_session = findViewById(R.id.trigger_session);
        this.trigger_session.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!logic.isAlive()) {
                    logic.start();
                }
                if (logic.isInSession()) {
                    trigger_session.setText("Start Session");
                    Log.d(TAG, "Session finished!");
                    logic.closeSession();

                } else {
                    trigger_session.setText("Stop Session");
                    logic.startNewSession();
                    //logic.getFDB();
                }
            }
        });
    }


    public void getLastLocation() {

        // Get last known recent location using new Google Play Services SDK (v11+)
        FusedLocationProviderClient locationClient = getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);

            return;
        }

        locationClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {

                    @Override
                    public void onSuccess(Location location) {
                        // GPS location can be null if GPS is switched off
                        if (location != null) {

                            onLocationChanged(location);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "Error trying to get last GPS location");
                        e.printStackTrace();
                    }
                });
    }


    // Trigger new location updates at interval
    protected void startLocationUpdates() {

        // Create the location request to start receiving updates
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

        // Create LocationSettingsRequest object using location request
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        // Check whether location settings are satisfied
        // https://developers.google.com/android/reference/com/google/android/gms/location/SettingsClient
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        getFusedLocationProviderClient(this).requestLocationUpdates(mLocationRequest, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        onLocationChanged(locationResult.getLastLocation());
                    }
                },
                Looper.myLooper());
    }


    public void onLocationChanged(Location location) {
        //If the location changes, the logic receive the new location
        logic.setMyLoc(location);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // start to find location...

            } else { // if permission is not granted

                // decide what you want to do if you don't get permissions
            }
        }
    }


    /*TODO:
    private RunSpeed getRunSpeedCap() {
        return (RunSpeed) getCapability(Capability.CapabilityType.RunSpeed);
    }*/

    private final RunSpeed.Listener mRunSpeedListener = new RunSpeed.Listener() {
        private RunSpeed.Data mLastCallbackData;

        @Override
        public void onRunSpeedData(RunSpeed.Data data) {
            mLastCallbackData = data;
            // TODO: REACT
        }

        @Override
        public void onRunSpeedDataReset() {
            //TODO: registerCallbackResult("onRunSpeedDataReset", "");

        }
    };

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Log.d(TAG, data.getDataString());
            }
        }
    }
/*
    private void findBluetoothDevice() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // device doesn't support bluetooth
            Log.d(TAG, "The device doesn't support bluetooth");

        } else {

            // bluetooth is off, ask user to on it.
            if (!bluetoothAdapter.isEnabled()) {
                Log.d(TAG, "Bluetooth is off");
                Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableAdapter, 0);
            }

            // Do whatever you want to do with your bluetoothAdapter
            Set<BluetoothDevice> all_devices = bluetoothAdapter.getBondedDevices();
            if (all_devices.size() > 0) {
                for (BluetoothDevice currentDevice : all_devices) {
                    Log.d(TAG, "Device Name " + currentDevice.getName());
                    if (currentDevice.getName().equals(DEVICE_NAME)) {
                        BluetoothDevice bluetoothDevice = currentDevice;
                        logic.setBluetoothConnection(bluetoothDevice);
                        boolean bluetoothDeviceFound = true;
                        break;
                    }
                }
            }
        }
    }
*/


    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onDeviceDiscovered(ConnectionParams params) {

    }

    @Override
    public void onDiscoveredDeviceLost(ConnectionParams params) {

    }

    @Override
    public void onDiscoveredDeviceRssiChanged(ConnectionParams params) {

    }

    @Override
    public void onFirmwareUpdateRequired(SensorConnection sensorConnection, String currentVersionNumber, String recommendedVersion) {

    }

    @Override
    public void onNewCapabilityDetected(SensorConnection sensorConnection, Capability.CapabilityType capabilityType) {

    }

    @Override
    public void onSensorConnectionStateChanged(SensorConnection sensorConnection, HardwareConnectorEnums.SensorConnectionState state) {

    }

    @Override
    public void onHardwareConnectorServiceConnected(WahooConnectorService hardwareConnectorService) {

        hardwareConnectorService.addListener(RecordingActivity.this);
        RecordingActivity.this
                .onHardwareConnectorServiceConnected(hardwareConnectorService);
         //getRunSpeedCap().addListener(mRunSpeedListener);

    }
    @Override
    public void onHardwareConnectorServiceDisconnected() {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
/*TODO:
        RunSpeed runSpeed = getRunSpeedCap();
        if (runSpeed != null) {
            runSpeed.removeListener(mRunSpeedListener);
        }*/
    }

}