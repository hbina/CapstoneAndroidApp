package com.smartstick.ceg4912.capstoneandroidapp.utility;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;

public class EmergencyHelp {

    private final static String TAG = "EmergencyHelp";
    private static String emergencyNumber;

    // TODO:Static methods and constructors...
    public static void sendEmergencySMS(Activity activity) {
        Log.d(TAG, String.format("Sending emergency number to emergencyNumber:%s", emergencyNumber));
        LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);

        if (activity.checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                activity.checkSelfPermission(
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && activity.checkSelfPermission(
                Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            String googleMapApiCall = "https://www.google.com/maps/?q=" + location.getLatitude() + "," + location.getLongitude();
            if (!TextUtils.isEmpty(emergencyNumber)) {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(emergencyNumber, null, googleMapApiCall, null, null);
                Log.d(TAG, "Sending...");
            } else {
                Log.d(TAG, "Given emergency number is empty");
            }
        } else {
            Log.d(TAG, "Insufficient permission.");
        }
    }

    public static void setEmergencyNumber(String number) {
        Log.d(TAG, String.format("New emergency number:%s", number));
        emergencyNumber = number;
    }
}
