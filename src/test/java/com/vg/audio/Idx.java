package com.vg.audio;

import java.io.File;
import java.io.Serializable;

public class Idx implements Serializable {
    private static final long serialVersionUID = 44L;
    int dist;
    int sampleRate;
    File file;
    byte[] index;
    int[] index2;
    float[] magnitudes;

    public int getDist() {
        return dist;
    }

    @Override
    public String toString() {
        return GsonFactory.gsonToString(this);
    }
}