package com.kindaworks.gridflow.util;

public final class TickScheduler {
    private int counter = 0;
    private final int interval;

    public TickScheduler(int interval) {
        this.interval = interval;
    }

    /** Run every tick - returns true once every `interval` ticks. Runs in first tick after initialization. */
    public boolean shouldRun() {
        try {
            return counter == 0;
        } finally {
            if (++counter >= interval) {
                counter = 0;
            }
        }
    }
}