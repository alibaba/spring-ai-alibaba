/*
 * Copyright 2023-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.example.cli.clidebugexample.function;

import com.alibaba.cloud.ai.example.cli.clidebugexample.entity.Response;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.function.Function;

/**
 * Title Mock weather service.<br>
 * Description Mock weather service.<br>
 *
 * @author yuanci.ytb
 * @since 2024/8/16 11:29
 */
public class MockWeatherService implements Function<MockWeatherService.Request, Response> {

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

}
