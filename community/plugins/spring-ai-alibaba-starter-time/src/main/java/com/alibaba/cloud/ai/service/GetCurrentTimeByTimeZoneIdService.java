package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.utils.ZoneUtils;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.function.Function;

public class GetCurrentTimeByTimeZoneIdService
		implements Function<GetCurrentTimeByTimeZoneIdService.Request, GetCurrentTimeByTimeZoneIdService.Response> {

	@Override
	public GetCurrentTimeByTimeZoneIdService.Response apply(GetCurrentTimeByTimeZoneIdService.Request request) {
		String timeZoneId = request.timeZoneId;
		return new Response(String.format("The current time zone is %s and the current time is %s", timeZoneId,
				ZoneUtils.getTimeByZoneId(timeZoneId)));
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonClassDescription("Get the current time based on time zone id")
	public record Request(@JsonProperty(required = true,
			value = "timeZoneId") @JsonPropertyDescription("Time zone id, such as Asia/Shanghai") String timeZoneId) {
	}

	public record Response(String description) {
	}

}
