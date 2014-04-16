package com.vg.audio;

import org.junit.Assert;

import edu.emory.mathcs.jtransforms.fft.FloatFFT_1D;

public class JTransformsFFT {
    private int signalSize;
    private FloatFFT_1D jfft;
    private float[] spectrum;
    private float[] copy;

    public JTransformsFFT(int signalSize) {
        this.signalSize = signalSize;
        this.spectrum = new float[signalSize / 2];
        this.jfft = new FloatFFT_1D(signalSize);
        this.copy = new float[signalSize];
    }

    public float[] spectrum(float[] signal) {
        Assert.assertFalse(signal.length > copy.length);
        Assert.assertFalse(signalSize > signal.length);
        System.arraycopy(signal, 0, copy, 0, signalSize);
        jfft.realForward(copy);
        for (int i = 0; i < spectrum.length; i++) {
            float reali = copy[i * 2];
            float imagi = copy[i * 2 + 1];
            spectrum[i] = (float) Math.sqrt(reali * reali + imagi * imagi);
        }
        return spectrum;
    }

}
