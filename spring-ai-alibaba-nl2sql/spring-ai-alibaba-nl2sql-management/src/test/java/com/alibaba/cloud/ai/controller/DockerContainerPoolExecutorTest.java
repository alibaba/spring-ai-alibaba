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
package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.config.ContainerConfiguration;
import com.alibaba.cloud.ai.tool.PythonExecutorTool;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = { ContainerConfiguration.class })
@DisplayName("Run Python Code in Docker Test Without Network")
@ActiveProfiles("docker")
public class DockerContainerPoolExecutorTest {

	private static final Logger log = LoggerFactory.getLogger(DockerContainerPoolExecutorTest.class);

	@Autowired
	private PythonExecutorTool pythonExecutorTool;

	static final String NORMAL_CODE = """
			def func(x: int):
			    if x <= 0:
			        return 1;
			    else:
			        return x * func(x-1)
			if __name__ == "__main__":
			    print(func(10))
			""";

	static final String CODE_WITH_DEPENDENCY = """
			import numpy as np

			matrix = np.array([[1, 2], [3, 4]])
			inverse_matrix = np.linalg.inv(matrix)

			print(matrix)
			print(inverse_matrix)
			""";

	static final String TIMEOUT_CODE = """
			while True:
				continue
			""";

	static final String ERROR_CODE = """
			void main() {}
			""";

	static final String NETWORK_CHECK = """
			import socket
			import urllib.request

			ans1 = "C" + "o" + "nnected"
			ans2 = "F" + "a" + "iled"
			if __name__ == "__main__":
			    try:
			        socket.gethostbyname("www.aliyun.com")
			        print("DNS " + ans1)
			    except:
			        print("DNS " + ans2)
			    try:
			        with urllib.request.urlopen("http://www.aliyun.com", timeout=3) as response:
			            print("HTTP " + ans1)
			    except Exception as e:
			        print(f"HTTP {ans2}: {str(e)}")
			""";

	static final String NEED_INPUT = """
			print(input())
			""";

	private void testNormalCode() {
		log.info("Run Normal Code");
		String response = pythonExecutorTool.executePythonCode(NORMAL_CODE, null, "DataFrame Data");
		System.out.println(response);
		assertThat(response).contains("3628800");
		log.info("Run Normal Code Finished");
	}

	private void testCodeWithDependency() {
		log.info("Run Code with Third-parties Installed");
		String response = pythonExecutorTool.executePythonCode(CODE_WITH_DEPENDENCY, "numpy==2.2.6", "DataFrame Data");
		System.out.println(response);
		assertThat(response).doesNotContain("ModuleNotFoundError");
		log.info("Run Code with Third-parties Installed Finished");
	}

	private void testTimeoutCode() {
		log.info("Run Code with Endless Loop");
		String response = pythonExecutorTool.executePythonCode(TIMEOUT_CODE, null, "DataFrame Data");
		System.out.println(response);
		assertThat(response).contains("Killed");
		log.info("Run Code with Endless Loop Finished");
	}

	private void testErrorCode() {
		log.info("Run Code with Syntax Error");
		String response = pythonExecutorTool.executePythonCode(ERROR_CODE, null, "DataFrame Data");
		System.out.println(response);
		assertThat(response).contains("SyntaxError");
		log.info("Run Code with Syntax Error Finished");
	}

	private void testNetworkCheck() {
		log.info("Run Network Check");
		String response = pythonExecutorTool.executePythonCode(NETWORK_CHECK, null, "DataFrame Data");
		System.out.println(response);
		assertThat(response).contains("Connected").doesNotContain("Failed");
		log.info("Run Network Check Finished");
	}

	private void testNeedInput() {
		log.info("Check Need Input");
		String response = pythonExecutorTool.executePythonCode(NEED_INPUT, null, "DataFrame Data");
		System.out.println(response);
		assertThat(response).contains("DataFrame Data");
		log.info("Run Need Input Finished");
	}

	@Test
	@DisplayName("Concurrency Testing")
	public void testConcurrency() throws InterruptedException {
		ExecutorService executorService = Executors.newFixedThreadPool(10);
		executorService.submit(this::testNormalCode);
		executorService.submit(this::testCodeWithDependency);
		executorService.submit(this::testTimeoutCode);
		executorService.submit(this::testErrorCode);
		executorService.submit(this::testNetworkCheck);
		executorService.submit(this::testNeedInput);
		executorService.shutdown();
		while (!executorService.awaitTermination(500, TimeUnit.MILLISECONDS))
			;
	}

}
