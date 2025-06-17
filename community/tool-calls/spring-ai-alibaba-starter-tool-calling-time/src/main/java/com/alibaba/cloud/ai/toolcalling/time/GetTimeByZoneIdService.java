/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.toolcalling.time;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.function.Function;

/**
 * @author chengle
 */
public class GetTimeByZoneIdService
		implements Function<GetTimeByZoneIdService.Request, GetTimeByZoneIdService.Response> {

	@Override
	public GetTimeByZoneIdService.Response apply(GetTimeByZoneIdService.Request request) {
		String timeZoneId = request.timeZoneId;
		return new Response(String.format("The current time zone is %s and the current time is " + "%s", timeZoneId,
				TimeUtils.getTimeByZoneId(timeZoneId)));
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonClassDescription("Get the current time based on time zone id")
	public record Request(@JsonProperty(required = true, value = "timeZoneId") @JsonPropertyDescription("Time "
			+ "zone id, such as Asia/Shanghai") String timeZoneId) {
	}

	@JsonClassDescription("TimeByTimeZoneIdService response")
	public record Response(String description) {
	}

}
