package com.smartstick.ceg4912.capstoneandroidapp;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {

    private final static String SMART_STICK_URL = "http://SmartWalkingStick-env.irckrevpyt.us-east-1.elasticbeanstalk.com/path";
    private final static UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothDevice device;
    private static String bluetoothAddress = null;
    private static BluetoothSocket bluetoothSocket = null;
    private static boolean isBluetoothConnected = false;
    private static RequestQueue requestQueue;
    private static BluetoothAdapter myBluetooth = null;
    private TextView debugTextView;
    private boolean stopThread;
    private byte[] buffer;
    private OutputStream outputStream;
    private InputStream inputStream;
    private final String DEVICE_ADDRESS = "98:D3:31:FC:27:5D";
    private BluetoothSocket socket;
    TextView textView;
    boolean deviceConnected = false;
    Thread thread;
    int bufferPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        debugTextView = findViewById(R.id.debug_textview);
        requestQueue = Volley.newRequestQueue(getApplicationContext());
        myBluetooth = BluetoothAdapter.getDefaultAdapter();

        if (myBluetooth == null) {
            Log.d(this.toString(), "No Bluetooth devices available");
        } else if (!myBluetooth.isEnabled()) {
            Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnBTon, 1);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DisconnectBluetooth();
    }

    private void pairedDevicesList() {
        if (myBluetooth != null) {
            Set<BluetoothDevice> pairedDevices = myBluetooth.getBondedDevices();
            if (pairedDevices.size() > 0) {
                Log.d(this.toString(), "There are " + pairedDevices.size() + " Bluetooth devices");
                for (BluetoothDevice bt : pairedDevices) {
                    Log.d(this.toString(), "bt.bluetoothAddress:" + bt.getAddress());
                    bluetoothAddress = bt.getAddress();
                }
            } else {
                Log.d(this.toString(), "No paired Bluetooth devices found");
            }
        } else {
            Log.d(this.toString(), "No Bluetooth devices available");
        }
    }

    public void onSync(View v) {
        Log.d(this.toString(), "User have clicked onSync");
        StringRequest jsonObjRequest = new StringRequest(Request.Method.POST, SMART_STICK_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(this.toString(), "Response is: " + response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(this.toString(), error.getMessage());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> pars = new HashMap<>();
                pars.put("Content-Type", "application/x-www-form-urlencoded");
                return pars;
            }

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("from", "A");
                params.put("to", "Freedom");
                return params;
            }

        };
        Log.d(this.toString(), "perform request on link:" + jsonObjRequest.getUrl());
        requestQueue.add(jsonObjRequest);
        sendStringToBluetooth(String.valueOf(System.currentTimeMillis()));
        beginRequest();
    }


    public void onVoice(View v) {
        Log.d(this.toString(), "User have clicked onVoice");
        pairedDevicesList();
    }

    private void DisconnectBluetooth() {
        if (bluetoothSocket != null) //If the bluetoothSocket is busy
        {
            try {
                bluetoothSocket.close(); //close connection
            } catch (IOException e) {
                Log.e(this.toString(), e.getMessage());
            }
        }
    }

    private void sendStringToBluetooth(String data) {
        if (bluetoothSocket != null) {
            try {
                debugTextView.setText(data);
                bluetoothSocket.getOutputStream().write(data.getBytes());
            } catch (IOException e) {
                Log.e(this.toString(), e.getMessage());
            }
        }
    }

    private static class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean ConnectSuccess = true;

        @Override
        protected void onPreExecute() {
            Log.d(this.toString(), "Attempting to connect to Bluetooth device...");
        }

        @Override
        protected Void doInBackground(Void... v) {
            if (bluetoothAddress != null) {
                try {
                    if (bluetoothSocket == null || !isBluetoothConnected) {
                        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                        BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(bluetoothAddress);
                        bluetoothSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(myUUID);
                        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                        bluetoothSocket.connect();
                    } else {
                        Log.d(this.toString(), "Attempt to connect failed");
                    }
                } catch (IOException e) {
                    Log.e(this.toString(), e.getMessage());
                    ConnectSuccess = false;
                }
            } else {
                Log.d(this.toString(), "Address is null!");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            super.onPostExecute(v);
            if (!ConnectSuccess) {
                Log.d(this.toString(), "Connection Failed. Is it a SPP Bluetooth? Try again.");
            } else {
                Log.d(this.toString(), "Connected to Bluetooth device!");
                isBluetoothConnected = true;
            }
        }
    }

    public boolean BluetoothConnect() {
        Log.d(this.toString(), "begin connecting to Bluetooth...");
        boolean connected = true;
        try {
            socket = device.createRfcommSocketToServiceRecord(PORT_UUID);
            socket.connect();
        } catch (IOException e) {
            Log.d(this.toString(), e.getMessage());
            connected = false;
        }
        if (connected) {
            try {
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                Log.d(this.toString(), e.getMessage());
            }
            try {
                inputStream = socket.getInputStream();
            } catch (IOException e) {
                Log.d(this.toString(), e.getMessage());
            }

        }
        return connected;
    }

    public boolean BluetoothInit() {
        Log.d(this.toString(), "initializing Bluetooth...");
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.d(this.toString(), "device cannot open Bluetooth...");
            debugTextView.setText(getString(R.string.device_cannot_support_bluetooth));
            return false;
        } else {
            Log.d(this.toString(), "device have opened a Bluetooth...");
            if (!bluetoothAdapter.isEnabled()) {
                Log.d(this.toString(), "Bluetooth is not enabled! Requesting permission to enable now...");
                Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableAdapter, 0);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
            if (bondedDevices.isEmpty()) {
                Log.d(this.toString(), "no connected Bluetooth devices found...");
                debugTextView.setText(getString(R.string.please_pair_device_first));
            } else {
                for (BluetoothDevice iterator : bondedDevices) {
                    Log.d(this.toString(), "found Bluetooth device with address:" + iterator.getAddress());
                    if (iterator.getAddress().equals(DEVICE_ADDRESS)) {
                        device = iterator;
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public void beginRequest() {
        Log.d(this.toString(), "begin request...");
        stopThread = true;
        if (BluetoothInit()) {
            if (BluetoothConnect()) {
                deviceConnected = true;
                beginListenForData();
                debugTextView.append(getString(R.string.connection_opened));
            } else {
                Log.d(this.toString(), "unable to establish connection with Bluetooth device...");
            }

        } else {
            Log.d(this.toString(), "fail to initialize Bluetooth...");
        }
    }

    private void beginListenForData() {
        Log.d(this.toString(), "Begin listening to data...");
        final Handler handler = new Handler();
        stopThread = false;
        buffer = new byte[1024];
        Thread thread = new Thread(new Runnable() {
            public void run() {
                Log.d(this.toString(), "Begin running thread..");
                while (!Thread.currentThread().isInterrupted() && !stopThread) {
                    try {
                        int byteCount = inputStream.available();
                        if (byteCount > 0) {
                            Log.d(this.toString(), "byteCount is larger than 0");
                            byte[] rawBytes = new byte[byteCount];
                            inputStream.read(rawBytes);
                            final String string = new String(rawBytes, "UTF-8");
                            handler.post(new Runnable() {
                                public void run() {
                                    Log.d(this.toString(), "Received the following string:" + string);
                                    debugTextView.setText(string);
                                    stopThread = true;
                                }
                            });

                        }
                    } catch (IOException ex) {
                        Log.d(this.toString(), "error:" + ex.getMessage());
                        stopThread = true;
                    }
                }
            }
        });
        thread.start();
    }
}