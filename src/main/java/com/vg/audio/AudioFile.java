package com.vg.audio;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;

public class AudioFile {
    public int sampleRate;
    public float[] signal;
    public File file;

    public static AudioFile sox(File f) throws IOException, InterruptedException {
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
}