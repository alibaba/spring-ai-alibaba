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
package com.alibaba.cloud.ai.graph.node.code;

import com.alibaba.cloud.ai.graph.node.code.entity.CodeBlock;
import com.alibaba.cloud.ai.graph.node.code.entity.CodeExecutionConfig;
import com.alibaba.cloud.ai.graph.node.code.entity.CodeExecutionResult;
import com.alibaba.cloud.ai.graph.node.code.entity.RunnerAndPreload;
import com.alibaba.cloud.ai.graph.node.code.javascript.NashornTemplateTransformer;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InJvmCodeExecutorTest {

	@Test
	void nashornMainTest() throws Exception {

		NashornTemplateTransformer nashornTemplateTransformer = new NashornTemplateTransformer();

		String rawCode = """
				function main(args) {
				    var result = args;
				    console.log(result);
				    return result[0] + 1
				}
				""";

		List<Object> inputs = List.of(1, 2, 3);

		RunnerAndPreload runnerAndPreload = nashornTemplateTransformer.transformCaller(rawCode, inputs);

		// 1. 构造 DockerCodeExecutor
		InJvmCodeExecutor executor = new InJvmCodeExecutor();

		CodeBlock codeBlock = new CodeBlock("nashorn", runnerAndPreload.runnerScript(), inputs);

		// 3. 构造执行配置
		CodeExecutionConfig config = new CodeExecutionConfig();

		// 4. 执行
		CodeExecutionResult result = executor.executeCodeBlocks(List.of(codeBlock), config);

		// 5. 断言
		assertEquals(0, result.exitCode(), result.toString());

		assertNotNull(result.result().get(0));

	}

	@Test
	void nashornRawTest() throws Exception {
		NashornTemplateTransformer nashornTemplateTransformer = new NashornTemplateTransformer();
		String rawCode = """
				1 + 1
				""";
		List<Object> inputs = List.of(1, 2, 3);

		RunnerAndPreload runnerAndPreload = nashornTemplateTransformer.transformCaller(rawCode, inputs);

		// 1. 构造 DockerCodeExecutor
		InJvmCodeExecutor executor = new InJvmCodeExecutor();

		CodeBlock codeBlock = new CodeBlock("nashorn", runnerAndPreload.runnerScript(), inputs);

		// 3. 构造执行配置
		CodeExecutionConfig config = new CodeExecutionConfig();

		// 4. 执行
		CodeExecutionResult result = executor.executeCodeBlocks(List.of(codeBlock), config);

		// 5. 断言
		assertEquals(0, result.exitCode(), result.toString());

		assertNotNull(result.result().get(0));

	}

	@Test
	void groovyMainTest() throws Exception {
		NashornTemplateTransformer nashornTemplateTransformer = new NashornTemplateTransformer();

		String rawCode = """
				def main(args) {
				    def result = args
				    println(result)
				    return result[0] + 1
				}
				""";

		List<Object> inputs = List.of(1, 2, 3);

		RunnerAndPreload runnerAndPreload = nashornTemplateTransformer.transformCaller(rawCode, inputs);

		// 1. 构造 DockerCodeExecutor
		InJvmCodeExecutor executor = new InJvmCodeExecutor();

		CodeBlock codeBlock = new CodeBlock("groovy", runnerAndPreload.runnerScript(), inputs);

		// 3. 构造执行配置
		CodeExecutionConfig config = new CodeExecutionConfig();

		// 4. 执行
		CodeExecutionResult result = executor.executeCodeBlocks(List.of(codeBlock), config);

		// 5. 断言
		assertEquals(0, result.exitCode(), result.toString());

		assertNotNull(result.result().get(0));

	}

	@Test
	void groovyWithClassTest() throws Exception {

		NashornTemplateTransformer nashornTemplateTransformer = new NashornTemplateTransformer();

		String rawCode = """
				class A {
				    def main(args) {
				        def result = args
				        println(result)
				        return result[0] + 1
				    }
				}
				new A().main(args)
				""";

		List<Object> inputs = List.of(1, 2, 3);

		RunnerAndPreload runnerAndPreload = nashornTemplateTransformer.transformCaller(rawCode, inputs);

		// 1. 构造 DockerCodeExecutor
		InJvmCodeExecutor executor = new InJvmCodeExecutor();

		CodeBlock codeBlock = new CodeBlock("groovy", runnerAndPreload.runnerScript(), inputs);

		// 3. 构造执行配置
		CodeExecutionConfig config = new CodeExecutionConfig();

		// 4. 执行
		CodeExecutionResult result = executor.executeCodeBlocks(List.of(codeBlock), config);

		// 5. 断言
		assertEquals(0, result.exitCode(), result.toString());

		assertNotNull(result.result().get(0));

	}

	@Test
	void groovyReturnTest() throws Exception {

		NashornTemplateTransformer nashornTemplateTransformer = new NashornTemplateTransformer();

		String rawCode = """
				println("Hello World");
				println(args);
				return args
				""";

		List<Object> inputs = List.of(1, 2, 3);

		RunnerAndPreload runnerAndPreload = nashornTemplateTransformer.transformCaller(rawCode, inputs);

		// 1. 构造 DockerCodeExecutor
		InJvmCodeExecutor executor = new InJvmCodeExecutor();

		CodeBlock codeBlock = new CodeBlock("groovy", runnerAndPreload.runnerScript(), inputs);

		// 3. 构造执行配置
		CodeExecutionConfig config = new CodeExecutionConfig();

		// 4. 执行
		CodeExecutionResult result = executor.executeCodeBlocks(List.of(codeBlock), config);

		// 5. 断言
		assertEquals(0, result.exitCode(), result.toString());

		assertNotNull(result.result().get(0));

	}

	@Test
	void groovyRawTest() throws Exception {
		NashornTemplateTransformer nashornTemplateTransformer = new NashornTemplateTransformer();
		String rawCode = """
				1 + 1
				""";
		List<Object> inputs = List.of(1, 2, 3);

		RunnerAndPreload runnerAndPreload = nashornTemplateTransformer.transformCaller(rawCode, inputs);

		// 1. 构造 DockerCodeExecutor
		InJvmCodeExecutor executor = new InJvmCodeExecutor();

		CodeBlock codeBlock = new CodeBlock("groovy", runnerAndPreload.runnerScript(), inputs);

		// 3. 构造执行配置
		CodeExecutionConfig config = new CodeExecutionConfig();

		// 4. 执行
		CodeExecutionResult result = executor.executeCodeBlocks(List.of(codeBlock), config);

		// 5. 断言
		assertEquals(0, result.exitCode(), result.toString());

		assertNotNull(result.result().get(0));

	}

}
