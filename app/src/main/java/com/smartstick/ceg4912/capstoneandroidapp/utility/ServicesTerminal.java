package com.smartstick.ceg4912.capstoneandroidapp.utility;

import android.app.Activity;
import android.content.Context;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

// TODO: Make this database thread safe...or use Room Database in the future?
public class ServicesTerminal {

    private Stack<String> nodesInPath;
    private Stack<String> destinationNode; // TODO: Write to DB over some regular intervals
    private InputStream bluetoothInputStream;
    private AtomicBoolean isBluetoothRunning;
    private Stack<String> locationHistory; // TODO: Write to DB over some regular intervals
    private float currentBearing;
    private Activity callerActivity;

    private ServicesTerminal() {
        this.nodesInPath = new Stack<>();
        this.locationHistory = new Stack<>();
        this.destinationNode = new Stack<>();
        this.isBluetoothRunning = new AtomicBoolean(false);
    }

    private static ServicesTerminal servicesTerminal = null;

    public static ServicesTerminal getServicesTerminal() {
        if (servicesTerminal == null) {
            servicesTerminal = new ServicesTerminal();
        }
        return servicesTerminal;
    }

    public void setDestinationNode(String destination) {
        destinationNode.add(destination);
    }

    public String getCurrentDestinationNode() {
        return destinationNode.peek();
    }

    public void clearPaths() {
        nodesInPath.clear();
    }

    public void addNodeToPath(String node) {
        nodesInPath.add(node);
    }

    public String peekNodesInPath() {
        return nodesInPath.peek();
    }

    public void popNodeInPath() {
        nodesInPath.pop();
    }

    public void setBluetoothInputStream(InputStream inputStream) {
        bluetoothInputStream = inputStream;
    }

    public InputStream getBluetoothInputStream() {
        return bluetoothInputStream;
    }

    public void setIsBluetoothRunning(boolean bool) {
        isBluetoothRunning.set(bool);
    }

    public boolean getIsBluetoothRunning() {
        return isBluetoothRunning.get();
    }

    public String getLatestLocation() {
        return this.locationHistory.peek();
    }

    public void addLocationHistory(String location) {
        locationHistory.add(location);
    }

    public void setCurrentBearing(float bearing) {
        this.currentBearing = bearing;
    }

    public float getCurrentBearing() {
        return this.currentBearing;
    }

    public boolean isLocationHistoryEmpty() {
        return locationHistory.isEmpty();
    }

    public boolean isPathNodesEmpty() {
        return nodesInPath.isEmpty();
    }

    public boolean isInputStreamProvided() {
        return bluetoothInputStream == null;
    }

    public Activity getCallerActivity() {
        return callerActivity;
    }

    public void setCallerActivity(Activity callerActivity) {
        this.callerActivity = callerActivity;
    }
}
