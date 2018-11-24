package com.smartstick.ceg4912.capstoneandroidapp;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {

    private final static String SMART_STICK_URL = "http://SmartWalkingStick-env.irckrevpyt.us-east-1.elasticbeanstalk.com/path";
    private final static UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private final static UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final static String DEVICE_ADDRESS = "98:D3:31:FC:27:5D";
    private BluetoothDevice device;
    private static String bluetoothAddress = null;
    private static BluetoothSocket bluetoothSocket = null;
    private static boolean isBluetoothConnected = false;
    private static RequestQueue requestQueue;
    private static BluetoothAdapter myBluetooth = null;
    private TextView debugTextView;
    private boolean stopThread;
    private InputStream inputStream;
    private BluetoothSocket socket;
    TextView textView;
    boolean deviceConnected = false;
    Thread thread;
    int bufferPosition;
    private final int REQ_CODE_SPEECH_OUT = 143;
    private OutputStream outputStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        debugTextView = findViewById(R.id.debug_textview);
        requestQueue = Volley.newRequestQueue(getApplicationContext());
        myBluetooth = BluetoothAdapter.getDefaultAdapter();

        if (myBluetooth == null) {
            debugTextView.setText(getString(R.string.no_bluetooth_devices_available));
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
        if (myBluetooth == null) {
            debugTextView.setText(getString(R.string.no_bluetooth_devices_available));
        } else {
            Set<BluetoothDevice> pairedDevices = myBluetooth.getBondedDevices();
            if (pairedDevices.size() > 0) {
                debugTextView.setText(MessageFormat.format("There are {0} Bluetooth devices", pairedDevices.size()));
                for (BluetoothDevice bt : pairedDevices) {
                    debugTextView.setText(MessageFormat.format("bt.bluetoothAddress:{0}", bt.getAddress()));
                    bluetoothAddress = bt.getAddress();
                }
            } else {
                debugTextView.setText(getString(R.string.no_paired_bluetooth_devices_found));
            }
        }
    }

    public void onSync(View v) {
        debugTextView.setText(getString(R.string.user_clicked_onsync));
        StringRequest jsonObjRequest = new StringRequest(Request.Method.POST, SMART_STICK_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        debugTextView.setText(MessageFormat.format("Response is: {0}", response));
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                debugTextView.setText(error.getMessage());
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
        debugTextView.setText(MessageFormat.format("perform request on link:{0}", jsonObjRequest.getUrl()));
        requestQueue.add(jsonObjRequest);
        sendStringToBluetooth(String.valueOf(System.currentTimeMillis()));
        beginRequest();
    }


    public void onVoice(View v) {
        debugTextView.setText(getString(R.string.user_clicked_onvoice));
        btnToOpenMic();
    }


    private void btnToOpenMic() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, R.string.please_enter_the_destination);

        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_OUT);
        } catch (ActivityNotFoundException e) {

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_OUT: {
                if (requestCode == RESULT_OK && data != null) {
                    ArrayList<String> voiceInText = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    debugTextView.setText(voiceInText.get(0));
                }
                break;
            }
        }
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

    public boolean BluetoothConnect() {
        debugTextView.setText(getString(R.string.begin_connecting_to_bluetooth));

        boolean connected = true;
        try {
            socket = device.createRfcommSocketToServiceRecord(PORT_UUID);
            socket.connect();
        } catch (IOException e) {
            debugTextView.setText(e.getMessage());
            connected = false;
        }
        if (connected) {
            try {
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                debugTextView.setText(e.getMessage());
            }
            try {
                inputStream = socket.getInputStream();
            } catch (IOException e) {
                debugTextView.setText(e.getMessage());
            }

        }
        return connected;
    }

    public boolean BluetoothInit() {
        debugTextView.setText(getString(R.string.bluetooth_initialize));
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            debugTextView.setText(getString(R.string.device_cannot_open_bluetooth));
            return false;
        } else {
            debugTextView.setText(getString(R.string.device_cannot_open_bluetooth));
            if (!bluetoothAdapter.isEnabled()) {
                debugTextView.setText(getString(R.string.bluetooth_is_not_enabled));
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
                debugTextView.setText(getString(R.string.no_bluetooth_devices_found));
            } else {
                for (BluetoothDevice iterator : bondedDevices) {
                    debugTextView.setText(MessageFormat.format("found Bluetooth device with address:{0}", iterator.getAddress()));
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
        debugTextView.setText(getString(R.string.begin_request));
        stopThread = true;
        if (BluetoothInit()) {
            if (BluetoothConnect()) {
                beginListenForData();
                debugTextView.append(getString(R.string.connection_opened));
            } else {
                debugTextView.setText(getString(R.string.unable_to_establish_connection_with_bluetooth_device));
            }

        } else {
            debugTextView.setText(getString(R.string.fail_to_initialize_bluetooth));
        }
    }

    private void beginListenForData() {
        debugTextView.setText(getString(R.string.begin_listening_to_data));
        final Handler handler = new Handler();
        stopThread = false;
        Thread thread = new Thread(new Runnable() {
            public void run() {
                debugTextView.setText(getString(R.string.begin_running_thread));
                while (!Thread.currentThread().isInterrupted() && !stopThread) {
                    try {
                        int byteCount = inputStream.available();
                        if (byteCount > 0) {
                            byte[] rawBytes = new byte[byteCount];
                            inputStream.read(rawBytes);
                            final String string = new String(rawBytes, "UTF-8");
                            handler.post(new Runnable() {
                                public void run() {
                                    debugTextView.setText(string);
                                    stopThread = true;
                                }
                            });

                        }
                    } catch (IOException ex) {
                        debugTextView.setText(MessageFormat.format("error:{0}", ex.getMessage()));
                        stopThread = true;
                    }
                }
            }
        });
        thread.start();
    }
}