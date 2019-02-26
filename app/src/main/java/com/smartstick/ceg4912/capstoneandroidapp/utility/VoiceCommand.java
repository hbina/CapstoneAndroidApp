package com.smartstick.ceg4912.capstoneandroidapp.utility;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.util.Log;

import com.smartstick.ceg4912.capstoneandroidapp.MainActivity;
import com.smartstick.ceg4912.capstoneandroidapp.model.Keyword;
import com.smartstick.ceg4912.capstoneandroidapp.utility.ServicesTerminal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.PriorityQueue;

public class VoiceCommand {

    private static final String TAG = "VoiceCommand";
    private final HashMap<String, Integer> map;
    private static final String[] TRUE_KEYWORDS = {
            "Set direction",
            "Set emergency number",
            "Send help",
            "Sync"
    };

    public VoiceCommand() {
        this.map = new HashMap<>();
        for (int a = 0; a < TRUE_KEYWORDS.length; a++) {
            map.put(TRUE_KEYWORDS[a], a);
        }
    }

    public void openMic(int requestCode) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH);
        try {
            ServicesTerminal.getServicesTerminal().getCallerActivity().startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            Log.d(TAG, e.getMessage());
        }
    }

    public int evaluateCommands(ArrayList<String> givenKeywords) {
        if (givenKeywords.size() > 0) {
            PriorityQueue<Keyword> arrOfGivenKeywords = new PriorityQueue<>();
            for (String trueKeyword : TRUE_KEYWORDS) {
                for (String givenKeyword : givenKeywords) {
                    Keyword keyword = new Keyword(givenKeyword, trueKeyword);
                    arrOfGivenKeywords.add(keyword);
                }
            }
            Keyword answerKeyword = arrOfGivenKeywords.poll();
            if (answerKeyword == null) {
                return -1;
            } else {
                String trueKeyword = answerKeyword.getTrueKeyword();
                return map.get(trueKeyword);
            }
        } else {
            return -1;
        }

    }

    public String evaluateForNumber(ArrayList<String> generatedStrings) {
        for (String generateString : generatedStrings) {
            generateString = generateString.replaceAll("[^0-9]", "");
            if (TextUtils.isDigitsOnly(generateString)) {
                return generateString;
            }
        }
        return null;
    }

    public String evaluateAsPlaces(ArrayList<String> generatedStrings) {
        // TODO : Evaluate which is more likely to be places
        return generatedStrings.size() > 0 ? generatedStrings.get(0) : null;
    }
}
