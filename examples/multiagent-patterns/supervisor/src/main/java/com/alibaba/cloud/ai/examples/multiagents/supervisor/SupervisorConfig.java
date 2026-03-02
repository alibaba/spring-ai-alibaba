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
package com.alibaba.cloud.ai.examples.multiagents.supervisor;

import com.alibaba.cloud.ai.examples.multiagents.supervisor.tools.CalendarStubTools;
import com.alibaba.cloud.ai.examples.multiagents.supervisor.tools.EmailStubTools;
import com.alibaba.cloud.ai.graph.agent.AgentTool;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the supervisor personal assistant: stub tools, calendar agent, email agent,
 * and supervisor agent. Specialized agents (calendar, email) are wrapped as tools via {@link AgentTool} and
 * configured with {@link ReactAgent#instruction(String)} and {@link ReactAgent.Builder#inputType(java.lang.reflect.Type)}.
 */
@Configuration
public class SupervisorConfig {

	private static final String CALENDAR_AGENT_INSTRUCTION = """
			You are a calendar scheduling assistant. \
			Parse natural language scheduling requests (e.g., 'next Tuesday at 2pm') \
			into proper ISO datetime formats. \
			Use get_available_time_slots to check availability when needed. \
			Use create_calendar_event to schedule events. \
			Always confirm what was scheduled in your final response.
			""";

	private static final String EMAIL_AGENT_INSTRUCTION = """
			You are an email assistant. \
			Compose professional emails based on natural language requests. \
			Extract recipient information and craft appropriate subject lines and body text. \
			Use send_email to send the message. \
			Always confirm what was sent in your final response.
			""";

	private static final String SUPERVISOR_INSTRUCTION = """
			You are a helpful personal assistant. \
			You can schedule calendar events and send emails. \
			Break down user requests into appropriate tool calls and coordinate the results. \
			When a request involves multiple actions, use multiple tools in sequence.
			""";

	private static final String SCHEDULE_EVENT_DESCRIPTION = """
			Schedule calendar events using natural language.
			Use this when the user wants to create, modify, or check calendar appointments.
			Handles date/time parsing, availability checking, and event creation.
			Input: Natural language scheduling request (e.g., 'meeting with design team next Tuesday at 2pm')
			""";

	private static final String MANAGE_EMAIL_DESCRIPTION = """
			Send emails using natural language.
			Use this when the user wants to send notifications, reminders, or any email communication.
			Handles recipient extraction, subject generation, and email composition.
			Input: Natural language email request (e.g., 'send them a reminder about the meeting')
			""";

	@Bean
	public CalendarStubTools calendarStubTools() {
		return new CalendarStubTools();
	}

	@Bean
	public EmailStubTools emailStubTools() {
		return new EmailStubTools();
	}

	@Bean
	public ReactAgent calendarAgent(ChatModel chatModel, CalendarStubTools calendarStubTools) {
		return ReactAgent.builder()
				.name("schedule_event")
				.description(SCHEDULE_EVENT_DESCRIPTION)
				.instruction(CALENDAR_AGENT_INSTRUCTION)
				.model(chatModel)
				.methodTools(calendarStubTools)
				.inputType(String.class)
				.build();
	}

	@Bean
	public ReactAgent emailAgent(ChatModel chatModel, EmailStubTools emailStubTools) {
		return ReactAgent.builder()
				.name("manage_email")
				.description(MANAGE_EMAIL_DESCRIPTION)
				.instruction(EMAIL_AGENT_INSTRUCTION)
				.model(chatModel)
				.methodTools(emailStubTools)
				.inputType(String.class)
				.build();
	}

	@Bean
	public ReactAgent supervisorAgent(ChatModel chatModel, ReactAgent calendarAgent, ReactAgent emailAgent) {
		return ReactAgent.builder()
				.name("personal_assistant")
				.instruction(SUPERVISOR_INSTRUCTION)
				.model(chatModel)
				.tools(
						AgentTool.getFunctionToolCallback(calendarAgent),
						AgentTool.getFunctionToolCallback(emailAgent))
				.build();
	}
}
