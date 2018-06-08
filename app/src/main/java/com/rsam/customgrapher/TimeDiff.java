package com.rsam.customgrapher;

public class TimeDiff {
    public long millisA = 0;
    public long millisB = 0;
    public long diff;
    public long prevDiff;

    public void setMillisA() {
        millisA = System.currentTimeMillis();
    }

    // Only set B will start the diff calculation
    public void setMillisB() {
        millisB = System.currentTimeMillis();
        prevDiff = diff;
        diff = millisB - millisA;
    }
}