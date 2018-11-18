package com.smartstick.ceg4912.capstoneandroidapp;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {

    private final static String SMART_STICK_URL = "http://SmartWalkingStick-env.irckrevpyt.us-east-1.elasticbeanstalk.com/path";
    private TextView debugTextView;
    private RequestQueue requestQueue;
    private BluetoothAdapter myBluetooth = null;

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

    private void pairedDevicesList() {
        int counter = 0;
        if (myBluetooth != null) {
            Set<BluetoothDevice> pairedDevices = myBluetooth.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice bt : pairedDevices) {
                    Log.d(this.toString(), bt.toString());
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
                        debugTextView.setText("Response is: " + response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                debugTextView.setText("" + error.getMessage());
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
        Log.d(this.toString(), jsonObjRequest.toString());
        requestQueue.add(jsonObjRequest);
    }


    public void onVoice(View v) {
        Log.d(this.toString(), "User have clicked onVoice");
        pairedDevicesList();
    }
}