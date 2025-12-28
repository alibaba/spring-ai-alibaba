package com.alibaba.cloud.ai.studio.admin.utils;

import com.alibaba.fastjson.JSONArray;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class CommonUtils {

    public static List<Long> parseItemIds(String itemIds) {
        if (itemIds == null || itemIds.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            JSONArray jsonArray = JSONArray.parseArray(itemIds);
            return jsonArray.stream()
                    .map(obj -> Long.valueOf(obj.toString()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("解析itemIds字符串失败: {}", itemIds, e);
            return new ArrayList<>();
        }
    }


    public static String extractRawText(String markdownCode) {
        // Find the start of a code block (3 or more backticks)
        int startIndex = -1;
        int delimiterLength = 0;

        for (int i = 0; i <= markdownCode.length() - 3; i++) {
            if (markdownCode.substring(i, i + 3).equals("```")) {
                startIndex = i;
                delimiterLength = 3;
                // Count additional backticks
                while (i + delimiterLength < markdownCode.length() && markdownCode.charAt(i + delimiterLength) == '`') {
                    delimiterLength++;
                }
                break;
            }
        }

        if (startIndex == -1) {
            return markdownCode; // No code block found
        }

        // Skip the opening delimiter and optional language specification
        int contentStart = startIndex + delimiterLength;
        while (contentStart < markdownCode.length() && markdownCode.charAt(contentStart) != '\n') {
            contentStart++;
        }
        if (contentStart < markdownCode.length() && markdownCode.charAt(contentStart) == '\n') {
            contentStart++; // Skip the newline after language spec
        }

        // Find the closing delimiter
        String closingDelimiter = "`".repeat(delimiterLength);
        int endIndex = markdownCode.indexOf(closingDelimiter, contentStart);

        if (endIndex == -1) {
            // No closing delimiter found, return from content start to end
            return markdownCode.substring(contentStart);
        }

        // Extract just the content between delimiters
        return markdownCode.substring(contentStart, endIndex);
    }

}
