/**
 * 
 */
package com.vg.audio;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonFactory {

    public static Gson create() {
        return create(false);
    }

    public static <T> T gsonClone(T t) {
        if (t != null) {
            Gson create = GsonFactory.create();
            String json = create.toJson(t);
            T fromJson = (T) create.fromJson(json, t.getClass());
            return fromJson;
        }
        return null;
    }

    public static Gson create(boolean serializeNulls) {
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        if (serializeNulls)
            builder.serializeNulls();
        builder.registerTypeAdapter(File.class, new FileAdapter());
        return builder.create();
    }

    public static Gson createUgly() {
        GsonBuilder builder = new GsonBuilder();
        builder.serializeNulls();
        return builder.create();
    }

    public static String toGson(Object src) {
        return create().toJson(src);
    }

    public static <T> T fromFile(File file, Class<T> classOf) {
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            return GsonFactory.create().fromJson(IOUtils.toString(is), classOf);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            closeQuietly(is);
        }
    }

    public static <T> T fromInputStream(InputStream in, Class<T> classOf) {
        return GsonFactory.create().fromJson(new InputStreamReader(in), classOf);
    }

    public static <T> T fromFile(File file, java.lang.reflect.Type t) {
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            return GsonFactory.create().fromJson(IOUtils.toString(is), t);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            closeQuietly(is);
        }
    }

    public static String gsonToString(Object o) {
        return new GsonBuilder().create().toJson(o);
    }

}