package com.smartstick.ceg4912.capstoneandroidapp.utility;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;

import com.smartstick.ceg4912.capstoneandroidapp.MainActivity;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class BluetoothServices {

    private static final String TAG = "BluetoothServices";
    private boolean isConnectedToBluetooth;
    private static BluetoothDevice device;
    private static BluetoothSocket socket;

    private static final UUID BLUETOOTH_PORT_UUID = UUID
            .fromString("00001101-0000-1000-8000-00805f9b34fb");
    private static final String DEVICE_ADDRESS = "98:D3:31:FC:27:5D";

    public static void initializeBluetooth(Activity callerActivity) {
        checkIfBluetoothCapable(callerActivity);
        beginBluetoothConnection(callerActivity);
    }

    private static void checkIfBluetoothCapable(Activity callerActivity) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.d(TAG, "Phone does not have Bluetooth adapter");
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                callerActivity.startActivityForResult(turnBTon, MainActivity.REQUEST_CODE_TURN_BLUETOOTH_ON);
            }
        }
    }

    private static void beginBluetoothConnection(Activity callerActivity) {
        if (!findArduinoDevice(callerActivity)) {
            Log.d(TAG, "Cannot find Arduino device");
        }
        if (!establishBluetoothInputSocket()) {
            Log.d(TAG, "Cannot establish connection to socket");
        }
    }

    private static boolean findArduinoDevice(Activity callerActivity) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            return false;
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                callerActivity.startActivityForResult(enableAdapter, 0);
            }
            Set<BluetoothDevice> connectedDevices = bluetoothAdapter.getBondedDevices();
            if (connectedDevices.isEmpty()) {
                Log.d(TAG, "No Bluetooth device is connected to the phone");
            } else {
                for (BluetoothDevice iterator : connectedDevices) {
                    if (iterator.getAddress().equals(DEVICE_ADDRESS)) {
                        Log.d(TAG, "Successfully found the required device");
                        device = iterator;
                        return true;
                    }
                }
                Log.d(TAG, "Cannot find the required device");
            }
            return false;
        }
    }

    private static boolean establishBluetoothInputSocket() {
        boolean connected = true;
        try {
            socket = device.createRfcommSocketToServiceRecord(BLUETOOTH_PORT_UUID);
            socket.connect();
        } catch (IOException e) {
            connected = false;
            Log.e(TAG, e.getMessage());
        }
        if (connected) {
            try {
                ServicesTerminal.getServicesTerminal().setBluetoothInputStream(socket.getInputStream());
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        return connected;
    }
}
