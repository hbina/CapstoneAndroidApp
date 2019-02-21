package com.smartstick.ceg4912.capstoneandroidapp;

public class Keyword implements Comparable<Keyword> {
    private int score;
    private final String givenKeyword;
    private final String trueKeyword;

    public Keyword(String givenKeyword, String trueKeyword, int score) {
        this.givenKeyword = givenKeyword;
        this.trueKeyword = trueKeyword;
        this.score = score;
    }

    public String getGivenKeyword() {
        return this.givenKeyword;
    }

    public String getTrueKeyword() {
        return this.trueKeyword;
    }

    @Override
    public int compareTo(Keyword o) {
        return Integer.compare(this.score, o.score);
    }
}
