package com.alibaba.cloud.ai.studio.admin.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Version utility class
 */
@Slf4j
public class VersionUtils {

    /**
     * Generate version number for dataset
     */
    public static String generateVersionNumber(String currentVersion) {

        if (StringUtils.isBlank(currentVersion)){
            return "1.0.0";
        }

        try {
            String[] parts = currentVersion.split("\\.");
            if (parts.length >= 3) {
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]);
                int patch = Integer.parseInt(parts[2]);
                return major + "." + minor + "." + (patch + 1);
            } else {
                int major = Integer.parseInt(parts[0]);
                int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                return major + "." + (minor + 1) + ".0";
            }
        } catch (NumberFormatException e) {
            log.warn("Failed to parse version number: {}, using fallback", currentVersion, e);
            return currentVersion + "." + System.currentTimeMillis();
        }
    }

}