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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.function.BiFunction;

public class TicketTool implements BiFunction<TicketTool.TicketRequest, ToolContext, String> {
	public int counter = 0;

	public TicketTool() {
	}

	@Override
	public String apply(TicketRequest request, ToolContext toolContext) {
		counter++;
		System.out.println("Ticket tool called : " + request.name + ", date: " + request.date);
		return "Ticket booked for " + request.name + " on " + request.date;
	}

	public static ToolCallback createTicketToolCallback(String name, TicketTool ticketTool) {
		return FunctionToolCallback.builder(name, ticketTool)
				.description("Ticket booking tool")
				.inputType(TicketRequest.class)
				.build();
	}

	/**
	 * Request object for ticket booking containing name and date.
	 */
	public static class TicketRequest {
		@JsonProperty(required = true, value = "user name for the ticket")
		private String name;

		@JsonProperty(required = true, value = "date for the ticket")
		private String date;

		public TicketRequest() {
		}

		public TicketRequest(String name, String date) {
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
