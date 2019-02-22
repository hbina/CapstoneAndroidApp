package com.smartstick.ceg4912.capstoneandroidapp.utility;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class BluetoothServices {

    private boolean isConnectedToBluetooth;
    private BluetoothAdapter bluetoothAdapter;
    private Activity callerActivity;
    private BluetoothDevice device;
    private InputStream inputStream;
    private BluetoothSocket socket;
    private AtomicBoolean stopThread;
    private int byteCount;
    private Stack<String> locationHistory;

    private static final UUID BLUETOOTH_PORT_UUID = UUID
            .fromString("00001101-0000-1000-8000-00805f9b34fb");
    private static final String DEVICE_ADDRESS = "98:D3:31:FC:27:5D";

    public BluetoothServices(Activity callerActivity) {
        this.isConnectedToBluetooth = false;
        this.bluetoothAdapter = null;
        this.callerActivity = callerActivity;
        this.device = null;
        this.inputStream = null;
        this.socket = null;
        this.stopThread = new AtomicBoolean();
        this.byteCount = 0;
    }

    public void init(int returnCode) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.d(this.toString(), "Phone does not have Bluetooth adapter");
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                callerActivity.startActivityForResult(turnBTon, returnCode);
            }
        }
    }

    public void beginBluetoothConnection() {
        if (!isConnectedToBluetooth && findArduinoDevice() && establishBluetoothInputSocket()) {
            isConnectedToBluetooth = true;
            createListeningThread();
        }
    }

    private boolean findArduinoDevice() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            return false;
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                callerActivity.startActivityForResult(enableAdapter, 0);
            }
            Set<BluetoothDevice> connectedDevices = bluetoothAdapter.getBondedDevices();
            if (connectedDevices.isEmpty()) {
                Log.d(this.toString(), "No Bluetooth device is connected to the phone");
            } else {
                for (BluetoothDevice iterator : connectedDevices) {
                    if (iterator.getAddress().equals(DEVICE_ADDRESS)) {
                        Log.d(this.toString(), "Successfully found the required device");
                        device = iterator;
                        return true;
                    }
                }
                Log.d(this.toString(), "Cannot find the required device");
            }
            return false;
        }
    }

    private boolean establishBluetoothInputSocket() {
        boolean connected = true;
        try {
            socket = device.createRfcommSocketToServiceRecord(BLUETOOTH_PORT_UUID);
            socket.connect();
        } catch (IOException e) {
            connected = false;
            Log.e(this.toString(), e.getMessage());
        }
        if (connected) {
            try {
                inputStream = socket.getInputStream();
            } catch (IOException e) {
                Log.e(this.toString(), e.getMessage());
            }

        }
        return connected;
    }

    private void createListeningThread() {
        final Handler handler = new Handler();
        Thread thread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted() && !stopThread.get()) {
                    try {
                        byteCount = inputStream.available();
                        if (byteCount > 0) {
                            byte[] rawBytes = new byte[byteCount];
                            final int read = inputStream.read(rawBytes);
                            final String receivedString = new String(rawBytes, StandardCharsets.UTF_8);
                            handler.post(new Runnable() {
                                public void run() {
                                    // TODO: Additional check of current time
                                    if (!locationHistory.peek().equals(receivedString)) {
                                        locationHistory.push(receivedString);
                                    }
                                }
                            });
                        }
                    } catch (IOException e) {
                        Log.e(this.toString(), e.getMessage());
                    }
                }
                Log.d(this.toString(), "Thread have finished running");
            }
        });
        thread.start();
    }

    public void killBluetooth() {
        // TODO: Kill listening thread, preferably when user have arrived at the destination
        stopThread.set(false);
    }

    public String getCurrentLocation() {
        return this.locationHistory.peek();
    }
}
