package com.smartstick.ceg4912.capstoneandroidapp.utility;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;

import com.smartstick.ceg4912.capstoneandroidapp.MainActivity;

import java.util.Locale;

public class EmergencyServices implements LocationListener {

    private static final String TAG = "EmergencyServices";
    private double latitude;
    private double longitude;
    private final MainActivity callerActivity;
    private String emergencyNumber;

    public EmergencyServices(MainActivity callerActivity) {
        this.latitude = 0.0f;
        this.longitude = 0.0f;
        this.callerActivity = callerActivity;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        } else {
            Log.e(TAG, "location is null");
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d(TAG, String.format(Locale.ENGLISH, "provider:%s have been disabled", provider));
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d(TAG, String.format(Locale.ENGLISH, "provider:%s have been enabled", provider));
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d(TAG, String.format(Locale.ENGLISH, "provider:%s extras:%s", provider, extras.toString()));
    }

    public void sendEmergencySMS() {
        Log.d(TAG, String.format("Sending emergency number to emergencyNumber:%s", emergencyNumber));
        LocationManager locationManager = (LocationManager) callerActivity.getSystemService(Context.LOCATION_SERVICE);

        if (callerActivity.checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                callerActivity.checkSelfPermission(
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && callerActivity.checkSelfPermission(
                Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            onLocationChanged(location);
            String googleMapApiCall = "https://www.google.com/maps/?q=" + latitude + "," + longitude;
            if (!TextUtils.isEmpty(emergencyNumber)) {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(emergencyNumber, null, googleMapApiCall, null, null);
                Log.d(TAG, "sending...");
            } else {
                Log.d(TAG, "Given emergency number is empty");
            }
        } else {
            Log.d(TAG, "Insufficient permission.");
        }

    }

    public void setEmergencyNumber(String emergencyNumber) {
        Log.d(TAG, String.format("emergencyNumber:%s", emergencyNumber));
        this.emergencyNumber = emergencyNumber;
    }
}
