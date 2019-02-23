package com.smartstick.ceg4912.capstoneandroidapp;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.smartstick.ceg4912.capstoneandroidapp.utility.BluetoothServices;
import com.smartstick.ceg4912.capstoneandroidapp.utility.DirectionServices;
import com.smartstick.ceg4912.capstoneandroidapp.utility.VoiceCommandServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import static com.smartstick.ceg4912.capstoneandroidapp.MainActivity.RequestCodes.REQUEST_CODE_PERMISSION_SEND_SMS;
import static com.smartstick.ceg4912.capstoneandroidapp.MainActivity.RequestCodes.REQUEST_CODE_TURN_BLUETOOTH_ON;

public class MainActivity extends Activity implements LocationListener {
    private final static int REQ_CODE_SPEECH_OUT = 143;

    private static double latitude;
    private static double longtitude;
    private TextToSpeech textToSpeech;
    private String emergencyNumber;
    private BluetoothServices bluetoothServices;
    private DirectionServices directionServices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializedTextToSpeech();
        bluetoothServices = new BluetoothServices(this);
        bluetoothServices.init(REQUEST_CODE_TURN_BLUETOOTH_ON);

        directionServices = new DirectionServices(this);
    }

    private void initializedTextToSpeech() {
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
    public void onDestroy() {
        super.onDestroy();
        bluetoothServices.killBluetooth();
        directionServices.cancelAll();
    }


    public void onSync(View v) {
        Log.d(this.toString(), "User pressed onSync()");
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
                    ArrayList<String> paths = directionServices.getDirectionFromDb(bluetoothServices.getCurrentLocation(), VoiceCommandServices.evaluate(data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)));
                    for (String arr : paths) {
                        Log.d(this.toString(), arr);
                    }
                } else {
                    Log.d(this.toString(), "Data is null");
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
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.SEND_SMS},
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
                    bluetoothServices.beginBluetoothConnection();
                } else {
                    Log.d(this.toString(), "Bluetooth permission have not been granted. Application will not function properly.");
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