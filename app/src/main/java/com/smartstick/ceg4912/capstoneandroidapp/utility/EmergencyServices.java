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

import androidx.core.app.ActivityCompat;

public class EmergencyServices implements LocationListener {


    private double latitude;
    private double longitude;
    private MainActivity callerActivity;
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
            Log.e(this.toString(), "location is null");
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d(this.toString(), String.format(Locale.ENGLISH, "provider:%s have been disabled", provider));
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d(this.toString(), String.format(Locale.ENGLISH, "provider:%s have been enabled", provider));
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d(this.toString(), String.format(Locale.ENGLISH, "provider:%s extras:%s", provider, extras.toString()));
    }

    public void sendEmergencySMS() {
        LocationManager locationManager = (LocationManager) callerActivity.getSystemService(Context.LOCATION_SERVICE);

        if (checkEmergencyPermissions()) {
            ActivityCompat.requestPermissions(callerActivity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.SEND_SMS},
                    1);
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            onLocationChanged(location);
            String googleMapApiCall = "https://www.google.com/maps/?q=" + latitude + "," + longitude;
            if (!TextUtils.isEmpty(emergencyNumber)) {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(emergencyNumber, null, googleMapApiCall, null, null);
            } else {
                Log.e(this.toString(), "Permissions are not granted.");
            }
        }

    }

    public boolean checkEmergencyPermissions() {
        return callerActivity.checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                callerActivity.checkSelfPermission(
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && callerActivity.checkSelfPermission(
                Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED;
    }

    public void setEmergencyNumber(String emergencyNumber) {
        this.emergencyNumber = emergencyNumber;
    }
}
