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
package com.alibaba.cloud.ai.graph.node;

import java.util.Map;

import com.alibaba.cloud.ai.graph.OverAllState;

/**
 * Integration test/demo for TemplateTransformNode
 */
public class TemplateTransformNodeIntegrationTest {

	public static void main(String[] args) {

		System.out.println("=== Demo 1: Basic Template Transformation ===");
		TemplateTransformNode basicNode = TemplateTransformNode.builder()
			.template("Hello {{name}}, welcome to {{platform}}!")
			.outputKey("greeting")
			.build();

		OverAllState state1 = new OverAllState(Map.of("name", "Alice", "platform", "Spring AI Alibaba"));

		Map<String, Object> result1 = basicNode.apply(state1);
		System.out.println("Result: " + result1.get("greeting"));
		System.out.println("Expected: Hello Alice, welcome to Spring AI Alibaba!");
		System.out.println();

		System.out.println("=== Demo 2: Missing Variables ===");
		TemplateTransformNode missingVarNode = TemplateTransformNode.builder()
			.template("Available: {{found}}, Missing: {{missing}}")
			.build();

		OverAllState state2 = new OverAllState(Map.of("found", "value"));
		Map<String, Object> result2 = missingVarNode.apply(state2);
		System.out.println("Result: " + result2.get("result"));
		System.out.println("Expected: Available: value, Missing: {{missing}}");
		System.out.println();

		System.out.println("=== Demo 3: Complex Template ===");
		TemplateTransformNode complexNode = TemplateTransformNode.builder()
			.template("User {{user.name}} ({{user.email}}) has {{user.score}} points")
			.outputKey("user_summary")
			.build();

		OverAllState state3 = new OverAllState(
				Map.of("user.name", "John Doe", "user.email", "john@example.com", "user.score", 1500));

		Map<String, Object> result3 = complexNode.apply(state3);
		System.out.println("Result: " + result3.get("user_summary"));
		System.out.println("Expected: User John Doe (john@example.com) has 1500 points");
		System.out.println();

		System.out.println("=== All demos completed successfully! ===");
	}

}
