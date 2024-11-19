package com.alibaba.cloud.ai.utils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ZoneUtils {
    public static String getTimeByZoneId(String zoneId) {

        // 使用ZoneId获取时区
        ZoneId zid = ZoneId.of(zoneId);

        // 获取该时区的当前时间
        ZonedDateTime zonedDateTime = ZonedDateTime.now(zid);

        // 定义格式化器
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

        // 将ZonedDateTime格式化为字符串
        String formattedDateTime = zonedDateTime.format(formatter);

        return formattedDateTime;
    }
}
