package com.thoughtworks.androiddemo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import com.radiusnetworks.ibeacon.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ListBeaconsActivity extends Activity implements IBeaconConsumer {

    private static final String TAG = ListBeaconsActivity.class.getSimpleName();

    public static final String EXTRAS_TARGET_ACTIVITY = "extrasTargetActivity";
    public static final String EXTRAS_BEACON_MAJOR = "extrasBeaconMajor";
    public static final String EXTRAS_BEACON_MINOR = "extrasBeaconMinor";

    private static final int REQUEST_ENABLE_BT = 1234;
    private static final String ESTIMOTE_BEACON_PROXIMITY_UUID = "B9407F30-F5F8-466E-AFF9-25556B57FE6D";
    private static final String ESTIMOTE_IOS_PROXIMITY_UUID = "8492E75F-4FD6-469D-B132-043FE94921D8";
    private static final Region ALL_BEACONS_REGION = new Region("rid", null, null, null);

    private IBeaconManager beaconManager = IBeaconManager.getInstanceForApplication(this);

    private LeDeviceListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        // Configure device list.
        adapter = new LeDeviceListAdapter(this);
        ListView list = (ListView) findViewById(R.id.device_list);
        list.setAdapter(adapter);
        list.setOnItemClickListener(createOnItemClickListener());

        // If Bluetooth is not enabled, let user enable it.
        verifyBluetooth();
    }

    private List<IBeacon> filterBeacons(Collection<IBeacon> beacons) {
        List<IBeacon> filteredBeacons = new ArrayList<IBeacon>(beacons.size());
        for (IBeacon beacon : beacons) {
            if (beacon.getProximityUuid().equalsIgnoreCase(ESTIMOTE_BEACON_PROXIMITY_UUID)
                    || beacon.getProximityUuid().equalsIgnoreCase(ESTIMOTE_IOS_PROXIMITY_UUID)) {
                filteredBeacons.add(beacon);
            }
        }
        return filteredBeacons;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scan_menu, menu);
        MenuItem refreshItem = menu.findItem(R.id.refresh);
        refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        beaconManager.unBind(this);

        super.onDestroy();
    }

    private void verifyBluetooth() {
        if (!IBeaconManager.getInstanceForApplication(this).checkAvailability()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            Log.i(TAG, "binding beaconManager");
            beaconManager.bind(this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                beaconManager.bind(this);
            } else {
                Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_LONG).show();
                getActionBar().setSubtitle("Bluetooth not enabled");
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private AdapterView.OnItemClickListener createOnItemClickListener() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (getIntent().getStringExtra(EXTRAS_TARGET_ACTIVITY) != null) {
                    try {
                        Class<?> clazz = Class.forName(getIntent().getStringExtra(EXTRAS_TARGET_ACTIVITY));
                        Intent intent = new Intent(ListBeaconsActivity.this, clazz);
                        intent.putExtra(EXTRAS_BEACON_MAJOR, adapter.getItem(position).getMajor());
                        intent.putExtra(EXTRAS_BEACON_MINOR, adapter.getItem(position).getMinor());
                        startActivity(intent);
                    } catch (ClassNotFoundException e) {
                        Log.e(TAG, "Finding class by name failed", e);
                    }
                }
            }
        };
    }

    @Override
    public void onIBeaconServiceConnect() {
        getActionBar().setSubtitle("Scanning...");
        adapter.replaceWith(Collections.<IBeacon>emptyList());

        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<IBeacon> iBeacons, Region region) {

                final List<IBeacon> estimoteBeacons = filterBeacons(iBeacons);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getActionBar().setSubtitle("Found beacons: " + estimoteBeacons.size());
                        adapter.replaceWith(estimoteBeacons);

                    }
                });
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(ALL_BEACONS_REGION);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
