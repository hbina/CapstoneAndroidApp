package com.smartstick.ceg4912.capstoneandroidapp;

class VoiceAnalysis {

    static int calculateScore(String test, String correct) {
        int score = 0;
        int maxIndex = (test.length() > correct.length()) ? test.length() : correct.length();
        int minIndex = (test.length() > correct.length()) ? correct.length() : test.length();

        for (int a = 0; a < minIndex; a++) {
            if (test.charAt(a) == correct.charAt(a)) {
                score++;
            } else {
                score--;
            }
        }
        score -= (maxIndex - minIndex);
        return score;
    }

    static class SpeechCodes{
        static final String SET_EMERGENCY_NUMBER = "set emergency number";
    }
}
