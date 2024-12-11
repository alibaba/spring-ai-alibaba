/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.plugin.time;

import com.fasterxml.jackson.annotation.JsonClassDescription;

import java.util.TimeZone;
import java.util.function.Function;

/**
 * @author chengle
 */
public class GetCurrentLocalTimeService
		implements Function<GetCurrentLocalTimeService.Request, GetCurrentLocalTimeService.Response> {

	@Override
	public Response apply(Request request) {
		TimeZone timeZone = TimeZone.getDefault();
		return new Response(String.format("The current local time is %s", ZoneUtils.getTimeByZoneId(timeZone.getID())));
	}

	@JsonClassDescription("Request to obtain the current local time")
	public record Request() {
	}

	public record Response(String description) {
	}

}
