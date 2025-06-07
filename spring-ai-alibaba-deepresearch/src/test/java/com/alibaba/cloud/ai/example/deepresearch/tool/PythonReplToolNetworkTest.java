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

package com.alibaba.cloud.ai.example.deepresearch.tool;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static com.alibaba.cloud.ai.example.deepresearch.tool.PythonReplToolBasisTest.CODE_WITH_DEPENDENCY;
import static com.alibaba.cloud.ai.example.deepresearch.tool.PythonReplToolBasisTest.NETWORK_CHECK;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Run Python Code in Docker Test With Network
 *
 * @author vlsmb
 */
@SpringBootTest
@DisplayName("Run Python Code in Docker Test With Network")
@ActiveProfiles("python_network")
public class PythonReplToolNetworkTest {

	@Autowired
	private PythonReplTool pythonReplTool;

	@Test
	@DisplayName("Run Code with Third-parties Installed")
	public void testCodeWithDependency() {
		String response = pythonReplTool.executePythonCode(CODE_WITH_DEPENDENCY, "numpy==2.2.6");
		System.out.println(response);
		assertThat(response).contains("Successfully executed").doesNotContain("ModuleNotFoundError");
	}

	@Test
	@DisplayName("Check Network is Enabled")
	public void testNetworkCheck() {
		String response = pythonReplTool.executePythonCode(NETWORK_CHECK, null);
		System.out.println(response);
		assertThat(response).doesNotContain("Failed").contains("Connected");
	}

}
