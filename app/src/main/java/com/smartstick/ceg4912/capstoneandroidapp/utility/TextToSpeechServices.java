package com.smartstick.ceg4912.capstoneandroidapp.utility;

import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.smartstick.ceg4912.capstoneandroidapp.MainActivity;

import java.util.Locale;

// TODO: Make this run on its own thread?
public class TextToSpeechServices {

    private static final String TAG = "TextToSpeechServices";
    private TextToSpeech textToSpeech;
    private final MainActivity callerActivity;

    public TextToSpeechServices(MainActivity callerActivity) {
        this.callerActivity = callerActivity;
        initializedTextToSpeech();
    }

    private void initializedTextToSpeech() {
        textToSpeech = new TextToSpeech(callerActivity, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.US);
                } else {
                    String errorStatus = "Failed to initialize textToSpeech with status:" + status;
                    Log.d(TAG, errorStatus);
                }
            }
        });
    }

    public void logAndSpeak(String toSpeak) {
        Log.d(TAG, toSpeak);
        textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_ADD, null, null);
    }

    public void logAndForceSpeak(String toSpeak) {
        Log.d(TAG, toSpeak);
        textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
    }
}
