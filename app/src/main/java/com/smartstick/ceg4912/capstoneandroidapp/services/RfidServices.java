package com.smartstick.ceg4912.capstoneandroidapp.services;

import android.util.Log;
import android.widget.TextView;

import com.smartstick.ceg4912.capstoneandroidapp.MainActivity;
import com.smartstick.ceg4912.capstoneandroidapp.R;
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

    static void setNodes(List<String> nodes) {
        Log.d(TAG, "nodes:" + nodes.toString());
        nodesInPath.clear();
        nodesInPath.addAll(nodes);
    }

    public RfidServices(MainActivity callerActivity) {
        this.callerActivity = callerActivity;
        BluetoothConnector.initializeBluetooth(callerActivity);
    }

    static String peekFirst() {
        return nodesInPath.peekFirst();
    }

    @Override
    public void run() {
        while (isRunning.get()) {
            if (BluetoothConnector.getInputStream() != null) {
                String receivedString = getBluetoothData();
                if (receivedString != null) {
                    Log.d(TAG, "receivedStrng:" + receivedString + " length:" + receivedString.length());
                    if (receivedString.length() == 10) {
                        receivedString = receivedString.substring(2);
                        Log.d(TAG, "after receivedStrng:" + receivedString);
                    }
                    updateCurrentLocation(receivedString);
                    handleReceivedString(receivedString);
                }
            } else {
                isRunning.set(false);
            }
        }
        Log.d(TAG, "Thread have finished running");
    }

    private void updateTextView() {
        ((TextView) callerActivity.findViewById(R.id.di_content_path)).setText(nodesInPath.toString());
        ((TextView) callerActivity.findViewById(R.id.di_current_path)).setText(currentLocation);
    }

    private void handleReceivedString(String receivedString) {
        if (nodesInPath.isEmpty()) {
            SpeechServices.addText("Please enter a destination...");
            return;
        }
        if (nodesInPath.size() == 1) {
            SpeechServices.addText("Congratulation, you have arrived at " + nodesInPath.poll());
        } else {
            if (currentLocation.equals(decodeNodeNameToId(nodesInPath.peekFirst()))) {
                SpeechServices.addText("You have arrived at " + encodeIdToName(receivedString));
                nodesInPath.removeFirst();
            } else {
                Log.d(TAG, String.format("currentLocation:%s peekFirst:%s", currentLocation, nodesInPath.peekFirst()));
            }
            BearingRequest bearingRequest = new BearingRequest(currentLocation, nodesInPath.peekFirst(), String.valueOf(BearingListener.getBearing()));
            RequestServices.addBearingRequest(bearingRequest);
        }
        callerActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateTextView();
            }
        });
    }

    private void updateCurrentLocation(String receivedString) {
        currentLocation = receivedString;
    }

    private String getBluetoothData() {
        try {
            int availableBytes = BluetoothConnector.getInputStream().available();
            if (availableBytes > 7) {
                byte[] rawBytes = new byte[availableBytes];
                final int receivedLength = BluetoothConnector.getInputStream().read(rawBytes);
                final String receivedString = filterBluetooth((new String(rawBytes, StandardCharsets.UTF_8)));
                if (receivedLength > 1) {
                    return receivedString;
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (IOException exception) {
            Log.e(TAG, exception.toString());
        }
        return null;
    }

    // TODO: Rework this messy crud
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
            case "H2": {
                return "49D74C9A";
            }
            case "I": {
                return "43D1BD75";
            }
            case "J": {
                return "B4B01FA3";
            }
            case "K": {
                return "F0F875A4";
            }
        }
        return null;
    }

    static String encodeIdToName(String peekNodesInPath) {
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
            case "49D74C9A": {
                return "H2";
            }
            case "43D1BD75": {
                return "I";
            }
            case "B4B01FA3": {
                return "J";
            }
            case "F0F875A4": {
                return "K";
            }
        }
        return null;
    }

    static String filterBluetooth(String location) {
        return location.length() == 9 ? location.substring(1) : location;
    }

    public static String getCurrentLocation() {
        return currentLocation;
    }
}
