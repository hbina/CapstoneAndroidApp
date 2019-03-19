package com.smartstick.ceg4912.capstoneandroidapp.services;

import android.app.Activity;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SpeechServices extends Services {

    private static final String TAG = "SpeechServices";

    private final static ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
    private TextToSpeech textToSpeech;

    public SpeechServices(Activity activity) {
        initializedTextToSpeech(activity);
    }

    private void initializedTextToSpeech(Activity activity) {
        textToSpeech = new TextToSpeech(activity, new TextToSpeech.OnInitListener() {
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

    private void logAndSpeak(String toSpeak) {
        Log.d(TAG, toSpeak);
        textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_ADD, null, null);
    }

    @Override
    public void run() {
        while (isRunning.get()) {
            if (!queue.isEmpty()) {
                logAndSpeak(queue.poll());
            }
        }
        textToSpeech.shutdown();
    }

    public static void addText(String string) {
        queue.add(string);
    }
}
