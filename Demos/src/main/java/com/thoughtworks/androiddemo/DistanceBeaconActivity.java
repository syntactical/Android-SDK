package com.thoughtworks.androiddemo;

import android.app.Activity;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;
import com.radiusnetworks.ibeacon.*;

import java.util.Collection;
import java.util.List;

public class DistanceBeaconActivity extends Activity implements IBeaconConsumer{

    private static final String TAG = DistanceBeaconActivity.class.getSimpleName();

    // Y positions are relative to height of bg_distance image.
    private static final double RELATIVE_START_POS = 320.0 / 1110.0;
    private static final double RELATIVE_STOP_POS = 885.0 / 1110.0;

    private IBeaconManager beaconManager;
    private Region region;

    private View dotView;
    private int startY = -1;
    private int segmentLength = -1;
    private int major;
    private int minor;
    private double beaconDistance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.distance_view);
        dotView = findViewById(R.id.dot);


        major = getIntent().getIntExtra(ListBeaconsActivity.EXTRAS_BEACON_MAJOR, -1);
        minor = getIntent().getIntExtra(ListBeaconsActivity.EXTRAS_BEACON_MINOR, -1);

        region = new Region("beacon", null, major, minor);

        beaconManager = IBeaconManager.getInstanceForApplication(this);
        beaconManager.bind(this);

        final View view = findViewById(R.id.sonar);
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                startY = (int) (RELATIVE_START_POS * view.getMeasuredHeight());
                int stopY = (int) (RELATIVE_STOP_POS * view.getMeasuredHeight());
                segmentLength = stopY - startY;

                dotView.setVisibility(View.VISIBLE);
                dotView.setTranslationY(computeDotPosY(beaconDistance));
            }
        });
    }

    @Override
    public void onIBeaconServiceConnect() {
        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<IBeacon> iBeacons, Region region) {
                final List<IBeacon> beaconCollection = (List) iBeacons;
                final IBeacon foundBeacon = beaconCollection.get(0);
                beaconDistance = foundBeacon.getAccuracy();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (foundBeacon != null) {
                            updateDistanceView(beaconDistance);
                        }
                    }
                });
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            Toast.makeText(DistanceBeaconActivity.this, "Cannot start ranging, something terrible happened",
                    Toast.LENGTH_LONG).show();
            Log.e(TAG, "Cannot start ranging", e);
        }
    }

    private void updateDistanceView(double beaconDistance) {
        if (segmentLength == -1) {
            return;
        }

        dotView.animate().translationY(computeDotPosY(beaconDistance)).start();
    }

    private int computeDotPosY(double beaconDistance) {
        // Let's put dot at the end of the scale when it's further than 6m.
        double distance = Math.min(beaconDistance, 6.0);
        return startY + (int) (segmentLength * (distance / 6.0));
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
    protected void onStop() {
        beaconManager.unBind(this);

        super.onStop();
    }
}
