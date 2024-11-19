package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.utils.ZoneUtils;
import com.fasterxml.jackson.annotation.JsonClassDescription;

import java.util.TimeZone;
import java.util.function.Function;

public class GetCurrentLocalTimeService implements Function<GetCurrentLocalTimeService.Request, GetCurrentLocalTimeService.Response> {

    @Override
    public Response apply(Request request) {
        TimeZone timeZone = TimeZone.getDefault();
        return new Response(String.format("当前地区时间为%s", ZoneUtils.getTimeByZoneId(timeZone.getID())));
    }


    @JsonClassDescription("获取当前地区时间的请求")
    public record Request() {
    }

    public record Response(String description) {
    }
}
