/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.examples.chatbot;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.function.BiFunction;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tool for executing Python code using GraalVM polyglot.
 *
 * This tool allows the agent to execute Python code snippets and get results.
 * It uses GraalVM's polyglot API to run Python code in a sandboxed environment.
 */
public class PythonTool implements BiFunction<PythonTool.PythonRequest, ToolContext, String> {

	public static final String DESCRIPTION = """
			Executes Python code and returns the result.
			
			Usage:
			- The code parameter must be valid Python code
			- The tool will execute the code and return the output
			- If the code produces a result, it will be returned as a string
			- Errors will be caught and returned as error messages
			- The execution is sandboxed for security
			
			Examples:
			- Simple calculation: code = "2 + 2" returns "4"
			- String operations: code = "'Hello, ' + 'World'" returns "Hello, World"
			- List operations: code = "[1, 2, 3][0]" returns "1"
			""";
	private static final Logger log = LoggerFactory.getLogger(PythonTool.class);
	private final Engine engine;

	public PythonTool() {
		// Create a shared engine for better performance
		this.engine = Engine.newBuilder()
				.option("engine.WarnInterpreterOnly", "false")
				.build();
	}

	/**
	 * Create a ToolCallback for the Python tool.
	 */
	public static ToolCallback createPythonToolCallback(String description) {
		return FunctionToolCallback.builder("python", new PythonTool())
				.description(description)
				.inputType(PythonRequest.class)
				.build();
	}

	@Override
	public String apply(PythonRequest request, ToolContext toolContext) {
		if (request.code == null || request.code.trim().isEmpty()) {
			return "Error: Python code cannot be empty";
		}

		try (Context context = Context.newBuilder("python")
				.engine(engine)
				.allowAllAccess(false) // Security: restrict access by default
				.allowIO(false) // Disable file I/O for security
				.allowNativeAccess(false) // Disable native access for security
				.allowCreateProcess(false) // Disable process creation for security
				.allowHostAccess(true) // Allow access to host objects
				.build()) {

			log.debug("Executing Python code: {}", request.code);

			// Execute the Python code
			Value result = context.eval("python", request.code);

			// Convert result to string
			if (result.isNull()) {
				return "Execution completed with no return value";
			}

			// Handle different result types
			if (result.isString()) {
				return result.asString();
			}
			else if (result.isNumber()) {
				return String.valueOf(result.as(Object.class));
			}
			else if (result.isBoolean()) {
				return String.valueOf(result.asBoolean());
			}
			else if (result.hasArrayElements()) {
				// Convert array/list to string representation
				StringBuilder sb = new StringBuilder("[");
				long size = result.getArraySize();
				for (long i = 0; i < size; i++) {
					if (i > 0) {
						sb.append(", ");
					}
					Value element = result.getArrayElement(i);
					sb.append(element.toString());
				}
				sb.append("]");
				return sb.toString();
			}
			else {
				// For other types, use toString()
				return result.toString();
			}
		}
		catch (PolyglotException e) {
			log.error("Error executing Python code", e);
			return "Error executing Python code: " + e.getMessage();
		}
		catch (Exception e) {
			log.error("Unexpected error executing Python code", e);
			return "Unexpected error: " + e.getMessage();
		}
	}

	/**
	 * Request structure for the Python tool.
	 */
	public static class PythonRequest {

		@JsonProperty(required = true)
		@JsonPropertyDescription("The Python code to execute")
		public String code;

		public PythonRequest() {
		}

		public PythonRequest(String code) {
			this.code = code;
		}
	}
}

