package com.smartstick.ceg4912.capstoneandroidapp.model;

public class BearingRequest {
    public final String currentRFID;
    public final String nextNode;
    public final String currentBearing;

    public BearingRequest(String currentRFID, String nextNode, String currentBearing) {
        this.currentRFID = currentRFID;
        this.nextNode = nextNode;
        this.currentBearing = currentBearing;
    }
}
