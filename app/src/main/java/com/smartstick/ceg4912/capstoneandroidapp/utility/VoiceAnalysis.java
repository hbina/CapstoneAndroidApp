package com.smartstick.ceg4912.capstoneandroidapp.utility;

import com.smartstick.ceg4912.capstoneandroidapp.model.Keyword;

import java.util.ArrayList;
import java.util.PriorityQueue;

public class VoiceAnalysis {

    private static final String[] TRUE_KEYWORDS = {
            "Set direction",
            "Send emergency message",
            "Sync device"
    };

    public static String evaluate(ArrayList<String> givenKeywords) {
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

    private static int calculateScore(String trueString, String givenString) {
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


}
