package com.alibaba.cloud.ai.example.manus.recorder.entity;

/**
 * Interface providing JSON serialization capabilities.
 * Classes implementing this interface should provide a method to convert their data to JSON format.
 */
public interface JsonSerializable {
    
    /**
     * Converts the object to its JSON representation.
     * 
     * @return A string containing the JSON representation of the object
     */
    String toJson();
    
    /**
     * Escape special characters for JSON string.
     * This is a default method that can be used by all implementing classes.
     * 
     * @param input String to escape
     * @return Escaped string
     */
    default String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\b", "\\b")
                   .replace("\f", "\\f")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }


    /**
     * Helper method to append a field to the JSON string.
     * 
     * @param json StringBuilder to append to
     * @param fieldName Name of the field
     * @param value Value of the field
     * @param isString Whether the value is a string (needing quotes)
     */
    default void appendField(StringBuilder json, String fieldName, Object value, boolean isString) {
        if (value != null) {
            json.append("\"").append(fieldName).append("\":");
            if (isString) {
                json.append("\"").append(escapeJson(value.toString())).append("\"");
            } else {
                json.append(value);
            }
            json.append(",");
        }
    }
}