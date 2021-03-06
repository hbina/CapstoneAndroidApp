package com.smartstick.ceg4912.capstoneandroidapp.listener;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.TextView;

import com.smartstick.ceg4912.capstoneandroidapp.MainActivity;
import com.smartstick.ceg4912.capstoneandroidapp.R;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import static com.smartstick.ceg4912.capstoneandroidapp.R.*;

// TODO: Correct bearing stuff...
public class BearingListener implements SensorEventListener {

    private final static String TAG = "BearingListener";
    private static final int ANGLE_DIFFERENCE_THRESHOLD = 3;

    private final float[] mGravity = new float[3];
    private final float[] mGeomagnetic = new float[3];
    private final SensorManager sensorManager;
    private final static AtomicInteger currentBearing = new AtomicInteger(0);
    private final static AtomicInteger bearingOffset = new AtomicInteger(0);
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
                int obtainedAngle = ((int) Math.toDegrees(orientation[0]) + 360) % 360;
                currentBearing.set(obtainedAngle);
                ((TextView) callerActivity.findViewById(id.di_content_bearing)).setText(String.format(Locale.ENGLISH, "Bearing:%d", (currentBearing.get() + bearingOffset.get()) % 360));
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, String.format("Accuracy changed sensor:%s accuracy:%d", sensor.toString(), accuracy));
    }

    public static int getBearing() {
        return (currentBearing.get() + bearingOffset.get()) % 360;
    }

    public static void syncBearingOffset() {
        bearingOffset.set(Math.abs(360 - currentBearing.get()));
    }
}
