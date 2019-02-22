package com.smartstick.ceg4912.capstoneandroidapp.utility;

import java.util.ArrayList;

public class LocalDatabase {
    private final ArrayList<String> userLocationHistory;

    public LocalDatabase() {
        this.userLocationHistory = new ArrayList<>();
    }

    public String getUserCurrentLocation() {
        return userLocationHistory.get(userLocationHistory.size() - 1);
    }

    public void setCurrentLocation(String userNewLocation) {
        // TODO: Make this thread safe
        userLocationHistory.add(userNewLocation);
    }

    public ArrayList<String> getLocationHistory() {
        return new ArrayList<>(userLocationHistory);
    }
    // TODO: Must declare everything required by other classes inside this...
}
