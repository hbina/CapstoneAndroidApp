package com.smartstick.ceg4912.capstoneandroidapp;

import android.util.Log;

import com.smartstick.ceg4912.capstoneandroidapp.listener.BearingListener;
import com.smartstick.ceg4912.capstoneandroidapp.services.RequestServices;
import com.smartstick.ceg4912.capstoneandroidapp.services.RfidServices;
import com.smartstick.ceg4912.capstoneandroidapp.services.SpeechServices;
import com.smartstick.ceg4912.capstoneandroidapp.utility.VoiceCommand;

class ServicesThread extends Thread {

    private final SpeechServices speechServices;
    private final RequestServices requestServices;
    private final RfidServices rfidServices;
    private final BearingListener bearingListener;

    private final static String TAG = "ServicesThread";

    //TODO: Use RoomDB or LiteSQL instead...
    ServicesThread(MainActivity callerActivity) {
        speechServices = new SpeechServices(callerActivity);
        requestServices = new RequestServices(callerActivity);
        rfidServices = new RfidServices(callerActivity);
        bearingListener = new BearingListener(callerActivity);
        VoiceCommand.init();
    }

    @Override
    public void run() {
        Log.d(TAG, "Begin spawning services...");
        speechServices.start();
        requestServices.start();
        rfidServices.start();
        bearingListener.registerListener();
        Log.d(TAG, "Done spawning services...");
    }

    void killServices() {
        Log.d(TAG, "Begin killing services...");
        speechServices.killService();
        requestServices.killService();
        rfidServices.killService();
        bearingListener.unregisterListener();
        Log.d(TAG, "Done killing services...");
    }
}
