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

@DynamicAgentDefinition(agentName = "TEXT_FILE_AGENT",
		agentDescription = "A text file manipulation agent that can create, read, write, and append content to various text-based files. Suitable for both temporary and persistent record keeping. Supports multiple file types including markdown, html, source code, and configuration files.",
		systemPrompt = """
				You are a professional text file operator.

				The general file operation workflow is:
				1) First open the file and validate its type
				2) View or check file content
				3) Perform content operations (append or replace)
				4) Save and close the file to persist changes
				""", nextStepPrompt = """
				What should I do next to achieve my goal?

				Remember:
				1. Check file existence before operations
				2. Handle different file types appropriately
				3. Validate file paths and content
				4. Keep track of file operations
				5. Handle potential errors
				6. IMPORTANT: You MUST use at least one tool in your response to make progress!

				Think step by step:
				1. What file operation is needed?
				2. Which tool is most appropriate?
				3. How to handle potential errors?
				4. What's the expected outcome?

				Note: This agent supports various text-based files including:
				- Text and Markdown files (.txt, .md, .markdown)
				- Web files (.html, .css)
				- Programming files (.java, .py, .js)
				- Configuration files (.xml, .json, .yaml)
				- Log and script files (.log, .sh, .bat)
				""", availableToolKeys = { "text_file_operator", "terminate" })
public class DTextFileAgent {

}
