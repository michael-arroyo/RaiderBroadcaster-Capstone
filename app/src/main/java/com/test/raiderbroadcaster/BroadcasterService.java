package com.test.raiderbroadcaster;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Background Service that listens for Location Updates and broadcasts them on a channel via PubNub
 * Assumes that Location is turned on on device
 */
public class BroadcasterService extends Service
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    private Pubnub mPubNub;
    private String busName;
    private String busType;
    private static final String CHANNEL = "source";
    private SharedPreferences sharedPrefs;

    private GoogleApiClient mGoogleApiClient;
    private NotificationManager notificationManager;
    private LocationRequest mLocationRequest;

    private boolean mAlreadyRunning = false;
    private static final int NOTIFICATION_MAIN_ID = 1;
    private static final String TAG = "BroadcasterService";

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Google API Client (with Location Services)
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        mLocationRequest = getLocationRequest();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        sharedPrefs = getSharedPreferences(getString(R.string.PREF_NAME), MODE_PRIVATE);
    }

    /**
     * Returnsa copy of the LocationRequest used by the Broadcast service
     */
    public static LocationRequest getLocationRequest() {
        LocationRequest request = new LocationRequest();
        request.setInterval(10000);
        request.setFastestInterval(5000);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return request;
    }

    /**
     * Disconnect Google Play Services and stop PubNub broadcast
     */
    @Override
    public void onDestroy() {
        stopLocationUpdates();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        if (mPubNub != null) {
            mPubNub.unsubscribeAll();
        }

        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(getString(R.string.PREF_STATE), "stopped");
        editor.commit();

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        // onStartCommand should only be called once
        if (!mAlreadyRunning) {
            busName = intent.getExtras().getString("busname");
            busType = intent.getExtras().getString("bustype");
            mGoogleApiClient.connect();

            initializePubNub();
            mAlreadyRunning = true;

            Intent mainActivityIntent = new Intent(this, MainActivity.class).putExtra("stop", true);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                    mainActivityIntent, 0);
            Notification notification = new NotificationCompat.Builder(this)
                    .setContentTitle("Broadcasting enabled")
                    .setContentText("Touch to disable location broadcast")
                    .setSmallIcon(R.drawable.broadcast_notification)
                    .setContentIntent(contentIntent)
                    .build();
            startForeground(NOTIFICATION_MAIN_ID, notification);

            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putString(getString(R.string.PREF_STATE), "running");
            editor.commit();
        }

        return START_REDELIVER_INTENT;
    }

    /**
     * Binding not supported.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Connection to Google Play Services Suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.v(TAG, "Could not establish connection to Google Play Services");
    }

    /**
     * Initialize PubNub
     */
    private void initializePubNub() {
        mPubNub = new Pubnub(getString(R.string.com_pubnub_publishKey),
                getString(R.string.com_pubnub_subscribeKey));
        mPubNub.setUUID(busName);
    }

    /**
     * Confirm Permissions and start listening for changes to user's location
     */
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    /**
     * Stop listening for changes to user's position
     */
    private void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    /**
     * Convert location info to JSON and publish with PubNub
     */
    @Override
    public void onLocationChanged(Location location) {
        JSONObject message = new JSONObject();
        try {
            message.put("busname", busName);
            message.put("bustype", busType);
            message.put("lat", location.getLatitude());
            message.put("lon", location.getLongitude());
            message.put("time", System.currentTimeMillis());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mPubNub.publish(CHANNEL, message, new Callback() {
            @Override
            public void successCallback(String channel, Object message) {
                Log.d(TAG, "Message: " + message.toString());
            }

            @Override
            public void errorCallback(String channel, PubnubError error) {
                Log.v(TAG, "BroadcasterService - PubNub: Could not publish\n\t" + error.toString());
            }
        });
    }
}