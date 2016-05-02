package com.test.raiderbroadcaster;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class StopFragment extends Fragment {
    private TextView busnameText;
    private Button stopBroadcastBtn;
    private MainActivity activity;
    private SharedPreferences sharedPrefs;

    public StopFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activity = (MainActivity) getActivity();
        sharedPrefs = activity.getSharedPreferences(getString(R.string.PREF_NAME),
                Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_stop, container, false);

        busnameText = (TextView) view.findViewById(R.id.text_busname);
        busnameText.setText(sharedPrefs.getString(MainActivity.NAME_KEY, ""));

        stopBroadcastBtn = (Button) view.findViewById(R.id.btn_stop_broadcast);
        stopBroadcastBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopBroadcast();
            }
        });
        return view;
    }

    /**
     * Stop the Broadcast service
     */
    public void stopBroadcast() {
        getActivity().stopService(new Intent(getContext(), BroadcasterService.class));
        activity.goToStart();
    }
}