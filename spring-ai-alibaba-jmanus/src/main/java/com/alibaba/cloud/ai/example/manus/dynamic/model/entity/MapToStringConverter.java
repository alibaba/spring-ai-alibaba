package com.alibaba.cloud.ai.example.manus.dynamic.model.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Map;

/**
 * @author dahua
 * @date 2025/7/12 13:02
 */
@Converter
public class MapToStringConverter implements AttributeConverter<Map<String, String>, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, String> attribute) {
        try {
            return objectMapper.writeValueAsString(attribute);
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Error converting map to string", e);
        }
    }

    @Override
    public Map<String, String> convertToEntityAttribute(String dbData) {
        try {
            return objectMapper.readValue(dbData, new TypeReference<>() {
            });
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Error converting string to map", e);
        }
    }

}