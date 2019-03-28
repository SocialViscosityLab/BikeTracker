package com.example.dani.biketracker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

import java.util.Observable;
import java.util.Observer;

import sensors.Synchro;
import sensors.WahooConnectorService;
import sensors.WahooConnectorServiceConnection;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class RecordingActivity extends AppCompatActivity implements LocationListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, WahooConnectorService.Listener, WahooConnectorServiceConnection.Listener, Observer, SensorEventListener {

    private Button trigger_session;
    private TextView suggestionView, header, state;
    private Logic logic;
    private final int LOCATION_PERMISSION_REQUEST_CODE = 1252;
    protected static final int REQUEST_ENABLE_BT = 0;

    //For the compass
    private ImageView compass;
    private float currentDegree = 0f;
    private SensorManager mSensorManager;

    private static String TAG = "DEBUGGING";


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

        //Ui managment methods
        initUI();
        //Buttons management methods
        initButton();

        //For the compass
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        //Set activity as observer
        logic.getObserver().addObserver(this);

    }

    protected void onResume(){
        super.onResume();
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_GAME);

    }

    private void initUI() {
        this.suggestionView = findViewById(R.id.suggestion_view);
        this.compass = findViewById(R.id.compass);
        this.header = findViewById(R.id.header);
        this.state = findViewById(R.id.state);
    }

    protected void initButton() {
        this.trigger_session = findViewById(R.id.trigger_session);
        this.trigger_session.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!logic.isAlive()) {
                    logic.start();
                    //trigger_session.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.highlight));

                }else{
                    if (logic.isInSession()) {
                        trigger_session.setText("Start Session");
                        Log.d(TAG, "Session finished!");
                        logic.closeSession();
                        suggestionView.setText("Are you ready?");
                        trigger_session.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.start_button));
                        trigger_session.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.highlight));
                        compass.setImageDrawable(ContextCompat.getDrawable(getApplicationContext() ,R.drawable.inicial_compass));
                        header.setText(getResources().getString(R.string.app_name));
                        state.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.transparent));
                        state.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.transparent));

                    } else {
                        trigger_session.setText("Stop Session");
                        trigger_session.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorSecondary));
                        trigger_session.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.finish_button));
                        logic.startNewSession();
                    }
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
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        long UPDATE_INTERVAL = 2000;
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        long FASTEST_INTERVAL = 1000;
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

            } else {
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(this, "We need your location for the app to work", duration);
                toast.show();
            }
        }
    }

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
    }

    @Override
    public void update(Observable observable, Object o) {
        //Log.d(TAG, "The suggestion is:" + (logic.getSuggestion()));
        header.setText(logic.getRouteName());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (logic.isOnRoute()) {
                    state.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.on_text));
                    state.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.on_background));
                    state.setText("On Route");

                    switch (logic.getSuggestion()) {
                        case -1:
                            //suggestionView.setText("Stop: the leader is " + logic.getGap() + " meters behind you");
                            if (logic.getMyLoc().getSpeed() < 1) {
                                suggestionView.setText("Hold on!");
                                compass.setImageResource(R.drawable.hold_on_compass);
                            } else {
                                suggestionView.setText("Slow down!");
                                compass.setImageResource(R.drawable.slow_down_compass);
                            }
                            break;

                        case 0:
                            suggestionView.setText("You are in the group!");
                            compass.setImageResource(R.drawable.perfect_compass);
                            break;

                        case 1:
                            suggestionView.setText("Speed up!");
                            compass.setImageResource(R.drawable.speed_up_compass);
                            break;
                    }
                }else{
                    suggestionView.setText("Join route");
                    compass.setImageResource(R.drawable.off_route_compass);
                    state.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.off_text));
                    state.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.off_background));
                    state.setText("Off Route");
                }
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        // get the angle around the z-axis rotated
        float degree = Math.round(sensorEvent.values[0]);

        Location myLoc = logic.getMyLoc();
        Location targetLocation = logic.getTarget();


        GeomagneticField gField = new GeomagneticField((float)myLoc.getLatitude(),(float)myLoc.getLongitude(), (float)myLoc.getAltitude(),myLoc.getTime());
        degree += gField.getDeclination();

        float bearing = myLoc.bearingTo(targetLocation);

        degree = normalizeDegree((bearing-degree)* -1);

        // create a rotation animation (reverse turn degree degrees)
        RotateAnimation ra = new RotateAnimation(
                currentDegree,
                -degree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f);

        // how long the animation will take place
        ra.setDuration(210);

        // set the animation after the end of the reservation status
        ra.setFillAfter(true);

        // Start the animation
        compass.startAnimation(ra);
        currentDegree = -degree;
    }

    private float normalizeDegree(float value) {
        if (value >= 0.0f && value <= 180.0f) {
            return value;
        } else {
            return 180 + (180 + value);
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}