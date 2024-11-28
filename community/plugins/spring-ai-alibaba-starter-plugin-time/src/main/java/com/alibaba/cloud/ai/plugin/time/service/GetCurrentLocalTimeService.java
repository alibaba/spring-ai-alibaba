package com.alibaba.cloud.ai.plugin.time.service;

import com.alibaba.cloud.ai.plugin.time.utils.TimeZoneUtils;
import com.fasterxml.jackson.annotation.JsonClassDescription;

import java.util.TimeZone;
import java.util.function.Function;

/**
 * @author chengle
 *
 */
public class GetCurrentLocalTimeService
		implements Function<GetCurrentLocalTimeService.Request, GetCurrentLocalTimeService.Response> {

	@Override
	public Response apply(Request request) {
		TimeZone timeZone = TimeZone.getDefault();
		return new Response(
				String.format("The current local time is %s", TimeZoneUtils.getTimeByZoneId(timeZone.getID())));
	}

	@JsonClassDescription("Request to obtain the current local time")
	public record Request() {
	}

	public record Response(String description) {
	}

}
