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

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ConsoleInteractionHandler implements HumanInteractionHandler {
	@Override
	public List<ToolFeedback> handleInterrupts(List<ToolFeedback> requests) {
		List<ToolFeedback> responses = new ArrayList<>();
		Scanner scanner = new Scanner(System.in);

		for (ToolFeedback request : requests) {
			System.out.println("\n" + "=".repeat(50));
			System.out.println("HUMAN INTERVENTION REQUIRED");
			System.out.println("=".repeat(50));
			System.out.println(request.getDescription());
			System.out.println("\nAvailable actions:");

			System.out.println("- 'accept': Approve the action as-is");
			System.out.println("- 'edit': Approve with modifications");
			System.out.println("- 'respond': Reject with feedback");

			System.out.print("\nYour choice: ");
			String choice = scanner.nextLine().trim().toLowerCase();

		}

		return responses;
	}
}
