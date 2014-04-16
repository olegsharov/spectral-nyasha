package com.vg.audio;

import static org.junit.Assert.*;
import gnu.trove.list.array.TFloatArrayList;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.lang.ProcessBuilder.Redirect;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.MappedByteBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;

import ch.lambdaj.Lambda;

public class GaTest {

    @Test
    public void testFloat() throws Exception {
        File f = new File("/Users/zhukov/testdata/philarmonia samples/contrabassoon/0442.raw");
        DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(f)));
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        try {
            for (;;) {
                float readFloat = in.readFloat();
                min = Math.min(min, readFloat);
                max = Math.max(max, readFloat);
            }
        } catch (EOFException e) {
        }
        System.out.println(min + " " + max);
    }

    static float[] readFloatSignal(File f) throws IOException {
        DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(f)));
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        TFloatArrayList samples = new TFloatArrayList();
        try {
            for (;;) {
                float readFloat = in.readFloat();
                samples.add(readFloat);
                min = Math.min(min, readFloat);
                max = Math.max(max, readFloat);
            }
        } catch (EOFException e) {
        } finally {
            in.close();
        }
        System.out.println(min + " " + max);

        return samples.toArray();
    }

    static float[] readWav(File ec) throws IOException {
        RandomAccessFile r = new RandomAccessFile(ec, "r");
        FileChannel channel = r.getChannel();
        MappedByteBuffer buf = channel.map(MapMode.READ_ONLY, 44, ec.length() - 44);
        FloatBuffer signal = FloatBuffer.allocate(buf.capacity() / 2);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.clear();
        ShortBuffer asShortBuffer = buf.asShortBuffer();
        float r32767 = 1f / 32767f;
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        while (asShortBuffer.hasRemaining()) {
            short s = asShortBuffer.get();
            float smp = s * r32767;
            min = Math.min(min, smp);
            max = Math.max(max, smp);
            signal.put(smp);
        }
        System.out.println(min + " " + max);

        Assert.assertFalse(signal.hasRemaining());
        signal.clear();
        return signal.array();
    }

    static float[] readRaw(File ec) throws IOException {
        RandomAccessFile r = new RandomAccessFile(ec, "r");
        FileChannel channel = r.getChannel();
        MappedByteBuffer buf = channel.map(MapMode.READ_ONLY, 0, ec.length());
        FloatBuffer signal = FloatBuffer.allocate(buf.capacity() / 2);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.clear();
        ShortBuffer asShortBuffer = buf.asShortBuffer();
        float r32767 = 1f / 32767f;
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        while (asShortBuffer.hasRemaining()) {
            short s = asShortBuffer.get();
            float smp = s * r32767;
            min = Math.min(min, smp);
            max = Math.max(max, smp);
            signal.put(smp);
        }
        System.out.println(min + " " + max);

        Assert.assertFalse(signal.hasRemaining());
        signal.clear();
        return signal.array();
    }

    static class AudioFile {
        int sampleRate;
        float[] signal;
        public File file;
    }

    static AudioFile sox(File f) throws IOException, InterruptedException {
        //soxi
        ProcessBuilder pbi = new ProcessBuilder("soxi", f.getAbsolutePath());
        pbi.redirectError(Redirect.INHERIT);
        Process soxi = pbi.start();
        soxi.getOutputStream().close();
        List<String> readLines = IOUtils.readLines(soxi.getInputStream());
        int exitCodei = soxi.waitFor();
        Assert.assertEquals(0, exitCodei);

        Map<String, String> map = new LinkedHashMap<String, String>();
        for (String string : readLines) {
            String[] split = string.split(":", 2);
            if (split.length == 2) {
                map.put(split[0].trim(), split[1].trim());
            }
        }
        AudioFile af = new AudioFile();
        af.file = f;
        af.sampleRate = Integer.parseInt(map.get("Sample Rate"));

        //sox 0442.mp3 -e signed -b 16 -t raw -
        ProcessBuilder pb = new ProcessBuilder("sox", f.getAbsolutePath(), "-e", "signed", "-b", "16", "-t", "raw", "-");
        pb.redirectError(Redirect.INHERIT);
        Process sox = pb.start();
        sox.getOutputStream().close();
        InputStream inputStream = sox.getInputStream();
        byte[] byteArray = IOUtils.toByteArray(inputStream);
        int exitCode = sox.waitFor();
        Assert.assertEquals(0, exitCode);

        ByteBuffer buf = ByteBuffer.wrap(byteArray);
        FloatBuffer signal = FloatBuffer.allocate(buf.capacity() / 2);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.clear();
        ShortBuffer asShortBuffer = buf.asShortBuffer();
        float r32767 = 1f / 32767f;
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        while (asShortBuffer.hasRemaining()) {
            short s = asShortBuffer.get();
            float smp = s * r32767;
            min = Math.min(min, smp);
            max = Math.max(max, smp);
            signal.put(smp);
        }
        System.out.println(min + " " + max);

        Assert.assertFalse(signal.hasRemaining());
        signal.clear();
        af.signal = signal.array();

        return af;
    }

    static byte[] index(float[] signal) {
        JTransformsFFT fft = new JTransformsFFT(signal.length);
        float[] spectrum = fft.spectrum(signal);
        System.out.println(spectrum.length);

        FloatBuffer _s = FloatBuffer.allocate(1024);
        FloatBuffer s = FloatBuffer.wrap(spectrum);
        int cutoffIndex = (int) (8000L * spectrum.length / (44100 / 2));
        System.out.println(cutoffIndex);
        Assert.assertTrue(cutoffIndex > 0);
        s.limit(Math.min(s.capacity(), cutoffIndex));
        int count = cutoffIndex / 1024;
        while (_s.hasRemaining()) {
            float _max = 0;
            for (int i = 0; i < count && s.hasRemaining(); i++) {
                _max = Math.max(_max, s.get());
            }
            _s.put(_max);
        }
        _s.clear();

        //        for (int i = 0; i < spectrum.length; i++) {
        //            float x = spectrum[i];
        //            int hz = (44100 / 2) * i / spectrum.length;
        //            min = Math.min(min, x);
        //            max = Math.max(max, x);
        //            if (x > 1f && !Float.isInfinite(x)) {
        //                System.out.println(hz + "hz " + i + " " + x);
        //            }
        //        }
        //        float r = 255f / max;
        ByteBuffer d = ByteBuffer.allocate(_s.capacity());
        while (_s.hasRemaining()) {
            float x = _s.get();
            int smp = (int) (x * 255f / spectrum.length);
            d.put((byte) Math.min(255, smp));
        }
        return d.array();
    }

    static int[] index2(AudioFile sox) {
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
                int binIdx = hz2bin[hz];
                float magnitude = spectrum[i];
                //                System.out.println(hz + " " + (int) magnitude);
                if (magnitudes[binIdx] < magnitude) {
                    magnitudes[binIdx] = magnitude;
                    frequencies[binIdx] = hz;
                }
            }
        }

        return frequencies;
    }

    static Idx index3(AudioFile sox) {
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
                int binIdx = hz2bin[hz];
                float magnitude = spectrum[i];
                //                System.out.println(hz + " " + (int) magnitude);
                if (magnitudes[binIdx] < magnitude) {
                    magnitudes[binIdx] = magnitude;
                    frequencies[binIdx] = hz;
                }
            }
        }

        float[] normalizedMagnitudes = normalize(magnitudes);

        Idx idx = new Idx();
        idx.sampleRate = sox.sampleRate;
        idx.file = sox.file;
        idx.index2 = frequencies;
        idx.magnitudes = normalizedMagnitudes;

        return idx;
    }

    private static float[] normalize(float[] magnitudes) {
        float max = maxArray(magnitudes);
        max *= 0.8f;
        float r = 1f / max;
        float[] normalized = new float[magnitudes.length];
        for (int i = 0; i < magnitudes.length; i++) {
            normalized[i] = Math.min(1f, magnitudes[i] * r);
        }
        return normalized;
    }

    @Test
    public void testNormalize() throws Exception {
        float[] a = new float[] { 1, 2, 3, 4 };
        String gsonToString = GsonFactory.gsonToString(normalize(a));
        System.out.println(gsonToString);
    }

    static float maxArray(float[] array) {
        float max = Float.MIN_VALUE;
        for (int i = 0; i < array.length; i++) {
            max = Math.max(max, array[i]);
        }
        return max;
    }

    @Test
    public void test440() throws Exception {
        File f2 = new File("/Users/zhukov/git/tle-1.3x/test-data/wav/440880.wav");
        File f = new File("/Users/zhukov/Downloads/sine 1k 0.5s.wav");
        AudioFile sox = sox(f);
        int[] index2 = index2(sox);
    }

    private final static int[] hz2bin = new int[24000];
    private final static float[] binWeights = new float[] { 1f, 1f, 1f, 1f, 100f / 110f, 100f / 120f, 100f / 140f,
            100f / 150f, 100f / 160f, 100f / 190f, 100f / 210f, 100f / 240f, 100f / 280f, 100f / 320f, 100f / 380f,
            100f / 450f, 100 / 550f, 100f / 700f, 100f / 900f, 100f / 1100f, 100f / 1300f, 100f / 1800f, 100f / 2500f,
            100f / 3500f, 100f / (24000f - 15500f) };
    private final static int[] binSizes = new int[] { 100, 100, 100, 100, 110, 120, 140, 150, 160, 190, 210, 240, 280,
            320, 380, 450, 550, 700, 900, 1100, 1300, 1800, 2500, 3500, (hz2bin.length - 15500) };
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

    @Test
    public void testOleg() throws Exception {
        //        File mp3442 = new File("/Users/zhukov/testdata/philarmonia samples/contrabassoon/0442.mp3");
        File mp3442 = new File("/Users/zhukov/testdata/oleg_voice.wav");
        AudioFile signal = sox(mp3442);
        JTransformsFFT fft = new JTransformsFFT(44100);
        FloatBuffer Signal = FloatBuffer.wrap(signal.signal);
        float[] window = new float[44100];
        float[] maxSpectrum = new float[44100 / 2];
        while (Signal.remaining() >= 44100) {
            Signal.get(window);
            float[] spectrum = fft.spectrum(window);
            for (int i = 0; i < spectrum.length; i++) {
                maxSpectrum[i] = Math.max(maxSpectrum[i], spectrum[i]);
            }
        }
        float max = 0;
        for (int i = 0; i < maxSpectrum.length; i++) {
            max = Math.max(max, maxSpectrum[i]);
        }
        float r = 65535f / max;

        BufferedImage bi = new BufferedImage(1, 22050, BufferedImage.TYPE_USHORT_GRAY);
        DataBufferUShort dataBuffer = (DataBufferUShort) bi.getRaster().getDataBuffer();
        System.out.println(dataBuffer.getClass());
        short[] data = dataBuffer.getData();
        int frequencies[] = new int[25];
        float magnitudes[] = new float[25];
        for (int i = 0; i < maxSpectrum.length; i++) {
            int hz = i;
            int binIdx = hz2bin[hz];
            float magnitude = maxSpectrum[i];
            if (magnitudes[binIdx] < magnitude) {
                magnitudes[binIdx] = magnitude;
                frequencies[binIdx] = hz;
            }

            int x = (int) (magnitude * r);
            System.out.println(x);
            data[i] = (short) (x & 0xffff);
        }
        ImageIO.write(bi, "png", new File("oleg_voice.png"));
    }

    @Test
    public void testOlegBins() throws Exception {
        //        File mp3442 = new File("/Users/zhukov/testdata/philarmonia samples/contrabassoon/0442.mp3");
        File mp3442 = new File("/Users/zhukov/testdata/oleg_voice.wav");
        AudioFile signal = sox(mp3442);
        JTransformsFFT fft = new JTransformsFFT(44100);
        FloatBuffer Signal = FloatBuffer.wrap(signal.signal);
        float[] window = new float[44100];
        float[] maxSpectrum = new float[44100 / 2];
        while (Signal.remaining() >= 44100) {
            Signal.get(window);
            float[] spectrum = fft.spectrum(window);
            for (int i = 0; i < spectrum.length; i++) {
                maxSpectrum[i] = Math.max(maxSpectrum[i], spectrum[i]);
            }
        }

        BufferedImage bi = new BufferedImage(1, 22050, BufferedImage.TYPE_BYTE_GRAY);
        DataBufferByte dataBuffer = (DataBufferByte) bi.getRaster().getDataBuffer();
        System.out.println(dataBuffer.getClass());
        byte[] data = dataBuffer.getData();
        int frequencies[] = new int[25];
        float magnitudes[] = new float[25];
        for (int i = 0; i < maxSpectrum.length; i++) {
            int hz = i;
            int binIdx = hz2bin[hz];
            float magnitude = maxSpectrum[i];
            if (magnitudes[binIdx] < magnitude) {
                magnitudes[binIdx] = magnitude;
                frequencies[binIdx] = hz;
            }
        }

        System.out.println(GsonFactory.gsonToString(frequencies));
        for (int i = 0; i < frequencies.length; i++) {
            int j = frequencies[i];
            data[j] = (byte) 255;
        }

        ImageIO.write(bi, "png", new File("oleg_voice_bins.png"));
    }

    @Test
    public void testFFT() throws Exception {
        File ec = new File("/Users/zhukov/Downloads/sine 1k.wav");
        File ec10 = new File("/Users/zhukov/Downloads/sine 1006hz 10s.wav");
        File _442 = new File("/Users/zhukov/testdata/philarmonia samples/contrabassoon/0442.wav");
        File raw442 = new File("/Users/zhukov/testdata/philarmonia samples/contrabassoon/0442.raw");
        File mp3442 = new File("/Users/zhukov/testdata/philarmonia samples/contrabassoon/0442.mp3");
        //        float[] signal = readFloatSignal(new File("/Users/zhukov/testdata/philarmonia samples/contrabassoon/0442.raw"));
        //        float[] signal = readWav(ec10);
        //        float[] signal = readRaw(raw442);
        AudioFile signal = sox(mp3442);
        JTransformsFFT fft = new JTransformsFFT(signal.signal.length);
        float[] spectrum = fft.spectrum(signal.signal);
        System.out.println(spectrum.length);
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;

        FloatBuffer _s = FloatBuffer.allocate(1024);
        FloatBuffer s = FloatBuffer.wrap(spectrum);
        int cutoffIndex = 8000 * spectrum.length / (44100 / 2);
        System.out.println(cutoffIndex);
        s.limit(cutoffIndex);
        int count = cutoffIndex / 1024;
        while (_s.hasRemaining()) {
            float _max = 0;
            for (int i = 0; i < count && s.hasRemaining(); i++) {
                _max = Math.max(_max, s.get());
            }
            _s.put(_max);
        }
        _s.clear();

        for (int i = 0; i < spectrum.length; i++) {
            float x = spectrum[i];
            int hz = (44100 / 2) * i / spectrum.length;
            min = Math.min(min, x);
            max = Math.max(max, x);
            if (x > 1f && !Float.isInfinite(x)) {
                System.out.println(hz + "hz " + i + " " + x + "/" + spectrum.length);
            }
        }
        BufferedImage bi = new BufferedImage(1, _s.capacity(), BufferedImage.TYPE_BYTE_GRAY);
        DataBufferByte dataBuffer = (DataBufferByte) bi.getRaster().getDataBuffer();
        byte[] data = dataBuffer.getData();
        ByteBuffer d = ByteBuffer.wrap(data);
        while (_s.hasRemaining()) {
            float x = _s.get();
            int smp = (int) (x * 255f / spectrum.length);
            System.out.println(smp);
            d.put((byte) Math.min(255, smp));
        }
        ImageIO.write(bi, "png", new File("test.png"));

    }

    @Test
    public void testIndex() throws Exception {
        List<File> find = find(new File("/Users/zhukov/testdata/philarmonia samples"), FileFilterUtils.suffixFileFilter(".mp3"));
        List<Idx> indexes = new ArrayList<Idx>();
        for (File file : find) {
            System.out.println(file);
            byte[] index = index(sox(file).signal);
            Idx idx = new Idx();
            idx.file = file;
            idx.index = index;
            idx.dist = 0;
            indexes.add(idx);
        }

        File file = new File("/Users/zhukov/Downloads/search term 1.aif");
        byte[] index = index(sox(file).signal);
        ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(new File(
                "index.dat"))));
        out.writeObject(indexes);
        out.close();

        for (Idx idx : indexes) {
            int usadu8 = usadu8(ByteBuffer.wrap(index), ByteBuffer.wrap(idx.index));
            idx.dist = usadu8;
        }

        List<Idx> sort = Lambda.sort(indexes, Lambda.on(Idx.class).getDist());
        for (int i = 0; i < 10; i++) {
            System.out.println(sort.get(i));
        }
    }

    @Test
    public void testIndex2() throws Exception {
        List<File> find = find(new File("/Users/zhukov/testdata/philarmonia samples"), FileFilterUtils.suffixFileFilter(".mp3"));
        ExecutorCompletionService<Idx> ecs = new ExecutorCompletionService<Idx>(
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
        for (final File file : find) {
            ecs.submit(new Callable<Idx>() {
                @Override
                public Idx call() throws Exception {
                    try {
                        System.out.println(file);
                        AudioFile sox = sox(file);
                        int[] index = index2(sox);
                        Idx idx = new Idx();
                        idx.sampleRate = sox.sampleRate;
                        idx.file = file;
                        idx.index2 = index;
                        idx.dist = 0;
                        return idx;
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.err.println(file + " " + e);
                    }
                    return null;
                }
            });
        }
        List<Idx> indexes = new ArrayList<Idx>();
        for (File file : find) {
            System.out.println(file);
            Future<Idx> take = ecs.take();
            Idx idx = take.get();
            if (idx != null) {
                indexes.add(idx);
            }
        }

        ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(new File(
                "index.dat"))));
        out.writeObject(indexes);
        out.close();
    }

    @Test
    public void testIndex3_239() throws Exception {
        File file = new File("/Users/zhukov/testdata/philarmonia samples/trumpet/239.mp3");
        AudioFile sox = sox(file);
        Idx idx = index3(sox);
        for (int i = 0; i < idx.index2.length; i++) {
            System.out.println(idx.index2[i] + "hz " + idx.magnitudes[i]);
        }
    }

    @Test
    public void testIndex3() throws Exception {
        List<File> find = find(new File("/Users/zhukov/testdata/philarmonia samples"), FileFilterUtils.suffixFileFilter(".mp3"));
        ExecutorCompletionService<Idx> ecs = new ExecutorCompletionService<Idx>(
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
        for (final File file : find) {
            ecs.submit(new Callable<Idx>() {
                @Override
                public Idx call() throws Exception {
                    try {
                        System.out.println(file);
                        AudioFile sox = sox(file);
                        Idx idx = index3(sox);
                        return idx;
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.err.println(file + " " + e);
                    }
                    return null;
                }
            });
        }
        List<Idx> indexes = new ArrayList<Idx>();
        for (File file : find) {
            System.out.println(file);
            Future<Idx> take = ecs.take();
            Idx idx = take.get();
            if (idx != null) {
                indexes.add(idx);
            }
        }

        ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(new File(
                "index3.dat"))));
        out.writeObject(indexes);
        out.close();
    }

    @Test
    public void testSearch() throws Exception {
        ObjectInputStream in = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(new File("index.dat"))));
        List<Idx> indexes = (List<Idx>) in.readObject();
        in.close();
        System.out.println(indexes.size());
        File file = new File("/Users/zhukov/Downloads/search term 1.aif");

        int[] index2 = index2(sox(file));

        for (Idx idx : indexes) {
            int[] index22 = idx.index2;
            float sad = 0;
            for (int bin = 0; bin < index2.length; bin++) {
                int f0 = index2[bin];
                int f1 = index22[bin];
                if (f0 > 0 && f1 > 0) {
                    float diff = Math.abs(f1 - f0);
                    float weight = binWeights[bin];
                    diff *= weight;
                    sad += diff;
                } else {
                    System.out.println("ololo");
                }
            }
            idx.dist = (int) sad;
        }
        List<Idx> sort = Lambda.sort(indexes, Lambda.on(Idx.class).getDist());
        for (int i = 0; i < 10; i++) {
            System.out.println(sort.get(i));
        }

    }

    @Test
    public void testSearch3() throws Exception {
        ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(
                new FileInputStream(new File("index3.dat"))));
        List<Idx> indexes = (List<Idx>) in.readObject();
        in.close();
        System.out.println(indexes.size());
