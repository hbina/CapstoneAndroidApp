package com.smartstick.ceg4912.capstoneandroidapp.services;

import java.util.concurrent.atomic.AtomicBoolean;

public class Services extends Thread {
    final AtomicBoolean isRunning = new AtomicBoolean(true);

    public void killThread() {
        isRunning.set(false);
    }
}
