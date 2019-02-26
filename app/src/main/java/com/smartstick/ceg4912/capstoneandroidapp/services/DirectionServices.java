package com.smartstick.ceg4912.capstoneandroidapp.services;

import android.util.Log;

import com.smartstick.ceg4912.capstoneandroidapp.model.BearingRequest;
import com.smartstick.ceg4912.capstoneandroidapp.utility.BluetoothConnector;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Stack;

public class DirectionServices extends Services {

    private final static String TAG = "DirectionServices";
    private final static Stack<String> locationHistory = new Stack<>();

    public DirectionServices() {
        BluetoothConnector.initializeBluetooth();
    }

    @Override
    public void run() {
        super.run();
        while (isRunning.get()) {
            try {
                if (BluetoothConnector.getInputStream() != null) {
                    int availableBytes = BluetoothConnector.getInputStream().available();
                    if (availableBytes > 0) {
                        Log.d(TAG, String.format("Received something from Bluetooth of size:%d", availableBytes));
                        byte[] rawBytes = new byte[availableBytes];
                        final int i = BluetoothConnector.getInputStream().read(rawBytes);
                        final String receivedString = (new String(rawBytes, StandardCharsets.UTF_8)).substring(1);
                        if (receivedString.length() > 1) {
                            Log.d(TAG, "receivedString:" + receivedString);
                            // TODO: Rewrite this...make it a lot simpler than this...
                            if (locationHistory.isEmpty()) {
                                locationHistory.add(receivedString);
                            } else {
                                if (!locationHistory.peek().equals(receivedString)) {
                                    locationHistory.add(receivedString);
                                    if (servicesTerminal.isPathNodesEmpty()) {
                                        Log.d(TAG, "User have already arrived");
                                    } else {
                                        if (servicesTerminal.getLatestLocation().equals(decodeNodeNameToId(servicesTerminal.peekNodesInPath()))) {
                                            servicesTerminal.popNodeInPath();
                                        }
                                        BearingRequest bearingRequest = new BearingRequest(servicesTerminal.getLatestLocation(), servicesTerminal.peekNodesInPath(), String.valueOf(servicesTerminal.getCurrentBearing()));
                                        RequestServices.addBearingRequest(bearingRequest);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (IOException exception) {
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
        return locationHistory.peek();
    }
}
