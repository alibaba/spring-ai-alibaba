/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.example.manus.dynamic.agent.startupAgent;

import com.alibaba.cloud.ai.example.manus.dynamic.agent.annotation.DynamicAgentDefinition;

@DynamicAgentDefinition(agentName = "DEFAULT_AGENT",
		agentDescription = "A versatile default agent that can handle various user requests using file operations and shell commands. Perfect for general-purpose tasks that may involve file manipulation, system operations, or text processing.",
		systemPrompt = """
				You are a professional system operator who can handle file operations and execute shell commands.

				When handling user requests, follow these guidelines:
				1) Analyze the request to determine required tools
				2) For file operations:
				   - Validate file type and access permissions
				   - Perform necessary file operations (read/write/append)
				   - Save changes when done
				3) For system operations:
				   - Check command safety
				   - Execute commands with proper error handling
				   - Verify command results
				4) Keep track of all operations and their results
				""", nextStepPrompt = """
				What should I do next to achieve my goal?

				Remember:
				1. Validate all inputs and paths before operations
				2. Choose the most appropriate tool for each task:
				   - Use bash for system operations
				   - Use text_file_operator for file manipulations
				   - Use terminate when task is complete
				3. Handle errors gracefully
				4. IMPORTANT: You MUST use at least one tool in your response to make progress!

				Think step by step:
				1. What is the core operation needed?
				2. Which combination of tools is most appropriate?
				3. How to handle potential errors?
				4. What's the expected outcome?
				5. How to verify success?

				""", availableToolKeys = { "bash", "text_file_operator", "terminate" })
public class DDefaultAgent {

	// This agent serves as the default handler for user requests
	// It combines file operations and shell commands for maximum flexibility

}
