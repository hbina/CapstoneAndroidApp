package com.smartstick.ceg4912.capstoneandroidapp.utility;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.TextView;

import com.smartstick.ceg4912.capstoneandroidapp.MainActivity;

public class BearingServices implements SensorEventListener {

    private float[] mGravity = new float[3];
    private float[] mGeomagnetic = new float[3];
    private SensorManager sensorManager;
    private final static String TAG = "BearingServices";
    private float oldBearing;
    private TextView bearingText ;

    public BearingServices(MainActivity callerAcitivity) {
        this.sensorManager = (SensorManager) callerAcitivity.getSystemService(Context.SENSOR_SERVICE);
    }

    public void registerListener(TextView textView) {
        this.bearingText = textView;
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
    }

    public void unregisterListener() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        bearingText.setText(this.getBearing() + "°");
        final float alpha = 0.97f;
        synchronized (this) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                for (int a = 0; a < mGravity.length; a++) {
                    mGravity[a] = alpha * mGravity[a] + (1 - alpha) * event.values[a];
                }
            }
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                for (int a = 0; a < mGravity.length; a++) {
                    mGeomagnetic[a] = alpha * mGeomagnetic[a] + (1 - alpha) * event.values[a];
                }
            }

            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                float azimuth = (float) Math.toDegrees(orientation[0]);
                azimuth = (azimuth + 360) % 360;
                oldBearing = ServicesTerminal.getServicesTerminal().getCurrentBearing();
                if (Math.abs(oldBearing - azimuth) > 10) {
                    Log.d(TAG, String.format("oldBearing:%f newBearing:%f", oldBearing, azimuth));
                    ServicesTerminal.getServicesTerminal().setCurrentBearing(azimuth);
                }
            }
        }
    }

    public float getBearing(){
        return oldBearing;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, String.format("Accuracy changed sensor:%s accuracy:%d", sensor.toString(), accuracy));
    }
}
