package com.vg.audio;

import java.io.File;
import java.io.Serializable;
import java.nio.FloatBuffer;

public class Idx implements Serializable {
    private static final long serialVersionUID = 45L;
    int dist;
    int sampleRate;
    File file;
    byte[] index;
    int[] index2;
    float[] magnitudes;

    public final static int[] hz2bin = new int[24000];
    public final static int[] binSizes = new int[] { 100, 100, 100, 100, 110, 120, 140, 150, 160, 190, 210, 240, 280,
            320, 380, 450, 550, 700, 900, 1100, 1300, 1800, 2500, 3500, (Idx.hz2bin.length - 15500) };
    public final static float[] binWeights = new float[] { 1f, 1f, 1f, 1f, 100f / 110f, 100f / 120f, 100f / 140f,
            100f / 150f, 100f / 160f, 100f / 190f, 100f / 210f, 100f / 240f, 100f / 280f, 100f / 320f, 100f / 380f,
            100f / 450f, 100 / 550f, 100f / 700f, 100f / 900f, 100f / 1100f, 100f / 1300f, 100f / 1800f, 100f / 2500f,
            100f / 3500f, 100f / (24000f - 15500f) };

    public int getDist() {
        return dist;
    }

    @Override
    public String toString() {
        return GsonFactory.gsonToString(this);
    }

    public static float maxArray(float[] array) {
        float max = Float.MIN_VALUE;
        for (int i = 0; i < array.length; i++) {
            max = Math.max(max, array[i]);
        }
        return max;
    }

    public static float[] normalize(float[] magnitudes) {
        float max = Idx.maxArray(magnitudes);
        max *= 0.8f;
        float r = 1f / max;
        float[] normalized = new float[magnitudes.length];
        for (int i = 0; i < magnitudes.length; i++) {
            normalized[i] = Math.min(1f, magnitudes[i] * r);
        }
        return normalized;
    }

    public static Idx index3(AudioFile sox) {
        int sampleRate = sox.sampleRate;
        int windowSize = Math.min(sampleRate, sox.signal.length);
        JTransformsFFT fft = new JTransformsFFT(windowSize);
        FloatBuffer Signal = FloatBuffer.wrap(sox.signal);
        float[] window = new float[windowSize];
        int frequencies[] = new int[25];
        float magnitudes[] = new float[25];
        int spectrumSize = (windowSize / 2);
        int maxHz = (sampleRate / 2);
        float r = (float) maxHz / (float) spectrumSize;

        while (Signal.remaining() >= windowSize) {
            Signal.get(window);
            float[] spectrum = fft.spectrum(window);
            for (int i = 0; i < spectrum.length; i++) {
                int hz = (int) (i * r);
                int binIdx = Idx.hz2bin[hz];
                float magnitude = spectrum[i];
                //                System.out.println(hz + " " + (int) magnitude);
                if (magnitudes[binIdx] < magnitude) {
                    magnitudes[binIdx] = magnitude;
                    frequencies[binIdx] = hz;
                }
            }
        }

        float[] normalizedMagnitudes = Idx.normalize(magnitudes);

        Idx idx = new Idx();
        idx.sampleRate = sox.sampleRate;
        idx.file = sox.file;
        idx.index2 = frequencies;
        idx.magnitudes = normalizedMagnitudes;

        return idx;
    }

    public static float dist(Idx search, Idx idx) {
        float sad = 0;
        for (int bin = 0; bin < search.index2.length; bin++) {
            int f0 = search.index2[bin];
            int f1 = idx.index2[bin];
            float m0 = search.magnitudes[bin];
            float m1 = idx.magnitudes[bin];
            if (f0 > 0 && f1 > 0) {
                float mdiff = Math.abs(m1 - m0);
                float diff = Math.abs(f1 - f0);
                float weight = binWeights[bin];
                diff += mdiff * binSizes[bin];
                diff *= weight;
                sad += diff;
            } else {
                System.out.println("ololo");
            }
        }
        return sad;
    }

    static {
        for (int hz = 0; hz < 100; hz++) {
            hz2bin[hz] = 0;
        }
        for (int hz = 100; hz < 200; hz++) {
            hz2bin[hz] = 1;
        }
        for (int hz = 200; hz < 300; hz++) {
            hz2bin[hz] = 2;
        }
        for (int hz = 300; hz < 400; hz++) {
            hz2bin[hz] = 3;
        }

        for (int hz = 400; hz < 510; hz++) {
            hz2bin[hz] = 4;
        }

        for (int hz = 510; hz < 630; hz++) {
            hz2bin[hz] = 5;
        }

        for (int hz = 630; hz < 770; hz++) {
            hz2bin[hz] = 6;
        }

        for (int hz = 770; hz < 920; hz++) {
            hz2bin[hz] = 7;
        }

        for (int hz = 920; hz < 1080; hz++) {
            hz2bin[hz] = 8;
        }
        for (int hz = 1080; hz < 1270; hz++) {
            hz2bin[hz] = 9;
        }
        for (int hz = 1270; hz < 1480; hz++) {
            hz2bin[hz] = 10;
        }
        for (int hz = 1480; hz < 1720; hz++) {
            hz2bin[hz] = 11;
        }
        for (int hz = 1720; hz < 2000; hz++) {
            hz2bin[hz] = 12;
        }
        for (int hz = 2000; hz < 2320; hz++) {
            hz2bin[hz] = 13;
        }
        for (int hz = 2320; hz < 2700; hz++) {
            hz2bin[hz] = 14;
        }
        for (int hz = 2700; hz < 3150; hz++) {
            hz2bin[hz] = 15;
        }
        for (int hz = 3150; hz < 3700; hz++) {
            hz2bin[hz] = 16;
        }
        for (int hz = 3700; hz < 4400; hz++) {
            hz2bin[hz] = 17;
        }
        for (int hz = 4400; hz < 5300; hz++) {
            hz2bin[hz] = 18;
        }
        for (int hz = 5300; hz < 6400; hz++) {
            hz2bin[hz] = 19;
        }
        for (int hz = 6400; hz < 7700; hz++) {
            hz2bin[hz] = 20;
        }
        for (int hz = 7700; hz < 9500; hz++) {
            hz2bin[hz] = 21;
        }
        for (int hz = 9500; hz < 12000; hz++) {
            hz2bin[hz] = 22;
        }
        for (int hz = 9500; hz < 12000; hz++) {
            hz2bin[hz] = 22;
        }
        for (int hz = 12000; hz < 15500; hz++) {
            hz2bin[hz] = 23;
        }
        for (int hz = 15500; hz < hz2bin.length; hz++) {
            hz2bin[hz] = 24;
        }

    }

}