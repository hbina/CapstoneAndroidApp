package com.smartstick.ceg4912.capstoneandroidapp.listener;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.smartstick.ceg4912.capstoneandroidapp.MainActivity;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

// TODO: Correct bearing stuff...
public class BearingListener implements SensorEventListener {

    private final static String TAG = "BearingListener";
    private static final int DIFFERENCE_TRESHOLD = 3;

    private final float[] mGravity = new float[3];
    private final float[] mGeomagnetic = new float[3];
    private final SensorManager sensorManager;
    private final static AtomicInteger currentBearing = new AtomicInteger(0);
    private final static AtomicInteger bearingOffset = new AtomicInteger(360);
    private final MainActivity callerActivity;

    public BearingListener(MainActivity callerActivity) {
        this.callerActivity = callerActivity;
        this.sensorManager = (SensorManager) callerActivity.getSystemService(Context.SENSOR_SERVICE);
    }

    public void registerListener() {
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
    }

    public void unregisterListener() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
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
                int azimuth = (int) Math.toDegrees(orientation[0]);
                azimuth = (azimuth + bearingOffset.get()) % 360;
                if (Math.abs(azimuth - currentBearing.get()) > DIFFERENCE_TRESHOLD) {
                    Log.d(TAG, String.format("oldBearing:%d newBearing:%d", currentBearing.get(), azimuth));
                    currentBearing.set((int) azimuth);
                    callerActivity.TEXT_VIEW_BEARING.setText(String.format(Locale.ENGLISH, "Bearing:%d", currentBearing.get()));
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, String.format("Accuracy changed sensor:%s accuracy:%d", sensor.toString(), accuracy));
    }

    public static String getBearing() {
        return String.valueOf(currentBearing.get());
    }

    public static void syncBearingOffset() {
        bearingOffset.set(Math.abs(bearingOffset.get() - currentBearing.get()));
    }
}
