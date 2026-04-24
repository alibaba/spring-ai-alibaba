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
 * Stub email API tool for the supervisor personal assistant example.
 * In production this would call SendGrid, Gmail API, etc.
 */
public class EmailStubTools {

	@Tool(name = "send_email", description = "Send an email via email API. Requires properly formatted addresses.")
	public String sendEmail(
			@ToolParam(description = "List of recipient email addresses") List<String> to,
			@ToolParam(description = "Email subject") String subject,
			@ToolParam(description = "Email body") String body,
			@ToolParam(description = "CC recipients", required = false) List<String> cc) {
		return String.format("Email sent to %s - Subject: %s",
				String.join(", ", to != null ? to : List.of()), subject);
	}
}
