package com.smartstick.ceg4912.capstoneandroidapp.services;

import android.util.Log;

import com.smartstick.ceg4912.capstoneandroidapp.model.BearingRequest;
import com.smartstick.ceg4912.capstoneandroidapp.utility.BluetoothConnector;
import com.smartstick.ceg4912.capstoneandroidapp.utility.ServicesTerminal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class DirectionServices extends Services {

    private final static String TAG = "DirectionServices";

    public DirectionServices() {
        BluetoothConnector.initializeBluetooth();
    }

    @Override
    public void run() {
        ServicesTerminal servicesTerminal = ServicesTerminal.getServicesTerminal();
        servicesTerminal.setIsBluetoothRunning(true);
        while (isRunning.get()) {
            try {
                if (!servicesTerminal.isInputStreamProvided()) {
                    int availableBytes = servicesTerminal.getBluetoothInputStream().available();
                    if (availableBytes > 0) {
                        Log.d(TAG, String.format("Received something from Bluetooth of size:%d", availableBytes));
                        byte[] rawBytes = new byte[availableBytes];
                        final int i = servicesTerminal.getBluetoothInputStream().read(rawBytes);
                        final String receivedString = (new String(rawBytes, StandardCharsets.UTF_8)).substring(1);
                        if (receivedString.length() > 1) {
                            Log.d(TAG, "receivedString:" + receivedString);
                            if (servicesTerminal.isLocationHistoryEmpty()) {
                                servicesTerminal.addLocationHistory(receivedString);
                            } else {
                                if (!servicesTerminal.getLatestLocation().equals(receivedString)) {
                                    servicesTerminal.addLocationHistory(receivedString);
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
}