//        File file = new File("/Users/zhukov/Downloads/search term 1.aif");
        File file = new File("/Users/zhukov/Downloads/gagaga.wav");

        Idx search = index3(sox(file));

        for (Idx idx : indexes) {
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
            idx.dist = (int) sad;
        }
        List<Idx> sort = Lambda.sort(indexes, Lambda.on(Idx.class).getDist());
        for (int i = 0; i < 42; i++) {
            System.out.println(sort.get(i));
        }

    }

    public static int usadu8(ByteBuffer a1, ByteBuffer a2) {
        ByteBuffer data1 = a1;
        ByteBuffer data2 = a2;
        Assert.assertEquals(data1.capacity(), data2.capacity());
        int sad = 0;
        while (data1.hasRemaining() && data2.hasRemaining()) {
            int p1 = data1.get() & 0xff;
            int p2 = data2.get() & 0xff;
            sad += Math.abs(p1 - p2);
        }
        return sad;
    }

    public static List<File> find(File file, FileFilter filter) {
        List<File> result = new ArrayList<File>();
        LinkedList<File> stack = new LinkedList<File>();
        stack.push(file);
        while (!stack.isEmpty()) {
            File f = stack.pop();
            if ((filter == null || filter.accept(f)) && f.exists()) {
                result.add(f);
            }

            if (f.isDirectory() && f.exists()) {
                File[] listFiles = f.listFiles();
                if (listFiles != null) {
                    stack.addAll(Arrays.asList(listFiles));
                }
            }
        }
        return result;
    }

}
