package com.estimote.examples.demos;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.radiusnetworks.ibeacon.IBeacon;
import com.thoughtworks.androiddemo.R;

import java.util.*;

public class LeDeviceListAdapter extends BaseAdapter {

    private ArrayList<IBeacon> beacons;
    private LayoutInflater inflater;

    public LeDeviceListAdapter(Context context) {
        this.inflater = LayoutInflater.from(context);
        this.beacons = new ArrayList<IBeacon>();
    }

    public void replaceWith(Collection<IBeacon> newBeacons) {
        this.beacons.clear();
        this.beacons.addAll(newBeacons);
        Collections.sort(beacons, new Comparator<IBeacon>() {
            @Override
            public int compare(IBeacon lhs, IBeacon rhs) {
                return (int) Math.signum(lhs.getAccuracy() - rhs.getAccuracy());
            }
        });
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return beacons.size();
    }

    @Override
    public IBeacon getItem(int position) {
        return beacons.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        view = inflateIfRequired(view, position, parent);
        bind(getItem(position), view);
        return view;
    }

    private void bind(IBeacon beacon, View view) {
        ViewHolder holder = (ViewHolder) view.getTag();
        holder.distanceTextView.setText(String.format("Distance: %.2fm (%s)", beacon.getAccuracy(), getProximityRangeName(beacon)));
        holder.majorTextView.setText("Major: " + beacon.getMajor());
        holder.minorTextView.setText("Minor: " + beacon.getMinor());
        holder.txPowerTextView.setText("Tx Power: " + beacon.getTxPower());
        holder.rssiTextView.setText("RSSI: " + beacon.getRssi());
    }

    private String getProximityRangeName(IBeacon beacon) {
        Map<Integer, String> proximityRanges = new HashMap<Integer, String>();

        proximityRanges.put(IBeacon.PROXIMITY_UNKNOWN, "UNKNOWN");
        proximityRanges.put(IBeacon.PROXIMITY_IMMEDIATE, "IMMEDIATE");
        proximityRanges.put(IBeacon.PROXIMITY_NEAR, "NEAR");
        proximityRanges.put(IBeacon.PROXIMITY_FAR, "FAR");

        return proximityRanges.get(beacon.getProximity());
    }

    private View inflateIfRequired(View view, int position, ViewGroup parent) {
        if (view == null) {
            view = inflater.inflate(R.layout.device_item, null);
            view.setTag(new ViewHolder(view));
        }
        return view;
    }

    static class ViewHolder {
        final TextView distanceTextView;
        final TextView majorTextView;
        final TextView minorTextView;
        final TextView txPowerTextView;
        final TextView rssiTextView;

        ViewHolder(View view) {
            distanceTextView = (TextView) view.findViewWithTag("distance");
            majorTextView = (TextView) view.findViewWithTag("major");
            minorTextView = (TextView) view.findViewWithTag("minor");
            txPowerTextView = (TextView) view.findViewWithTag("txpower");
            rssiTextView = (TextView) view.findViewWithTag("rssi");
        }
    }
}
