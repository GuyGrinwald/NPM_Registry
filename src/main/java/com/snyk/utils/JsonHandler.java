package com.snyk.utils;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * A json serializer and deserializer
 */
public class JsonHandler<T> {
    ObjectMapper objectMapper = new ObjectMapper();
    JavaType type;

    public JsonHandler(Class<T> clazz) {
        this.type = objectMapper.getTypeFactory().
                constructType(clazz);
    }

    public T deserialize(String json) throws IOException {
        return objectMapper.readerFor(type).readValue(json);
    }

    public String serialize(T t) throws IOException {
        return objectMapper.writerFor(type).writeValueAsString(t);
    }
}