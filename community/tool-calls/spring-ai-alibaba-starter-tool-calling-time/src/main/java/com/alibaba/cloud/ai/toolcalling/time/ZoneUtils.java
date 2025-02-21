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

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author chengle
 */

public class ZoneUtils {

	public static String getTimeByZoneId(String zoneId) {

		// Get the time zone using ZoneId
		ZoneId zid = ZoneId.of(zoneId);

		// Get the current time in this time zone
		ZonedDateTime zonedDateTime = ZonedDateTime.now(zid);

		// Defining a formatter
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

		// Format ZonedDateTime as a string
		String formattedDateTime = zonedDateTime.format(formatter);

		return formattedDateTime;
	}

}
