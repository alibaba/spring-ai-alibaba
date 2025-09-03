package com.alibaba.cloud.ai.memory.redis.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.ai.content.Media;

import java.io.IOException;
import java.util.Base64;

/**
 * Robust serializer for Spring AI Media:
 * - byte[] -> dataBase64
 * - URI / String -> uri
 */
public class MediaSerializer extends JsonSerializer<Media> {
    @Override
    public void serialize(Media v, JsonGenerator g, SerializerProvider p) throws IOException {
        g.writeStartObject();

        if (v.getMimeType() != null) {
            g.writeStringField("mimeType", v.getMimeType().toString());
        }

        Object data = v.getData();
        if (data instanceof byte[] bytes) {
            g.writeStringField("dataBase64", Base64.getEncoder().encodeToString(bytes));
        } else if (data != null) {
            g.writeStringField("uri", data.toString());
        } else {
            g.writeNullField("uri");
        }

        g.writeEndObject();
    }
}
