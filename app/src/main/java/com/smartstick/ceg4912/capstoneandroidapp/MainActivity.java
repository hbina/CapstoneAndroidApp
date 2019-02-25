package com.smartstick.ceg4912.capstoneandroidapp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;

import com.smartstick.ceg4912.capstoneandroidapp.utility.BearingServices;
import com.smartstick.ceg4912.capstoneandroidapp.utility.BluetoothServices;
import com.smartstick.ceg4912.capstoneandroidapp.utility.DirectionServices;
import com.smartstick.ceg4912.capstoneandroidapp.utility.EmergencyServices;
import com.smartstick.ceg4912.capstoneandroidapp.utility.ListeningForBluetoothThread;
import com.smartstick.ceg4912.capstoneandroidapp.utility.ServicesTerminal;
import com.smartstick.ceg4912.capstoneandroidapp.utility.TextToSpeechServices;
import com.smartstick.ceg4912.capstoneandroidapp.utility.VoiceCommandServices;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

public class MainActivity extends Activity {

    public static final int REQUEST_CODE_TURN_BLUETOOTH_ON = 0;
    public static final int REQUEST_CODE_GRANT_EMERGENCY_PERMISSIONS = 1;

    private enum LISTENING_STATE {
        LISTENING_FOR_COMMANDS,
        LISTENING_FOR_NEW_DIRECTION,
        LISTENING_FOR_EMERGENCY_NUMBER
    }

    private static final String TAG = "MainActivity";
    private LISTENING_STATE current_listening_state = LISTENING_STATE.LISTENING_FOR_COMMANDS;
    private final static int REQ_CODE_SPEECH_OUT = 0;
    private TextToSpeechServices textToSpeechServices;
    private DirectionServices directionServices;
    private VoiceCommandServices voiceCommandServices;
    private EmergencyServices emergencyServices;
    private BearingServices bearingServices;
    private ListeningForBluetoothThread listeningForBluetoothThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BluetoothServices.initializeBluetooth(this);
        textToSpeechServices = new TextToSpeechServices(this);
        directionServices = new DirectionServices(this, this.textToSpeechServices);
        voiceCommandServices = new VoiceCommandServices(this);
        emergencyServices = new EmergencyServices(this);
        bearingServices = new BearingServices(this);
        listeningForBluetoothThread = new ListeningForBluetoothThread(directionServices);
        listeningForBluetoothThread.start();
        Log.d(TAG, "Executed thread");
    }

    @Override
    public void onResume() {
        super.onResume();
        // TODO: Perform permission check....if not satisfied, logAndSpeak and request for permission...for every resume.
        if (checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission to access fine location is not granted");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);

        }
        if (checkSelfPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission to access coarse location is not granted");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    2);
        }
        if (checkSelfPermission(
                Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission to send SMS");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    3);
        }
        if (checkSelfPermission(
                Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission to use Bluetooth is not granted");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH},
                    4);
        }
        bearingServices.registerListener();
    }

    @Override
    public void onPause() {
        super.onPause();
        bearingServices.unregisterListener();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ServicesTerminal.getServicesTerminal().setIsBluetoothRunning(false);
        directionServices.cancelAll();
    }

    public void onVoice(View v) {
        voiceCommandServices.openMic(REQ_CODE_SPEECH_OUT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQ_CODE_SPEECH_OUT: {
                if (data != null) {
                    ArrayList<String> generatedStrings = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    Log.d(TAG, String.format("generatedString:%s", generatedStrings.toString()));
                    switch (current_listening_state) {
                        case LISTENING_FOR_COMMANDS: {
                            Log.d(TAG, "Listening for commands");
                            int command = voiceCommandServices.evaluateCommands(generatedStrings);
                            switch (command) {
                                case 0: {
                                    current_listening_state = LISTENING_STATE.LISTENING_FOR_NEW_DIRECTION;
                                    voiceCommandServices.openMic(REQ_CODE_SPEECH_OUT);
                                    break;
                                }
                                case 1: {
                                    current_listening_state = LISTENING_STATE.LISTENING_FOR_EMERGENCY_NUMBER;
                                    voiceCommandServices.openMic(REQ_CODE_SPEECH_OUT);
                                    break;
                                }
                                case 2: {
                                    emergencyServices.sendEmergencySMS();
                                    break;
                                }
                                case 3: {
                                    // TODO : Vibrate stuff
                                    break;
                                }
                                default: {
                                    Log.d(TAG, String.format("Unknown command. Given array contains:%s", generatedStrings.toString()));
                                    break;
                                }
                            }
                            break;
                        }
                        case LISTENING_FOR_NEW_DIRECTION: {
                            Log.d(TAG, "Listening for new direction");
                            // TODO: Replace with actual implementation
                            if (!ServicesTerminal.getServicesTerminal().isLocationHistoryEmpty()) {
                                directionServices.getDirectionFromDb(ServicesTerminal.getServicesTerminal().getLatestLocation(), voiceCommandServices.evaluateAsPlaces(generatedStrings));
                            } else {
                                Log.d(TAG, "Location history is empty...");
                            }
                            current_listening_state = LISTENING_STATE.LISTENING_FOR_COMMANDS;
                            break;
                        }
                        case LISTENING_FOR_EMERGENCY_NUMBER: {
                            Log.d(TAG, "Listening for emergency number");
                            String emergencyNumber = voiceCommandServices.evaluateForNumber(generatedStrings);
                            if (emergencyNumber == null) {
                                textToSpeechServices.logAndForceSpeak("An error have occured. Please try again.");
                            } else {
                                emergencyServices.setEmergencyNumber(emergencyNumber);
                            }
                            current_listening_state = LISTENING_STATE.LISTENING_FOR_COMMANDS;
                            break;
                        }
                        default: {
                            current_listening_state = LISTENING_STATE.LISTENING_FOR_COMMANDS;
                            Log.d(TAG, "State machine is broken");
                            break;
                        }
                    }
                } else {
                    Log.d(TAG, "Data is null");
                }
                break;
            }
            default: {
                Log.d(TAG,
                        "Unknown request code...defaulting. The requestCode was " + requestCode);
                break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            default: {
                Log.e(TAG, String.format("Returned from requestCode:%d", requestCode));
                break;
            }
        }
    }
}