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
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.concurrent.atomic.AtomicBoolean;

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
    private static AtomicBoolean stopThread;
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
                    String errorStatus = "Failed to initialize textToSpeech with status:" + status;
                    Log.d(this.toString(), errorStatus);
                    textToSpeech.speak(errorStatus, TextToSpeech.QUEUE_ADD, null, null);
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
            Toast.makeText(getApplicationContext(), getString(R.string.device_does_not_support_bluetooth), Toast.LENGTH_LONG).show();
            finish();
        } else {
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
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (requestQueue != null) {
            requestQueue.cancelAll(this.toString());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DisconnectBluetooth();
        stopThread.set(false);
    }

    public void onSync(View v) {
        debugTextView.setText(getString(R.string.user_clicked_onsync));
        beginBluetoothConnection();
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
                        textToSpeech.speak(voiceInText.get(voicesIndex), TextToSpeech.QUEUE_ADD, null, null);
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
    public void beginBluetoothConnection() {
        if (!isConnectedToBluetooth) {
            if (initializeBluetooth() && connectWithBluetoothSocket()) {
                isConnectedToBluetooth = true;
                beginListenForData();
            }
        } else {
            Log.d(this.toString(), getString(R.string.ERROR_MESSAGE_DEVICE_ALREADY_CONNECTED_TO_BLUETOOTH));
            textToSpeech.speak(getString(R.string.ERROR_MESSAGE_DEVICE_ALREADY_CONNECTED_TO_BLUETOOTH), TextToSpeech.QUEUE_ADD, null, null);
        }
    }

    public boolean initializeBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.d(this.toString(), getString(R.string.ERROR_MESSAGE_DEVICE_DOES_NOT_SUPPORT_BLUETOOTH));
            textToSpeech.speak(getString(R.string.ERROR_MESSAGE_DEVICE_DOES_NOT_SUPPORT_BLUETOOTH), TextToSpeech.QUEUE_ADD, null, null);
            return false;
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Log.d(this.toString(), getString(R.string.ERROR_MESSAGE_BLUETOOTH_IS_NOT_ENABLED));
                textToSpeech.speak(getString(R.string.ERROR_MESSAGE_BLUETOOTH_IS_NOT_ENABLED), TextToSpeech.QUEUE_ADD, null, null);
                Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableAdapter, 0);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.d(this.toString(), e.getMessage());
                }
            }
            Set<BluetoothDevice> connectedDevices = bluetoothAdapter.getBondedDevices();
            if (connectedDevices.isEmpty()) {
                Log.d(this.toString(), getString(R.string.ERROR_MESSAGE_NO_BLUETOOTH_DEVICE_CONNECTED));
                textToSpeech.speak(getString(R.string.ERROR_MESSAGE_NO_BLUETOOTH_DEVICE_CONNECTED), TextToSpeech.QUEUE_ADD, null, null);
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

    public boolean connectWithBluetoothSocket() {
        boolean connected = true;
        try {
            socket = device.createRfcommSocketToServiceRecord(PORT_UUID);
            socket.connect();
        } catch (IOException e) {
            connected = false;
            Log.e(this.toString(), e.getMessage());
            textToSpeech.speak(MessageFormat.format(getString(R.string.ERROR_MESSAGE_UNABLE_TO_CONNECT_TO_SOCKET_WITH_PORT_ID), PORT_UUID), TextToSpeech.QUEUE_ADD, null, null);
        }
        if (connected) {
            try {
                inputStream = socket.getInputStream();
            } catch (IOException e) {
                Log.e(this.toString(), e.getMessage());
                textToSpeech.speak(getString(R.string.ERROR_MESSAGE_UNABLE_GET_BLUETOOTH_SOCKET_INPUT_STREAM), TextToSpeech.QUEUE_ADD, null, null);
            }

        }
        return connected;
    }

    private void beginListenForData() {
        debugTextView.setText(getString(R.string.begin_listening_to_data));
        final Handler handler = new Handler();
        stopThread.set(false);
        thread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted() && !stopThread.get()) {
                    try {
                        byteCount = inputStream.available();
                        if (byteCount > 0) {
                            byte[] rawBytes = new byte[byteCount];
                            final int read = inputStream.read(rawBytes);
                            final String receivedString = new String(rawBytes, "UTF-8");
                            handler.post(new Runnable() {
                                public void run() {
                                    if (!oldString.equals(receivedString)) {
                                        oldString = receivedString;
                                        Log.d(this.toString(), "rawBytesReturnInt:" + read);
                                        debugTextView.setText(receivedString);
                                        textToSpeech.speak("You have arrived at " + receivedString, TextToSpeech.QUEUE_ADD, null, null);
                                    }
                                }
                            });

                        }
                    } catch (IOException e) {
                        Log.e(this.toString(), e.getMessage());
                    }
                }
                Log.d(this.toString(), "thread have finished running");
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
    private void getDirectionFromDb(final String from, final String to) {
        StringRequest jsonObjRequest = new StringRequest(Request.Method.POST, SMART_STICK_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(this.toString(), "received a response from server:" + response);
                        textToSpeech.speak(response, TextToSpeech.QUEUE_ADD, null, null);
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
                params.put(getString(R.string.http_request_api_param_from), from);
                params.put(getString(R.string.http_request_api_param_to), to);
                return params;
            }

        };
        debugTextView.setText(MessageFormat.format("performing request on link:{0}", jsonObjRequest.getUrl()));
        requestQueue.add(jsonObjRequest);
        sendStringToBluetooth(String.valueOf(System.currentTimeMillis()));
    }
}