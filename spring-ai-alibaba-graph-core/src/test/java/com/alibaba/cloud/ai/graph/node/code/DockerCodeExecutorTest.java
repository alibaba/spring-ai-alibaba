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
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DockerCodeExecutorTest {

	@Test
	void testPython3Sum() throws Exception {
		// 1. 构造 DockerCodeExecutor
		DockerCodeExecutor executor = new DockerCodeExecutor();

		// 2. 构造代码块（Python3 求和）
		String code = """
				def main(inputs):
				    return {\"result\": sum(inputs)}
				""";
		CodeBlock codeBlock = new CodeBlock("python3", code);

		// 3. 构造执行配置
		Path workDir = Files.createTempDirectory("docker-code-exec-test");
		CodeExecutionConfig config = new CodeExecutionConfig().setDocker("python:3.10")
			.setWorkDir(workDir.toAbsolutePath().toString())
			.setContainerName("docker-code-exec-test")
			.setTimeout(60);

		// 4. 执行
		CodeExecutionResult result = executor.executeCodeBlocks(List.of(codeBlock), config);

		// 5. 断言
		assertEquals(0, result.exitCode(), "Exit code should be 0");
	}

}
