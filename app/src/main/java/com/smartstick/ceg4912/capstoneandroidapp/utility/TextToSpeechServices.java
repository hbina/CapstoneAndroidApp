package com.smartstick.ceg4912.capstoneandroidapp.utility;

import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.smartstick.ceg4912.capstoneandroidapp.MainActivity;

import java.util.Locale;

public class TextToSpeechServices {

    private TextToSpeech textToSpeech;
    private MainActivity callerActivity;

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
                    Log.d(this.toString(), errorStatus);
                }
            }
        });
    }

    public void logAndSpeak(String toSpeak) {
        Log.d(this.toString(), toSpeak);
        textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_ADD, null, null);
    }

    public void logAndForceSpeak(String toSpeak) {
        Log.d(this.toString(), toSpeak);
        textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
    }
}
