package com.test.raiderbroadcaster;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import io.fabric.sdk.android.Fabric;

public class MainActivity extends AppCompatActivity {
    public static final String NAME_KEY = "BUSNAME";

    private StartFragment startFragment;
    private StopFragment stopFragment;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this);
        setContentView(R.layout.activity_main);

        startFragment = new StartFragment();
        stopFragment = new StopFragment();
        fragmentManager = getSupportFragmentManager();

        String state = getSharedPreferences(getString(R.string.PREF_NAME), MODE_PRIVATE)
                .getString(getString(R.string.PREF_STATE), "");
        switch (state) {
            case "running":
                goToStop();
                break;
            default:
                goToStart();
        }
    }

    public void goToStart() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, startFragment).commit();
    }

    public void goToStop() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, stopFragment).commit();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == StartFragment.LOCATION_RESOLVE_ERROR) {
            if (resultCode == RESULT_OK) {
                startFragment.setCanProceed(true);
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                        .setTitle(R.string.location_title)
                        .setMessage(R.string.location_message)
                        .setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //configureLocation();
                            }
                        })
                        .setNegativeButton("Ignore", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                builder.show();
            }
        }
    }
}