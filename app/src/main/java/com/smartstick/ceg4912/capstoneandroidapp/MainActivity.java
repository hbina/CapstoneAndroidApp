package com.smartstick.ceg4912.capstoneandroidapp;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;

import com.smartstick.ceg4912.capstoneandroidapp.utility.BluetoothServices;
import com.smartstick.ceg4912.capstoneandroidapp.utility.DirectionServices;
import com.smartstick.ceg4912.capstoneandroidapp.utility.EmergencyServices;
import com.smartstick.ceg4912.capstoneandroidapp.utility.TextToSpeechServices;
import com.smartstick.ceg4912.capstoneandroidapp.utility.VoiceCommandServices;

import java.util.ArrayList;

import androidx.annotation.NonNull;

import static com.smartstick.ceg4912.capstoneandroidapp.MainActivity.RequestCodes.EMERGENCY_REQUEST_CODES;
import static com.smartstick.ceg4912.capstoneandroidapp.MainActivity.RequestCodes.REQUEST_CODE_TURN_BLUETOOTH_ON;

public class MainActivity extends Activity {


    private enum LISTENING_STATE {
        LISTENING_FOR_COMMANDS,
        LISTENING_FOR_NEW_DIRECTION,
        LISTENING_FOR_EMERGENCY_NUMBER
    }

    ;
    private LISTENING_STATE listening_state;
    private final static int REQ_CODE_SPEECH_OUT = 143;
    private TextToSpeechServices textToSpeechServices;
    private BluetoothServices bluetoothServices;
    private DirectionServices directionServices;
    private VoiceCommandServices voiceCommandServices;
    private EmergencyServices emergencyServices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textToSpeechServices = new TextToSpeechServices(this);
        bluetoothServices = new BluetoothServices(this);
        bluetoothServices.init(REQUEST_CODE_TURN_BLUETOOTH_ON);
        directionServices = new DirectionServices(this);
        voiceCommandServices = new VoiceCommandServices(this);
        emergencyServices = new EmergencyServices();
    }

    @Override
    public void onResume() {
        super.onResume();
        emergencyServices.checkEmergencyPermissions();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        bluetoothServices.killBluetooth();
        directionServices.cancelAll();
    }

    public void onVoice(View v) {
        voiceCommandServices.openMic(REQ_CODE_SPEECH_OUT);
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
                    ArrayList<String> generatedStrings = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    switch (listening_state) {
                        case LISTENING_FOR_COMMANDS: {
                            String commands = voiceCommandServices.evaluateCommands(generatedStrings);
                            switch (commands) {
                                case "Set direction": {
                                    listening_state = LISTENING_STATE.LISTENING_FOR_NEW_DIRECTION;
                                    break;
                                }
                                case "Set emergency number": {
                                    listening_state = LISTENING_STATE.LISTENING_FOR_EMERGENCY_NUMBER;
                                    break;
                                }
                                case "Send emergency message": {
                                    emergencyServices.sendEmergencySMS();
                                    break;
                                }
                                case "Sync device": {
                                    // TODO : Vibrate stuff
                                    break;
                                }
                                default: {
                                    Log.d(this.toString(), "Evaluate command is broken");
                                    break;
                                }
                            }
                            break;
                        }
                        case LISTENING_FOR_NEW_DIRECTION: {
                            directionServices.getDirectionFromDb(bluetoothServices.getCurrentLocation(), voiceCommandServices.evaluateAsPlaces(generatedStrings), textToSpeechServices);
                            listening_state = LISTENING_STATE.LISTENING_FOR_COMMANDS;
                            break;
                        }
                        case LISTENING_FOR_EMERGENCY_NUMBER: {
                            emergencyServices.setEmergencyNumber(voiceCommandServices.evaluateForNumber(generatedStrings));
                            listening_state = LISTENING_STATE.LISTENING_FOR_COMMANDS;
                            break;
                        }
                        default: {
                            listening_state = LISTENING_STATE.LISTENING_FOR_COMMANDS;
                            Log.d(this.toString(), "State machine is broken");
                            break;
                        }
                    }

                    // TODO: Parse received string...
                    voiceCommandServices.parse(generatedStrings);
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

            case EMERGENCY_REQUEST_CODES: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // TODO: Always perform this check...
                    Log.d(this.toString(), "Emergency request have been granted");
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

    static class RequestCodes {
        static final int REQUEST_CODE_TURN_BLUETOOTH_ON = 0;
        static final int EMERGENCY_REQUEST_CODES = 1;

    }
}