package com.smartstick.ceg4912.capstoneandroidapp.services;

import android.util.Log;

import com.smartstick.ceg4912.capstoneandroidapp.MainActivity;
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
    private final MainActivity callerActivity;

    public static void setNodes(List<String> arr) {
        nodesInPath.clear();
        nodesInPath.addAll(arr);
    }

    public RfidServices(MainActivity callerActivity) {
        this.callerActivity = callerActivity;
        BluetoothConnector.initializeBluetooth(callerActivity);
    }

    public static String peekFirst() {
        return nodesInPath.peekFirst();
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
                        final String receivedString = filterBluetooth((new String(rawBytes, StandardCharsets.UTF_8)));
                        if (receivedLength > 1) {
                            Log.d(TAG, "receivedString:" + receivedString);
                            currentLocation = receivedString;
                            if (nodesInPath.isEmpty()) {
                                SpeechServices.addText("Please enter a destination...");
                            } else if (nodesInPath.size() == 1) {
                                SpeechServices.addText("Congratulation, you have arrived at " + nodesInPath.poll());
                            } else {
                                if (currentLocation.equals(decodeNodeNameToId(nodesInPath.peekFirst()))) {
                                    SpeechServices.addText("You have arrived at " + encodeIdToName(receivedString));
                                    nodesInPath.removeFirst();
                                } else {
                                    Log.d(TAG, String.format("currentLocation:%s peekFirst:%s", currentLocation, nodesInPath.peekFirst()));
                                }
                                BearingRequest bearingRequest = new BearingRequest(currentLocation, nodesInPath.peekFirst(), BearingListener.getBearing());
                                RequestServices.addBearingRequest(bearingRequest);
                            }
                        } else {
                            Log.d(TAG, "Delta string recevied => " + receivedString);
                        }
                        callerActivity.TEXT_VIEW_PATH.setText(nodesInPath.toString());
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
    public static String decodeNodeNameToId(String peekNodesInPath) {
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

    public static String encodeIdToName(String peekNodesInPath) {
        switch (peekNodesInPath) {
            case "ADA392FE": {
                return "A";
            }
            case "DDA8387E": {
                return "B";
            }
            case "ED8EA9FE": {
                return "C";
            }
            case "BD2091FE": {
                return "D";
            }
            case "8D97357E": {
                return "E";
            }
            case "BDA13A7E": {
                return "F";
            }
            case "C1D1D909": {
                return "G";
            }
            case "F3A0A775": {
                return "H";
            }
        }
        return null;
    }

    public static String filterBluetooth(String location) {
        return location.length() == 9 ? location.substring(1) : location;
    }

    public static String getCurrentLocation() {
        return currentLocation;
    }
}
