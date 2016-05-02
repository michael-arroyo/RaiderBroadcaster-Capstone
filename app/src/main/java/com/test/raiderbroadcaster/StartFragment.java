package com.test.raiderbroadcaster;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

/**
 * Fragment to start the Broadcast service after the user has supplied a bus name
 */
public class StartFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private Button broadcastBtn;
    private EditText busnameInput;
    private SharedPreferences sharedPrefs;
    private boolean mCanProceed = false;
    private GoogleApiClient mGoogleApiClient;
    private LocationSettingsRequest mLocationRequest;
    MainActivity activity;

    public static final int LOCATION_RESOLVE_ERROR = 1001;

    public StartFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

        activity = (MainActivity) getActivity();
        sharedPrefs = activity.getSharedPreferences(getString(R.string.PREF_NAME),
                Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_start, container, false);
        broadcastBtn = (Button) view.findViewById(R.id.btn_start_broadcast);
        busnameInput = (EditText) view.findViewById(R.id.input_busname);

        String busName = sharedPrefs.getString(MainActivity.NAME_KEY, "");
        busnameInput.setText(busName);
        busnameInput.setSelection(busnameInput.getText().length());

        broadcastBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startBroadcast();
            }
        });

        return view;
    }

    /**
     * If location is properly configured, save bus name and start broadcasting
     * Otherwise, prompt user to change settings
     */
    public void startBroadcast() {
        if (mCanProceed) {
            String busname = busnameInput.getText().toString();
            if (busname.equals("")) {
                busnameInput.setError("Bus name must not be empty");
                return;
            }

            // Save Bus ID
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putString(MainActivity.NAME_KEY, busname);
            editor.commit();

            Intent serviceIntent = new Intent(getActivity(), BroadcasterService.class);
            serviceIntent.putExtra("busname", busname);
            getActivity().startService(serviceIntent);

            activity.goToStop();
        } else {
            configureLocation();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        configureLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e("StartFragment", "Connection to Google Play Services failed");
    }

    /**
     * Determines if location settings are appropriate before starting Broadcast service
     * If not, makes a request for the user to change them.
     */
    private void configureLocation() {
        if (mLocationRequest == null) {
            LocationRequest request = BroadcasterService.getLocationRequest();
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(request);
            mLocationRequest = builder.build();
        }

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                        mLocationRequest);

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed by showing
                        // the user a dialog
                        try {
                            status.startResolutionForResult(getActivity(), LOCATION_RESOLVE_ERROR);
                        } catch (IntentSender.SendIntentException e) {
                            e.printStackTrace();
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Unsatisfied and theres nothing we can do about it
                        // TODO: Show some explanation
                        break;
                    default:
                        // Properly configured. Allow to start service
                        mCanProceed = true;
                }
            }
        });
    }

    public void setCanProceed(boolean mCanProceed) {
        this.mCanProceed = mCanProceed;
    }
}