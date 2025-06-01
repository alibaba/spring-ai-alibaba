package com.alibaba.cloud.ai.graph.utils;

/**
 * @author HeYQ
 * @since 2025-06-01 20:35
 */

public class CodeUtils {

    public static String getExecutableForLanguage(String language) throws Exception {
        return switch (language) {
            case "python3", "python" -> language;
            case "shell", "bash", "sh", "powershell" -> "sh";
            case "nodejs" -> "node";
            default -> throw new Exception("Language not recognized in code execution:" + language);
        };
    }

    public static String getFileExtForLanguage(String language) throws Exception {
        return switch (language) {
            case "python3", "python" -> "py";
            case "shell", "bash", "sh", "powershell" -> "sh";
            case "nodejs" -> "js";
            default -> throw new Exception("Language not recognized in code execution:" + language);
        };
    }
}
