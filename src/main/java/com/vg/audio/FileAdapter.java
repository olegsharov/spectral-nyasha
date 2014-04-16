package com.vg.audio;

import java.io.File;
import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class FileAdapter implements JsonSerializer<File>, JsonDeserializer<File> {

    @Override
    public File deserialize(JsonElement arg0, Type arg1, JsonDeserializationContext arg2) throws JsonParseException {
        return new File(((JsonPrimitive) arg0).getAsString());
    }

    @Override
    public JsonElement serialize(File arg0, Type arg1, JsonSerializationContext arg2) {
        return new JsonPrimitive(arg0.getPath());
    }

}
