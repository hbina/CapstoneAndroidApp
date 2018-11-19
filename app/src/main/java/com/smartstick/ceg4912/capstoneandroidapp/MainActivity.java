package com.smartstick.ceg4912.capstoneandroidapp;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {

    private final static String SMART_STICK_URL = "http://SmartWalkingStick-env.irckrevpyt.us-east-1.elasticbeanstalk.com/path";
    private static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static String bluetoothAddress = null;
    private static BluetoothSocket bluetoothSocket = null;
    private static boolean isBluetoothConnected = false;
    private static RequestQueue requestQueue;
    private static BluetoothAdapter myBluetooth = null;
    private static TextView debugTextView;

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
        new ConnectBT().execute();
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
}