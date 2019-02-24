package com.smartstick.ceg4912.capstoneandroidapp.model;

import org.jetbrains.annotations.NotNull;

public class Keyword implements Comparable<Keyword> {
    private final int score;
    private final String givenKeyword;
    private final String trueKeyword;

    public Keyword(String givenKeyword, String trueKeyword) {
        this.givenKeyword = givenKeyword;
        this.trueKeyword = trueKeyword;
        this.score = calculateScore();
    }

    private int calculateScore() {
        int score = 0;
        int maxIndex = (trueKeyword.length() > givenKeyword.length()) ? trueKeyword.length() : givenKeyword.length();
        int minIndex = (trueKeyword.length() > givenKeyword.length()) ? givenKeyword.length() : trueKeyword.length();

        for (int a = 0; a < minIndex; a++) {
            if (trueKeyword.charAt(a) == givenKeyword.charAt(a)) {
                score++;
            } else {
                score--;
            }
        }
        score -= (maxIndex - minIndex);
        return score;
    }

    public String getTrueKeyword() {
        return this.trueKeyword;
    }

    public int getScore() {
        return this.score;
    }

    @Override
    public int compareTo(Keyword o) {
        return Integer.compare(o.score, this.score);
    }

    @NotNull
    @Override
    public String toString() {
        return String.format("trueKeyword:%s givenKeyword:%s score:%s", trueKeyword, givenKeyword, score);
    }
}
