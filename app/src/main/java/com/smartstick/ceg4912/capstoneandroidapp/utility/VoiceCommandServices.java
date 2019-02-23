package com.smartstick.ceg4912.capstoneandroidapp.utility;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.util.Log;

import com.smartstick.ceg4912.capstoneandroidapp.MainActivity;
import com.smartstick.ceg4912.capstoneandroidapp.model.Keyword;

import java.util.ArrayList;
import java.util.Locale;
import java.util.PriorityQueue;

public class VoiceCommandServices {

    private static final String[] TRUE_KEYWORDS = {
            "Set direction",
            "Set emergency number",
            "Send emergency message",
            "Sync device"
    };
    private final MainActivity callerActivity;

    public VoiceCommandServices(MainActivity callerActivity) {
        this.callerActivity = callerActivity;
    }

    public void openMic(int requestCode) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH);
        // intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Please enter direction");

        try {
            callerActivity.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            Log.d(this.toString(), e.getMessage());
        }
    }

    public String evaluateCommands(ArrayList<String> givenKeywords) {
        PriorityQueue<Keyword> arrOfGivenKeywords = new PriorityQueue<>();
        for (String trueKeyword : TRUE_KEYWORDS) {
            for (String givenKeyword : givenKeywords) {
                int score = calculateScore(trueKeyword, givenKeyword);
                arrOfGivenKeywords.add(new Keyword(trueKeyword, givenKeyword, score));
            }
        }
        Keyword answerKeyword = arrOfGivenKeywords.poll();
        if (answerKeyword == null) {
            return "ERROR";
        }
        return answerKeyword.getTrueKeyword();
    }

    private int calculateScore(String trueString, String givenString) {
        int score = 0;
        int maxIndex = (trueString.length() > givenString.length()) ? trueString.length() : givenString.length();
        int minIndex = (trueString.length() > givenString.length()) ? givenString.length() : trueString.length();

        for (int a = 0; a < minIndex; a++) {
            if (trueString.charAt(a) == givenString.charAt(a)) {
                score++;
            } else {
                score--;
            }
        }
        score -= (maxIndex - minIndex);
        return score;
    }


    public void parse(ArrayList<String> generatedStrings) {

    }

    public String evaluateForNumber(ArrayList<String> generatedStrings) {
        // TODO: Evaluate which is more likely to be numbers
        return "";
    }

    public String evaluateAsPlaces(ArrayList<String> generatedStrings) {
        // TODO : Evaluate which is more likely to be places
        return "";
    }
}
