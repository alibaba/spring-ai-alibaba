/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent.tools;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.function.BiFunction;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HotelTool implements BiFunction<HotelTool.HotelBookingRequest, ToolContext, String> {
	public int counter = 0;

	public HotelTool() {
	}

	@Override
	public String apply(HotelBookingRequest request, ToolContext toolContext) {
		counter++;
		System.out.println("Hotel tool called : " + request.name + ", date: " + request.date);
		return "Hotel booked for " + request.name + " on " + request.date;
	}

	public static ToolCallback createHotelTool(String name, HotelTool ticketTool) {
		return FunctionToolCallback.builder(name, ticketTool)
				.description("Hotel booking tool")
				.inputType(HotelBookingRequest.class)
				.build();
	}

	/**
	 * Request object for ticket booking containing name and date.
	 */
	public static class HotelBookingRequest {
		@JsonProperty(required = true, value = "user name for the hotel booking")
		private String name;

		@JsonProperty(required = true, value = "date for the hotel booking")
		private String date;

		public HotelBookingRequest() {
		}

		public HotelBookingRequest(String name, String date) {
			this.name = name;
			this.date = date;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDate() {
			return date;
		}

		public void setDate(String date) {
			this.date = date;
		}
	}

}
