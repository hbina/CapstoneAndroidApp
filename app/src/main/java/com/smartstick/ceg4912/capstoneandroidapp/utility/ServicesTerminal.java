package com.smartstick.ceg4912.capstoneandroidapp.utility;

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

    void setDestinationNode(String destination) {
        destinationNode.add(destination);
    }

    public String getCurrentDestinationNode() {
        return destinationNode.peek();
    }

    void clearPaths() {
        nodesInPath.clear();
    }

    void addNodeToPath(String node) {
        nodesInPath.add(node);
    }

    String peekNodesInPath() {
        return nodesInPath.peek();
    }

    void popNodeInPath() {
        nodesInPath.pop();
    }

    void setBluetoothInputStream(InputStream inputStream) {
        bluetoothInputStream = inputStream;
    }

    InputStream getBluetoothInputStream() {
        return bluetoothInputStream;
    }

    public void setIsBluetoothRunning(boolean bool) {
        isBluetoothRunning.set(bool);
    }

    boolean getIsBluetoothRunning() {
        return isBluetoothRunning.get();
    }

    String getLatestLocation() {
        return this.locationHistory.peek();
    }

    void addLocationHistory(String location) {
        locationHistory.add(location);
    }

    void setCurrentBearing(float bearing) {
        this.currentBearing = bearing;
    }

    float getCurrentBearing() {
        return this.currentBearing;
    }
}
