package com.smartstick.ceg4912.capstoneandroidapp;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
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
    private static int byteCount = 0;
    private static BluetoothDevice device;
    private static boolean stopThread;
    private static InputStream inputStream;
    private static BluetoothSocket socket;
    private static BluetoothAdapter bluetoothAdapter;
    private TextView debugTextView;
    private TextToSpeech textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.US);
                } else {
                    Log.d(this.toString(), "Failed to initialize textToSpeech with status:" + status);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        debugTextView = findViewById(R.id.debug_textview);
        requestQueue = Volley.newRequestQueue(getApplicationContext());
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.d(this.toString(), "bluetoothAdapter is null");
        }
        if (!bluetoothAdapter.isEnabled()) {
            Log.d(this.toString(), "bluetoothAdapter is not enabled...Requesting permission to enable");
            Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnBTon, 1);
        }

        if (thread == null) {
            Log.d(this.toString(), "thread is null");
        } else if (thread.isAlive()) {
            Log.d(this.toString(), "thread is still alive");
        } else {
            Log.d(this.toString(), "thread have been killed");
        }
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
                    debugTextView.setText(getString(R.string.recevied_the_following_text));
                    for (int voicesIndex = 0; voicesIndex < voiceInText.size(); voicesIndex++) {
                        textToSpeech.speak(voiceInText.get(voicesIndex), TextToSpeech.QUEUE_FLUSH, null, null);
                        Log.d(this.toString(), voicesIndex + ". " + voiceInText.get(voicesIndex));
                        debugTextView.append("\n" + voicesIndex + ". " + voiceInText.get(voicesIndex));
                    }
                }
                break;
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
        if (!isConnectedToBluetooth && initializeBluetooth() && connectWithBluetooth()) {
            isConnectedToBluetooth = true;
            beginListenForData();
        } else {
            Log.d(this.toString(), "Unable to sync with Bluetooth device");
        }
    }

    public boolean initializeBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.d(this.toString(), "bluetoothAdapter is null");
            return false;
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Log.d(this.toString(), "bluetoothAdapter is not enabled");
                Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableAdapter, 0);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Set<BluetoothDevice> connectedDevices = bluetoothAdapter.getBondedDevices();
            if (connectedDevices.isEmpty()) {
                Log.d(this.toString(), "no bluetooth device connected");
            } else {
                for (BluetoothDevice iterator : connectedDevices) {
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
        boolean connected = true;
        try {
            socket = device.createRfcommSocketToServiceRecord(PORT_UUID);
            socket.connect();
        } catch (IOException e) {

            connected = false;
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
        debugTextView.setText(getString(R.string.begin_listening_to_data));
        final Handler handler = new Handler();
        stopThread = false;
        thread = new Thread(new Runnable() {
            public void run() {

                while (!Thread.currentThread().isInterrupted() && !stopThread) {
                    try {
                        byteCount = inputStream.available();
                        if (byteCount > 0) {
                            byte[] rawBytes = new byte[byteCount];
                            inputStream.read(rawBytes);
                            final String receivedString = new String(rawBytes, "UTF-8");
                            handler.post(new Runnable() {
                                public void run() {
                                    if (!oldString.equals(receivedString)) {
                                        oldString = receivedString;
                                        debugTextView.setText(receivedString);
                                    }
                                }
                            });

                        }
                    } catch (IOException ex) {
                        Log.e(this.toString(), ex.getMessage());
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
                        Log.d(this.toString(), "received a response from server:" + response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError e) {
                Log.d(this.toString(), e.getMessage());
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
        debugTextView.setText(MessageFormat.format("performing request on link:{0}", jsonObjRequest.getUrl()));
        requestQueue.add(jsonObjRequest);
        sendStringToBluetooth(String.valueOf(System.currentTimeMillis()));
    }
}