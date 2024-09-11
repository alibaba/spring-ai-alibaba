/*
 * All rights Reserved, Designed By Alibaba Group Inc.
 * Copyright: Copyright(C) 1999-2024
 * Company  : Alibaba Group Inc.
 */
package com.alibaba.cloud.ai.dashscope.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.function.Function;

/**
 * Title Mock weather service.<br>
 * Description Mock weather service.<br>
 * Created at 2024-09-03 10:20
 *
 * @author yuanci.ytb
 * @version 1.0.0
 * @since jdk8
 */

public class MockWeatherService implements Function<MockWeatherService.Request, MockWeatherService.Response> {

	@Override
	public Response apply(Request request) {
		if (request.city().contains("杭州")) {
			return new Response(String.format("%s%s晴转多云, 气温32摄氏度。", request.date(), request.city()));
		}
		else if (request.city().contains("上海")) {
			return new Response(String.format("%s%s多云转阴, 气温31摄氏度。", request.date(), request.city()));
		}
		else {
			return new Response(String.format("暂时无法查询%s的天气状况。", request.city()));
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonClassDescription("根据日期和城市查询天气")
	public record Request(
			@JsonProperty(required = true, value = "city") @JsonPropertyDescription("城市, 比如杭州") String city,
			@JsonProperty(required = true, value = "date") @JsonPropertyDescription("日期, 比如2024-08-22") String date) {
	}

	public record Response(String description) {
	}

}
