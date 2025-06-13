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

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.node.code.CodeExecutorNodeAction;
import com.alibaba.cloud.ai.graph.node.code.LocalCommandlineCodeExecutor;
import com.alibaba.cloud.ai.graph.node.code.entity.CodeExecutionConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author HeYQ
 * @since 2024-11-28 11:49
 */
public class CodeActionTest {

	private CodeExecutionConfig config;

	@TempDir
	Path tempDir;

	@BeforeEach
	void setUp() {
		// set up the configuration for each test
		config = new CodeExecutionConfig().setWorkDir(tempDir.toString());
	}

	@Test
	void testExecutePythonSuccessfully() throws Exception {
		String code = """
				def main(arg1: str, arg2: str) -> dict:
				    return {
				        "result": arg1 + arg2,
				    }
				""";
		Map<String, String> params = new HashMap<>(16);
		params.put("key1", "arg1");
		params.put("key2", "arg2");
		NodeAction codeNode = CodeExecutorNodeAction.builder()
			.codeExecutor(new LocalCommandlineCodeExecutor())
			.code(code)
			.codeLanguage("python")
			.config(config)
			.params(params)
			.build();
		Map<String, Object> initData = new HashMap<>(16);
		initData.put("arg1", "1");
		initData.put("arg2", "2");
		OverAllState mockState = new OverAllState(initData);
		Map<String, Object> stateData = codeNode.apply(mockState);
		System.out.println(stateData);
	}

	@Test
	void testExecuteJavaWithLocalExecutor() throws Exception {
		// Prepare test data
		String javaCode = """
				public static Object main(Object[] inputs) {
					// Process input parameters
					String text = (String) inputs[0];
					Integer count = (Integer) inputs[1];

					// Execute business logic
					StringBuilder result = new StringBuilder();
					for (int i = 0; i < count; i++) {
						result.append(text).append(" ");
					}

					Map<String, Object> response = new HashMap<>();
					response.put("repeated_text", result.toString().trim());
					response.put("length", result.length());
					response.put("count", count);

					return response;
				}
				""";

		// Create parameter mapping
		Map<String, String> params = new LinkedHashMap<>();
		params.put("text", "text");
		params.put("count", "count");
		// Create code execution node action
		NodeAction codeNode = CodeExecutorNodeAction.builder()
			.codeExecutor(new LocalCommandlineCodeExecutor())
			.code(javaCode)
			.codeLanguage("java")
			.config(config)
			.params(params)
			.build();

		// Prepare input data
		Map<String, Object> initData = new LinkedHashMap<>();
		initData.put("text", "Hello");
		initData.put("count", 3);
		OverAllState mockState = new OverAllState(initData);

		// Execute code
		Map<String, Object> result = codeNode.apply(mockState);

		// Verify results
		assertNotNull(result);
		assertEquals("Hello Hello Hello", result.get("repeated_text"));
		assertEquals(18, result.get("length"));
		assertEquals(3, result.get("count"));
	}

}
