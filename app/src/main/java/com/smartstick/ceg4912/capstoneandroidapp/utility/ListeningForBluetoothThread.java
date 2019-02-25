package com.smartstick.ceg4912.capstoneandroidapp.utility;

import android.util.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ListeningForBluetoothThread implements Runnable {

    private final static String TAG = "ListeningForBluetoothThread";
    private final DirectionServices directionServices;

    public ListeningForBluetoothThread(DirectionServices directionServices) {
        this.directionServices = directionServices;
    }

    @Override
    public void run() {
        ServicesTerminal servicesTerminal = ServicesTerminal.getServicesTerminal();
        while (!Thread.currentThread().isInterrupted() && servicesTerminal.getIsBluetoothRunning()) {
            try {
                int availableBytes = servicesTerminal.getBluetoothInputStream().available();
                if (availableBytes > 0) {
                    byte[] rawBytes = new byte[availableBytes];
                    final int i = servicesTerminal.getBluetoothInputStream().read(rawBytes);
                    final String receivedString = new String(rawBytes, StandardCharsets.UTF_8);
                    if (!servicesTerminal.getLatestLocation().equals(receivedString)) {
                        servicesTerminal.addLocationHistory(receivedString);
                        if (servicesTerminal.getLatestLocation().equals(servicesTerminal.peekNodesInPath())) {
                            servicesTerminal.popNodeInPath();
                        }
                        directionServices.getBearingFromDb(servicesTerminal.getLatestLocation(), servicesTerminal.peekNodesInPath(), String.valueOf(servicesTerminal.getCurrentBearing()));
                    }
                }
            } catch (IOException exception) {
                Log.e(TAG, exception.getMessage());
            }
        }
        Log.d(TAG, "Thread have finished running");
    }
}
