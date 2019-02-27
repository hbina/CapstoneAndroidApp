package com.smartstick.ceg4912.capstoneandroidapp.services;

import android.app.Activity;
import android.util.Log;

import com.smartstick.ceg4912.capstoneandroidapp.listener.BearingListener;
import com.smartstick.ceg4912.capstoneandroidapp.model.BearingRequest;
import com.smartstick.ceg4912.capstoneandroidapp.utility.BluetoothConnector;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.List;

public class RfidServices extends Services {

    private final static String TAG = "RfidServices";
    private static String currentLocation = "";
    private final static ArrayDeque<String> nodesInPath = new ArrayDeque<>();

    public static void setNodes(List<String> arr) {
        nodesInPath.clear();
        nodesInPath.addAll(arr);
    }

    public RfidServices(Activity callerActivity) {
        BluetoothConnector.initializeBluetooth(callerActivity);
    }

    @Override
    public void run() {
        super.run();
        // TODO: Rewrite this...make it a lot simpler than this...
        while (isRunning.get()) {
            try {
                if (BluetoothConnector.getInputStream() != null) {
                    int availableBytes = BluetoothConnector.getInputStream().available();
                    if (availableBytes > 0) {
                        Log.d(TAG, String.format("Received something from Bluetooth of size:%d", availableBytes));
                        byte[] rawBytes = new byte[availableBytes];
                        final int receivedLength = BluetoothConnector.getInputStream().read(rawBytes);
                        if (receivedLength > 1) {
                            final String receivedString = (new String(rawBytes, StandardCharsets.UTF_8)).substring(1);
                            Log.d(TAG, "receivedString:" + receivedString);
                            currentLocation = receivedString;
                            if (nodesInPath.isEmpty()) {
                                Log.d(TAG, "Nodes are empty...request a new destination to get a bearing");
                            } else {
                                if (!currentLocation.equals(nodesInPath.peekLast())) {
                                    SpeechServices.addText("You have arrived at " + receivedString);
                                    nodesInPath.removeLast();
                                }
                                BearingRequest bearingRequest = new BearingRequest(currentLocation, nodesInPath.peekLast(), BearingListener.getBearing());
                                RequestServices.addBearingRequest(bearingRequest);
                            }
                        }
                    }
                }

            } catch (
                    IOException exception) {
                Log.e(TAG, exception.getMessage());
            }
        }
        Log.d(TAG, "Thread have finished running");
    }


    // TODO: For the love of God please rework this wonky crud
    private static String decodeNodeNameToId(String peekNodesInPath) {
        switch (peekNodesInPath) {
            case "A":
                return "ADA392FE";
            case "B":
                return "DDA8387E";
            case "C":
                return "ED8EA9FE";
            case "D":
                return "BD2091FE";
            case "E":
                return "8D97357E";
            case "F":
                return "BDA13A7E";
            case "G":
                return "C1D1D909";
            case "H":
                return "F3A0A775";
        }
        return null;
    }

    public static String getCurrentLocation() {
        return currentLocation;
    }
}
