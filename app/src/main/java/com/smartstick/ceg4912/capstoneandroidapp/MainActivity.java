package com.smartstick.ceg4912.capstoneandroidapp;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
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

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import static com.smartstick.ceg4912.capstoneandroidapp.MainActivity.RequestCodes.REQUEST_CODE_PERMISSION_SEND_SMS;
import static com.smartstick.ceg4912.capstoneandroidapp.MainActivity.RequestCodes.REQUEST_CODE_TURN_BLUETOOTH_ON;
import static com.smartstick.ceg4912.capstoneandroidapp.VoiceAnalysis.calculateScore;

public class MainActivity extends Activity implements LocationListener {

  private final static String SMART_STICK_URL = "http://SmartWalkingStick-env.irckrevpyt.us-east-1.elasticbeanstalk.com/path";
  private static final UUID BLUETOOTH_PORT_UUID = UUID
      .fromString("00001101-0000-1000-8000-00805f9b34fb");
  private final static String DEVICE_ADDRESS = "98:D3:31:FC:27:5D";
  private final static int REQ_CODE_SPEECH_OUT = 143;
  private static final AtomicBoolean stopThread = new AtomicBoolean();
  private static RequestQueue requestQueue;
  private static boolean isConnectedToBluetooth = false;
  private static String currentLocation = "";
  private static int byteCount = 0;
  private static BluetoothDevice device;
  private static InputStream inputStream;
  private static BluetoothSocket socket;
  private static BluetoothAdapter bluetoothAdapter;
  private static double latitude;
  private static double longtitude;
  private TextToSpeech textToSpeech;
  private String emergencyNumber;

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
    requestQueue = Volley.newRequestQueue(getApplicationContext());
    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    if (bluetoothAdapter == null) {
      logAndSpeak(getString(R.string.device_does_not_support_bluetooth));
      Toast.makeText(this, R.string.device_does_not_support_bluetooth, Toast.LENGTH_LONG).show();
      finish();
    } else {
      if (!bluetoothAdapter.isEnabled()) {
        logAndSpeak(getString(R.string.BLUETOOTH_PERMISSION_IS_NOT_GRANTED_REQUESTING_NOW));
        Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(turnBTon, REQUEST_CODE_TURN_BLUETOOTH_ON);
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
  }

  public void onSync(View v) {
    beginBluetoothConnection();
  }

  public void onVoice(View v) {
    openMic();
  }

  public void onSos(View v) {
    sendEmergencySMS(emergencyNumber);
  }

  /*
  Voice Control
   */
  private void openMic() {
    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    intent
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

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
        Log.d(this.toString(), "Result was OK...whatever that means");
        break;
      }

      case REQ_CODE_SPEECH_OUT: {
        if (data != null) {
          ArrayList<String> voiceInText = data
              .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
          int semanticScore = 0;
          String helloWorld = "hello world";
          for (String voiceAsString : voiceInText) {
            Log.d(this.toString(),
                "The score of " + voiceAsString + " against " + helloWorld + " is " + calculateScore(
                    voiceAsString, helloWorld));
          }
          getDirectionFromDb(currentLocation, voiceInText.get(0));
        } else {
          logAndSpeak(getString(R.string.DATA_WAS_NULL));
        }
        break;
      }
      default: {
        Log.d(this.toString(),
            "Unknown request code...defaulting. The requestCode was " + requestCode);
        break;
      }
    }
  }

  /*
  Bluetooth Connection
   */
  private void beginBluetoothConnection() {
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

  private boolean initializeBluetooth() {
    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    if (bluetoothAdapter == null) {
      logAndSpeak(getString(R.string.ERROR_MESSAGE_DEVICE_DOES_NOT_SUPPORT_BLUETOOTH));
      return false;
    } else {
      if (!bluetoothAdapter.isEnabled()) {
        logAndSpeak(getString(R.string.ERROR_MESSAGE_BLUETOOTH_IS_NOT_ENABLED));
        Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableAdapter, 0);
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

  private boolean connectWithBluetoothSocket() {
    boolean connected = true;
    try {
      socket = device.createRfcommSocketToServiceRecord(BLUETOOTH_PORT_UUID);
      socket.connect();
    } catch (IOException e) {
      connected = false;
      Log.e(this.toString(), e.getMessage());
      logAndSpeak(MessageFormat
          .format(getString(R.string.ERROR_MESSAGE_UNABLE_TO_CONNECT_TO_SOCKET_WITH_PORT_ID),
              BLUETOOTH_PORT_UUID));
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
    final Handler handler = new Handler();
    Thread thread = new Thread(new Runnable() {
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
                    logAndForceSpeak("You have arrived at " + receivedString);
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

  /*
  Database queries
   */
  private void getDirectionFromDb(final String from, final String to) {
    StringRequest jsonObjRequest = new StringRequest(Request.Method.POST, SMART_STICK_URL,
        new Response.Listener<String>() {
          @Override
          public void onResponse(String response) {
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
        Log.d(this.toString(),
            (e == null || e.getMessage() == null) ? "An unexpected error have occured" : e
                .getMessage());
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
    requestQueue.add(jsonObjRequest);
  }

  private void logAndSpeak(String toSpeak) {
    Log.d(this.toString(), toSpeak);
    textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_ADD, null, null);
  }

  private void logAndForceSpeak(String toSpeak) {
    Log.d(this.toString(), toSpeak);
    textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
  }

  /**
   * MINGWEI
   */
  private void sendEmergencySMS(String phoneNumber) {
    LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    if (
        checkSelfPermission(
            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(
            Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED
        ) {
      ActivityCompat.requestPermissions(this,
          new String[]{ Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.SEND_SMS },
          1);
      Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
      onLocationChanged(location);
      String googleMapApiCall = "https://www.google.com/maps/?q=" + latitude + "," + longtitude;
      if (!TextUtils.isEmpty(phoneNumber) && !TextUtils.isEmpty(googleMapApiCall)) {
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNumber, null, googleMapApiCall, null, null);
      } else {
        Log.e(this.toString(), "Permissions are not granted.");
      }
    }

  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         @NonNull String permissions[],
                                         @NonNull int[] grantResults) {
    switch (requestCode) {
      case REQUEST_CODE_TURN_BLUETOOTH_ON: {
        if (grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          Log.d(this.toString(), "Bluetooth have been turned on");
        } else {
          logAndForceSpeak("The application requires Bluetooth connection. Exiting the program!");
          finish();
        }
        break;
      }

      case REQUEST_CODE_PERMISSION_SEND_SMS: {
        if (grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          Log.d(this.toString(), "Bluetooth have been turned on");
        } else {
          Log.d(this.toString(),
              "Permission to send SMS is disabled. This is a very dangerous behavior. Another request to enable this should be made later.");
        }
        break;
      }

      default: {
        Log.e(this.toString(), "Unknwon requestCode:" + requestCode);
        break;
      }
    }
  }

  @Override
  public void onLocationChanged(Location location) {
    if (location != null) {
      latitude = location.getLatitude();
      longtitude = location.getLongitude();
    } else {
      Log.e(this.toString(), "location is null");
    }
  }

  @Override
  public void onProviderDisabled(String provider) {
    Log.d(this.toString(), "Provider have been disabled.");
    Log.d(this.toString(), "provider:" + provider);
  }

  @Override
  public void onProviderEnabled(String provider) {
    Log.d(this.toString(), "Provider have been enabled.");
    Log.d(this.toString(), "provider:" + provider);
  }

  @Override
  public void onStatusChanged(String provider, int status, Bundle extras) {
  }

  static class RequestCodes {
    static final int REQUEST_CODE_TURN_BLUETOOTH_ON = 0;
    static final int REQUEST_CODE_PERMISSION_SEND_SMS = 1;

  }
}