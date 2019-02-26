package com.smartstick.ceg4912.capstoneandroidapp.services;

import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.smartstick.ceg4912.capstoneandroidapp.MainActivity;
import com.smartstick.ceg4912.capstoneandroidapp.utility.ServicesTerminal;

import org.w3c.dom.Text;

import java.util.Locale;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

// TODO: Make this run on its own thread?
public class SpeechServices extends Services {

    private static final String TAG = "SpeechServices";

    private final static ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
    private TextToSpeech textToSpeech;

    public SpeechServices() {
        initializedTextToSpeech();
    }

    private void initializedTextToSpeech() {
        textToSpeech = new TextToSpeech(ServicesTerminal.getServicesTerminal().getCallerActivity(), new TextToSpeech.OnInitListener() {
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

    private void logAndForceSpeak(String toSpeak) {
        Log.d(TAG, toSpeak);
        textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    @Override
    public void run() {
        while (isRunning.get()) {
            if (!queue.isEmpty()) {
                logAndSpeak(queue.poll());
            }
        }
    }

    public static void addText(String string) {
        queue.add(string);
    }
}
