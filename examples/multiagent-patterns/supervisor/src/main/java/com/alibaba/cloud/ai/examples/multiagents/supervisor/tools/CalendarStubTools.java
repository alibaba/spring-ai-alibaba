/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.examples.multiagents.supervisor.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;

/**
 * Stub calendar API tools for the supervisor personal assistant example.
 * In production these would call Google Calendar API, Outlook API, etc.
 */
public class CalendarStubTools {

	@Tool(name = "create_calendar_event", description = "Create a calendar event. Requires exact ISO datetime format.")
	public String createCalendarEvent(
			@ToolParam(description = "Event title") String title,
			@ToolParam(description = "Start time in ISO format, e.g. 2024-01-15T14:00:00") String startTime,
			@ToolParam(description = "End time in ISO format, e.g. 2024-01-15T15:00:00") String endTime,
			@ToolParam(description = "List of attendee email addresses") List<String> attendees,
			@ToolParam(description = "Event location", required = false) String location) {
		return String.format("Event created: %s from %s to %s with %d attendees",
				title, startTime, endTime, attendees != null ? attendees.size() : 0);
	}

	@Tool(name = "get_available_time_slots", description = "Check calendar availability for given attendees on a specific date.")
	public String getAvailableTimeSlots(
			@ToolParam(description = "List of attendee email addresses") List<String> attendees,
			@ToolParam(description = "Date in ISO format, e.g. 2024-01-15") String date,
			@ToolParam(description = "Duration in minutes") int durationMinutes) {
		return "[\"09:00\", \"14:00\", \"16:00\"]";
	}
}
