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
package com.alibaba.cloud.ai.graph.agent.hook.hip;

import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback.FeedbackResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.stream.Collectors;

public class ConsoleInteractionHandler implements HumanInteractionHandler {

	private final Scanner scanner = new Scanner(System.in);

	@Override
	public List<ToolFeedback> handleInterrupts(List<ToolFeedback> requests) {
		List<ToolFeedback> responses = new ArrayList<>();

		for (ToolFeedback request : requests) {
			System.out.println(request.getDescription());
			var decision = readDecision(request);
			var response = ToolFeedback.builder(request).result(decision);
			switch (decision) {
				case EDITED -> response.arguments(readLine("Edited arguments (JSON): "));
				case REJECTED -> response.description(readLine("Rejection reason: "));
				case RESPONDED -> response.response(readLine("Your response: "));
				case APPROVED -> { }
			}
			responses.add(response.build());
		}

		return responses;
	}

	private FeedbackResult readDecision(ToolFeedback request) {
		var allowed = request.getAllowedDecisions();
		String choices = allowed.stream().map(ConsoleInteractionHandler::choice).collect(Collectors.joining("/"));
		while (true) {
			String answer = readLine("Your decision (" + choices + "): ").trim().toLowerCase(Locale.ROOT);
			for (FeedbackResult decision : allowed) {
				if (choice(decision).equals(answer)) {
					return decision;
				}
			}
		}
	}

	private String readLine(String prompt) {
		System.out.print(prompt);
		return scanner.nextLine();
	}

	private static String choice(FeedbackResult decision) {
		return switch (decision) {
			case APPROVED -> "approve";
			case EDITED -> "edit";
			case REJECTED -> "reject";
			case RESPONDED -> "respond";
		};
	}
}
