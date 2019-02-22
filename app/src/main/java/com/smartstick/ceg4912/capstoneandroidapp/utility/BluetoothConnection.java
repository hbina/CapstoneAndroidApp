package com.smartstick.ceg4912.capstoneandroidapp.utility;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.smartstick.ceg4912.capstoneandroidapp.R;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class BluetoothConnection {

    private final LocalDatabase localDatabase;
    private boolean isConnectedToBluetooth;
    private BluetoothAdapter bluetoothAdapter;
    private Activity callerActivity;
    private BluetoothDevice device;
    private InputStream inputStream;
    private BluetoothSocket socket;
    private AtomicBoolean stopThread;
    private static String currentLocation = "";
    private int byteCount;
    private static final UUID BLUETOOTH_PORT_UUID = UUID
            .fromString("00001101-0000-1000-8000-00805f9b34fb");
    private static final String DEVICE_ADDRESS = "98:D3:31:FC:27:5D";

    public BluetoothConnection(Activity callerActivity, LocalDatabase localDatabase) {
        this.localDatabase = localDatabase;
        this.isConnectedToBluetooth = false;
        this.bluetoothAdapter = null;
        this.callerActivity = callerActivity;
        this.device = null;
        this.inputStream = null;
        this.socket = null;
        this.stopThread = new AtomicBoolean();
        this.byteCount = 0;
    }

    /*
    Bluetooth Connection
     */
    public boolean beginBluetoothConnection() {
        if (!isConnectedToBluetooth && initializeBluetooth() && connectWithBluetoothSocket()) {
            isConnectedToBluetooth = true;
            beginListenForData();
            return true;
        }
        Log.d(this.toString(), "Device already connected to Bluetooth");
        return false;
    }

    private boolean initializeBluetooth() {
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

    private boolean connectWithBluetoothSocket() {
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

    private void beginListenForData() {
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
                                    if (!localDatabase.getUserCurrentLocation().equals(receivedString)) {
                                        localDatabase.setCurrentLocation(receivedString);
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
}
