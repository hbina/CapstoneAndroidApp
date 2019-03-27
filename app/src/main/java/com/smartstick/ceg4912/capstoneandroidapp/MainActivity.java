package com.smartstick.ceg4912.capstoneandroidapp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.smartstick.ceg4912.capstoneandroidapp.listener.BearingListener;
import com.smartstick.ceg4912.capstoneandroidapp.model.DirectionRequest;
import com.smartstick.ceg4912.capstoneandroidapp.services.RequestServices;
import com.smartstick.ceg4912.capstoneandroidapp.services.SpeechServices;
import com.smartstick.ceg4912.capstoneandroidapp.utility.EmergencyHelp;
import com.smartstick.ceg4912.capstoneandroidapp.utility.VoiceCommand;
import com.smartstick.ceg4912.capstoneandroidapp.services.RfidServices;

import java.util.ArrayList;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

public class MainActivity extends Activity {
    private ServicesThread servicesThread;

    private enum LISTENING_STATE {
        LISTENING_FOR_COMMANDS,
        LISTENING_FOR_NEW_DIRECTION,
        LISTENING_FOR_EMERGENCY_NUMBER
    }

    private static final String TAG = "MainActivity";
    private LISTENING_STATE currentListeningState = LISTENING_STATE.LISTENING_FOR_COMMANDS;
    private final static int REQ_CODE_SPEECH_OUT = 0;

    /**
     * public TextView TEXT_VIEW_BEARING;
     * public TextView TEXT_VIEW_PATH;
     * public TextView TEXT_VIEW_DIRECTION;
     * private TextView TEXT_VIEW_EMERGENCY;
     **/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /**
         TEXT_VIEW_BEARING = findViewById(R.id.di_content_bearing);
         TEXT_VIEW_PATH = findViewById(R.id.di_content_path);
         TEXT_VIEW_DIRECTION = findViewById(R.id.di_content_direction);
         TEXT_VIEW_EMERGENCY = findViewById(R.id.di_content_emergency_number);
         */
        servicesThread = new ServicesThread(this);
        servicesThread.start();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(
                Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(
                Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Requesting permissions...");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.SEND_SMS, Manifest.permission.BLUETOOTH},
                    2);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        servicesThread.killServices();
    }

    public void onVoice(View v) {
        VoiceCommand.openMic(this, REQ_CODE_SPEECH_OUT);
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
                    // TODO: Turn this into command type... with an interace execute()
                    switch (currentListeningState) {
                        case LISTENING_FOR_COMMANDS: {
                            int command = VoiceCommand.evaluateCommands(generatedStrings);
                            Log.d(TAG, "comamnd:" + command);
                            switch (command) {
                                case 0: {
                                    currentListeningState = LISTENING_STATE.LISTENING_FOR_NEW_DIRECTION;
                                    VoiceCommand.openMic(this, REQ_CODE_SPEECH_OUT);
                                    break;
                                }
                                case 1: {
                                    currentListeningState = LISTENING_STATE.LISTENING_FOR_EMERGENCY_NUMBER;
                                    VoiceCommand.openMic(this, REQ_CODE_SPEECH_OUT);
                                    break;
                                }
                                case 2: {
                                    EmergencyHelp.sendEmergencySMS(this);
                                    break;
                                }
                                case 3: {
                                    // TODO : Vibrate stuff
                                    Log.d(TAG, "Syncing??");
                                    BearingListener.syncBearingOffset();
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
                            if (RfidServices.getCurrentLocation() != null) {
                                DirectionRequest directionRequest = new DirectionRequest(RfidServices.getCurrentLocation(), VoiceCommand.evaluateAsPlaces(generatedStrings));
                                RequestServices.addDirectionRequest(directionRequest);
                            } else {
                                Log.d(TAG, "Location history is empty...");
                            }
                            currentListeningState = LISTENING_STATE.LISTENING_FOR_COMMANDS;
                            break;
                        }
                        case LISTENING_FOR_EMERGENCY_NUMBER: {
                            Log.d(TAG, "Listening for emergency number");
                            String emergencyNumber = VoiceCommand.evaluateAsNumbers(generatedStrings);
                            if (emergencyNumber == null) {
                                SpeechServices.addText("An error have occured. Please try again.");
                            } else {
                                ((TextView) findViewById(R.id.di_content_emergency_number)).setText(String.format(Locale.ENGLISH, "Emergency number:%s", emergencyNumber));
                                EmergencyHelp.setEmergencyNumber(emergencyNumber);
                            }
                            currentListeningState = LISTENING_STATE.LISTENING_FOR_COMMANDS;
                            break;
                        }
                        default: {
                            currentListeningState = LISTENING_STATE.LISTENING_FOR_COMMANDS;
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