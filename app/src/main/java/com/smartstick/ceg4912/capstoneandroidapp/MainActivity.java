package com.smartstick.ceg4912.capstoneandroidapp;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ActivityNotFoundException;
import android.content.Intent;
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
    private final static String DEVICE_ADDRESS = "98:D3:31:FC:27:5D";
    private final static int REQ_CODE_SPEECH_OUT = 143;
    private static BluetoothSocket bluetoothSocket = null;
    private static RequestQueue requestQueue;
    private static boolean isConnectedToBluetooth = false;
    private static String oldString = "";
    private static Thread thread;
    private static int rawBytesLength = 0;
    private static int byteCount = 0;
    private static BluetoothDevice device;
    private static boolean stopThread;
    private static InputStream inputStream;
    private static BluetoothSocket socket;
    private TextView debugTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onResume() {
        super.onResume();
        debugTextView = findViewById(R.id.debug_textview);
        requestQueue = Volley.newRequestQueue(getApplicationContext());
        BluetoothAdapter myBluetooth = BluetoothAdapter.getDefaultAdapter();

        if (myBluetooth == null) {
            debugTextView.setText(getString(R.string.no_bluetooth_devices_available));
        } else if (!myBluetooth.isEnabled()) {
            Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnBTon, 1);
        }
        debugTextView.setText(R.string.resumed_from);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DisconnectBluetooth();
    }

    public void onSync(View v) {
        debugTextView.setText(getString(R.string.user_clicked_onsync));
        beginBluetoothSync();
    }


    public void onVoice(View v) {
        debugTextView.setText(getString(R.string.user_clicked_onvoice));
        btnToOpenMic();
    }

    /*
    Voice Control
     */
    private void btnToOpenMic() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, R.string.please_enter_the_destination);

        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_OUT);
        } catch (ActivityNotFoundException e) {
            Log.d(this.toString(), e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RESULT_OK: {
                Log.d(this.toString(), "RESULT_OK");
                break;
            }

            case REQ_CODE_SPEECH_OUT: {
                Log.d(this.toString(), "REQ_CODE_SPEECH_OUT");
                if (data != null) {
                    ArrayList<String> voiceInText = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    debugTextView.setText(voiceInText.get(0));
                    break;
                }
            }
            default: {
                Log.d(this.toString(), "requestCode:" + requestCode);
                break;
            }
        }
    }

    /*
    Bluetooth Connection
     */
    public void beginBluetoothSync() {
        debugTextView.setText(getString(R.string.begin_request));
        if (!isConnectedToBluetooth) {
            if (initializeBluetooth()) {
                if (connectWithBluetooth()) {
                    isConnectedToBluetooth = true;
                    beginListenForData();
                    debugTextView.append(getString(R.string.connection_opened));
                } else {
                    debugTextView.setText(getString(R.string.unable_to_establish_connection_with_bluetooth_device));
                }
            } else {
                debugTextView.setText(getString(R.string.fail_to_initialize_bluetooth));
            }
        } else {
            debugTextView.setText(R.string.already_connected);
        }
    }

    public boolean initializeBluetooth() {
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

    public boolean connectWithBluetooth() {
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
                inputStream = socket.getInputStream();
            } catch (IOException e) {
                debugTextView.setText(e.getMessage());
            }

        }
        return connected;
    }

    private void beginListenForData() {
        debugTextView.setText(getString(R.string.begin_listening_to_data));
        final Handler handler = new Handler();
        stopThread = false;
        thread = new Thread(new Runnable() {
            public void run() {
                debugTextView.setText(getString(R.string.begin_running_thread));
                while (!Thread.currentThread().isInterrupted() && !stopThread) {
                    try {
                        byteCount = inputStream.available();
                        if (byteCount > 0) {
                            byte[] rawBytes = new byte[byteCount];
                            rawBytesLength = inputStream.read(rawBytes);
                            final String string = new String(rawBytes, "UTF-8");
                            handler.post(new Runnable() {
                                public void run() {
                                    if (!oldString.equals(string)) {
                                        debugTextView.setText(MessageFormat.format("received string of byteCount:{0}\nrawBytesLength:{1}\ncontaining:{2}", byteCount, rawBytesLength, string));
                                        oldString = string;
                                    }
                                }
                            });

                        }
                    } catch (IOException ex) {
                        debugTextView.setText(MessageFormat.format("error:{0}", ex.getMessage()));
                    }
                }
            }
        });
        thread.start();
    }

    private void DisconnectBluetooth() {
        isConnectedToBluetooth = false;
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

    /*
    Database queries
     */
    private void getDirectionFromDb() {
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
    }
}