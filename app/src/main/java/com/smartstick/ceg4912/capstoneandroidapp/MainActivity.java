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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
    private static final UUID BLUETOOTH_PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private final static String DEVICE_ADDRESS = "98:D3:31:FC:27:5D";
    private final static int REQ_CODE_SPEECH_OUT = 143;
    private static boolean expectingFirstCharacter = true;
    private static BluetoothSocket bluetoothSocket = null;
    private static RequestQueue requestQueue;
    private static boolean isConnectedToBluetooth = false;
    private static String currentLocation = "";
    private static Thread thread;
    private static int byteCount = 0;
    private static BluetoothDevice device;
    private static AtomicBoolean stopThread = new AtomicBoolean();
    private static InputStream inputStream;
    private static BluetoothSocket socket;
    private static BluetoothAdapter bluetoothAdapter;
    private TextToSpeech textToSpeech;
    private TextView debugTextView;

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
            logAndSpeak(getString(R.string.device_does_not_support_bluetooth));
            Toast.makeText(getApplicationContext(), getString(R.string.device_does_not_support_bluetooth), Toast.LENGTH_LONG).show();
            finish();
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                logAndSpeak(getString(R.string.BLUETOOTH_PERMISSION_IS_NOT_GRANTED_REQUESTING_NOW));
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
        boolean granted = Utility.checkAndRequestPermission(this);
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
        logAndSpeak(getString(R.string.YOU_HAVE_PRESSED_THE_SYNC_BUTTON));
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
                    logAndSpeak("The following options are possible:");
                    for (int voicesIndex = 0; voicesIndex < voiceInText.size(); voicesIndex++) {
                        logAndSpeak((voicesIndex + 1) + ". " + voiceInText.get(voicesIndex));
                    }
                    getDirectionFromDb(currentLocation, voiceInText.get(0));
                } else {
                    logAndSpeak(getString(R.string.DATA_WAS_NULL));
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
                logAndSpeak(getString(R.string.CONNECTION_TO_BLUETOOTH_DEVICE_SUCCESSFULL));
                beginListenForData();
            }
        } else {
            logAndSpeak(getString(R.string.ERROR_MESSAGE_DEVICE_ALREADY_CONNECTED_TO_BLUETOOTH));
        }
    }

    public boolean initializeBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            logAndSpeak(getString(R.string.ERROR_MESSAGE_DEVICE_DOES_NOT_SUPPORT_BLUETOOTH));
            return false;
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                logAndSpeak(getString(R.string.ERROR_MESSAGE_BLUETOOTH_IS_NOT_ENABLED));
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
                logAndSpeak(getString(R.string.ERROR_MESSAGE_NO_BLUETOOTH_DEVICE_CONNECTED));
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
            socket = device.createRfcommSocketToServiceRecord(BLUETOOTH_PORT_UUID);
            socket.connect();
        } catch (IOException e) {
            connected = false;
            Log.e(this.toString(), e.getMessage());
            logAndSpeak(MessageFormat.format(getString(R.string.ERROR_MESSAGE_UNABLE_TO_CONNECT_TO_SOCKET_WITH_PORT_ID), BLUETOOTH_PORT_UUID));
        }
        if (connected) {
            try {
                inputStream = socket.getInputStream();
            } catch (IOException e) {
                Log.e(this.toString(), e.getMessage());
                logAndSpeak(getString(R.string.ERROR_MESSAGE_UNABLE_GET_BLUETOOTH_SOCKET_INPUT_STREAM));
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
                                    if (!currentLocation.equals(receivedString)) {
                                        currentLocation = receivedString;
                                        Log.d(this.toString(), "rawBytesReturnInt:" + read);
                                        debugTextView.setText(receivedString);
                                        logAndSpeak("You have arrived at " + receivedString, TextToSpeech.QUEUE_FLUSH);
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
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
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
                        //logAndSpeak(response);
                        try {
                            JSONObject reader = new JSONObject(response);
                            JSONArray contacts = reader.getJSONArray("Path");
                            logAndSpeak("there are " + contacts.length() + " nodes you have to visit. They are:");
                            for (int i = 0; i < contacts.length(); i++) {
                                logAndSpeak(contacts.getString(i));
                                if (i != contacts.length() - 1) {
                                    logAndSpeak("then");
                                }
                            }
                        } catch (JSONException e) {
                            Log.e(this.toString(), e.getMessage());
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError e) {
                Log.d(this.toString(), (e == null || e.getMessage() == null) ? "An unexpected error have occured" : e.getMessage());
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

    private void logAndSpeak(String toSpeak) {
        Log.d(this.toString(), toSpeak);
        textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_ADD, null, null);
    }

    private void logAndSpeak(String toSpeak, int queueMode) {
        Log.d(this.toString(), toSpeak);
        textToSpeech.speak(toSpeak, queueMode, null, null);
    }
}