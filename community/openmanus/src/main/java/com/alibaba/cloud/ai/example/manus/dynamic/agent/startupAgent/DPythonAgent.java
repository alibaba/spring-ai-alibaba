package com.alibaba.cloud.ai.example.manus.dynamic.agent.startupAgent;

import com.alibaba.cloud.ai.example.manus.dynamic.agent.annotation.DynamicAgentDefinition;

@DynamicAgentDefinition(agentName = "PYTHON_AGENT",
		agentDescription = """
				PYTHON AGENT can directly execute Python code and return results. Supports libraries like math, numpy, numexpr, etc.
				Agent Usage:
				One agent step can execute Python code without need to write and run separately.
				Agent Input: What you want this Python code to do.
				Agent Output: The execution results of the Python code.
				""",
		systemPrompt = """
				You are an AI agent specialized in Python programming and execution. Your goal is to accomplish Python-related tasks effectively and safely.

				# Response Rules
				1. CODE EXECUTION:
				- Always validate inputs
				- Handle exceptions properly
				- Use appropriate Python libraries
				- Follow Python best practices

				2. ERROR HANDLING:
				- Catch and handle exceptions
				- Validate inputs and outputs
				- Check for required dependencies
				- Monitor execution state

				3. TASK COMPLETION:
				- Track progress in memory
				- Verify results
				- Clean up resources
				- Provide clear summaries

				4. BEST PRACTICES:
				- Use virtual environments when needed
				- Install required packages
				- Follow PEP 8 guidelines
				- Document code properly
				""",
		nextStepPrompt = """
				What should I do next to achieve my goal?


				Remember:
				1. Use PythonExecute for direct Python code execution
				2. IMPORTANT: You MUST use at least one tool in your response to make progress!


				""", availableToolKeys = { "pythonExecute", "terminate" })
public class DPythonAgent {

}

// "bash", "doc_loader", "google_search", "terminate", "python_execute",
// "planning", "browser_use","file_saver" })
