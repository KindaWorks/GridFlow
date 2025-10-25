package com.kindaworks.rsfactory.util;

public final class TickScheduler {
    private int counter = 0;
    private final int interval;

    public TickScheduler(int interval) {
        this.interval = interval;
    }

    public boolean shouldRun() {
        counter++;
        if (counter >= interval) {
            counter = 0;
            return true;
        }
        return false;
    }
}