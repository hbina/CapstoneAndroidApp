package com.smartstick.ceg4912.capstoneandroidapp.utility;

import android.util.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.SSLEngineResult;

public class ListeningForBluetoothThread extends Thread {

    private final static String TAG = "ListeningForBluetoothThread";
    private final DirectionServices directionServices;

    public ListeningForBluetoothThread(DirectionServices directionServices) {
        this.directionServices = directionServices;
    }

    @Override
    public void run() {
        ServicesTerminal servicesTerminal = ServicesTerminal.getServicesTerminal();
        servicesTerminal.setIsBluetoothRunning(true);
        while (!Thread.currentThread().isInterrupted() && servicesTerminal.getIsBluetoothRunning()) {
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
                                        directionServices.getBearingFromDb(servicesTerminal.getLatestLocation(), servicesTerminal.peekNodesInPath(), String.valueOf(servicesTerminal.getCurrentBearing()));
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
