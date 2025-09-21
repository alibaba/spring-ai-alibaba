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
import com.alibaba.cloud.ai.graph.node.code.entity.CodeParam;
import com.alibaba.cloud.ai.graph.node.code.entity.CodeStyle;
import com.alibaba.cloud.ai.graph.node.code.LocalCommandlineCodeExecutor;
import com.alibaba.cloud.ai.graph.node.code.entity.CodeExecutionConfig;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author HeYQ
 * @since 2024-11-28 11:49
 */
public class CodeActionTest {

	@TempDir
	Path tempDir;

	private CodeExecutionConfig config;

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
		NodeAction codeNode = CodeExecutorNodeAction.builder()
			.codeExecutor(new LocalCommandlineCodeExecutor())
			.code(code)
			.codeStyle(CodeStyle.EXPLICIT_PARAMETERS)
			.codeLanguage("python3")
			.config(config)
			.params(List.of(new CodeParam("arg1", "data1"), new CodeParam("arg2", "data2")))
			.outputKey("output")
			.build();
		OverAllState mockState = new OverAllState(Map.of("data1", "1", "data2", "2"));
		Map<String, Object> stateData = codeNode.apply(mockState);
		System.out.println(stateData);
		assertNotNull(stateData);
		assertEquals(Map.of("output", Map.of("result", "12")), stateData);
	}

	@Test
	void testExecutePythonGlobalDictStyleSuccessfully() throws Exception {
		String code = """
				def main():
				  ret = {
				      "output": params['arg1'] + params['arg2'] + params['arg3']
				  }
				  return ret
				""";
		NodeAction codeNode = CodeExecutorNodeAction.builder()
			.codeExecutor(new LocalCommandlineCodeExecutor())
			.code(code)
			.codeStyle(CodeStyle.GLOBAL_DICTIONARY)
			.codeLanguage("python3")
			.config(config)
			.params(List.of(CodeParam.withKey("arg1", "arg1"), CodeParam.withKey("arg2", "arg2"),
					CodeParam.withValue("arg3", "3")))
			.outputKey("output")
			.build();
		OverAllState mockState = new OverAllState(Map.of("arg1", "1", "arg2", "2"));
		Map<String, Object> stateData = codeNode.apply(mockState);
		assertNotNull(stateData);
		assertEquals(Map.of("output", Map.of("output", "123")), stateData);
		System.out.println(stateData);
	}

	@Test
	void testExecuteJavascriptSuccessfully() throws Exception {
		String code = """
				function main({arg1, arg2}) {
				    return {
				        result: arg1 + arg2
				    }
				}
				""";
		NodeAction codeNode = CodeExecutorNodeAction.builder()
			.codeExecutor(new LocalCommandlineCodeExecutor())
			.code(code)
			.codeStyle(CodeStyle.EXPLICIT_PARAMETERS)
			.codeLanguage("javascript")
			.config(config)
			.params(List.of(new CodeParam("arg1", "data1"), new CodeParam("arg2", "data2")))
			.outputKey("output")
			.build();
		OverAllState mockState = new OverAllState(Map.of("data1", "1", "data2", "2"));
		Map<String, Object> stateData = codeNode.apply(mockState);
		System.out.println(stateData);
		assertNotNull(stateData);
		assertEquals(Map.of("output", Map.of("result", "12")), stateData);
	}

	@Test
	void testExecuteJavascriptGlobalDictStyleSuccessfully() throws Exception {
		String code = """
				function main() {
				  const ret = {
				      "output": params.arg1 + params.arg2 + params.arg3
				  };
				  return ret;
				}
				""";
		NodeAction codeNode = CodeExecutorNodeAction.builder()
			.codeExecutor(new LocalCommandlineCodeExecutor())
			.code(code)
			.codeStyle(CodeStyle.GLOBAL_DICTIONARY)
			.codeLanguage("javascript")
			.config(config)
			.params(List.of(CodeParam.withKey("arg1", "arg1"), CodeParam.withKey("arg2", "arg2"),
					CodeParam.withValue("arg3", "3")))
			.outputKey("output")
			.build();
		OverAllState mockState = new OverAllState(Map.of("arg1", "1", "arg2", "2"));
		Map<String, Object> stateData = codeNode.apply(mockState);
		assertNotNull(stateData);
		assertEquals(Map.of("output", Map.of("output", "123")), stateData);
		System.out.println(stateData);
	}

	@Test
	void testExecuteJavaWithLocalExecutor() throws Exception {
		// Prepare test data
		String javaCode = """
				public static Object run(String arg0, Integer arg1) {
				    String text = arg0;
				    int count = arg1;
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

		// Create code execution node action
		NodeAction codeNode = CodeExecutorNodeAction.builder()
			.codeExecutor(new LocalCommandlineCodeExecutor())
			.code(javaCode)
			.codeStyle(CodeStyle.EXPLICIT_PARAMETERS)
			.codeLanguage("java")
			.config(config)
			.params(List.of(new CodeParam("arg0", "text"), new CodeParam("arg1", "count")))
			.outputKey("codeNode1_output")
			.build();

		// Prepare input data
		Map<String, Object> initData = new LinkedHashMap<>();
		initData.put("text", "Hello");
		initData.put("count", 3);
		OverAllState mockState = new OverAllState(initData);

		// Execute code
		Map<String, Object> result = codeNode.apply(mockState);
		assertNotNull(result);
		System.out.println(result);
		assertEquals(Map.of("codeNode1_output", Map.of("length", 18, "count", 3, "repeated_text", "Hello Hello Hello")),
				result);
	}

	@Test
	void testExecuteJavaGlobalDictStyleWithLocalExecutor() throws Exception {
		// Prepare test data
		String javaCode = """
				public static Object run() {
				    String text = (String) params.get("arg0");
				    int count = (Integer) params.get("arg1");
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

		// Create code execution node action
		NodeAction codeNode = CodeExecutorNodeAction.builder()
			.codeExecutor(new LocalCommandlineCodeExecutor())
			.code(javaCode)
			.codeLanguage("java")
			.config(config)
			.codeStyle(CodeStyle.GLOBAL_DICTIONARY)
			.params(List.of(new CodeParam("arg0", "text"), new CodeParam("arg1", "count")))
			.outputKey("codeNode1_output")
			.build();

		// Prepare input data
		Map<String, Object> initData = new LinkedHashMap<>();
		initData.put("text", "Hello");
		initData.put("count", 3);
		OverAllState mockState = new OverAllState(initData);

		// Execute code
		Map<String, Object> result = codeNode.apply(mockState);
		assertNotNull(result);
		System.out.println(result);
		assertEquals(Map.of("codeNode1_output", Map.of("length", 18, "count", 3, "repeated_text", "Hello Hello Hello")),
				result);
	}

}
