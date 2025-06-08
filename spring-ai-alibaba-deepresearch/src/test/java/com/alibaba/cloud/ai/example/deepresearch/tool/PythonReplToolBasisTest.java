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
import static org.assertj.core.api.Assertions.*;

/**
 * Run Python Code in Docker Test Without Network
 *
 * @author vlsmb
 */
@SpringBootTest
@DisplayName("Run Python Code in Docker Test Without Network")
@ActiveProfiles("python_basis")
public class PythonReplToolBasisTest {

	@Autowired
	private PythonReplTool pythonReplTool;

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

	@Test
	@DisplayName("Run Normal Code")
	public void testNormalCode() {
		String response = pythonReplTool.executePythonCode(NORMAL_CODE, null);
		System.out.println(response);
		assertThat(response).contains("Successfully executed").contains("3628800");
	}

	@Test
	@DisplayName("Run Code with Third-parties but Not Installed")
	public void testCodeWithoutDependency() {
		String response = pythonReplTool.executePythonCode(CODE_WITH_DEPENDENCY, null);
		System.out.println(response);
		assertThat(response).contains("Error executing code").contains("ModuleNotFoundError");
	}

	@Test
	@DisplayName("Run Code with Third-parties Installed")
	public void testCodeWithDependency() {
		String response = pythonReplTool.executePythonCode(CODE_WITH_DEPENDENCY, "numpy==2.2.6");
		System.out.println(response);
		assertThat(response).contains("Successfully executed").doesNotContain("ModuleNotFoundError");
	}

	@Test
	@DisplayName("Run Code with Endless Loop")
	public void testTimeoutCode() {
		String response = pythonReplTool.executePythonCode(TIMEOUT_CODE, null);
		System.out.println(response);
		assertThat(response).contains("Error executing code");
	}

	@Test
	@DisplayName("Run Code with Syntax Error")
	public void testErrorCode() {
		String response = pythonReplTool.executePythonCode(ERROR_CODE, null);
		System.out.println(response);
		assertThat(response).contains("SyntaxError");
	}

	@Test
	@DisplayName("Check Network is Disabled")
	public void testNetworkCheck() {
		String response = pythonReplTool.executePythonCode(NETWORK_CHECK, null);
		System.out.println(response);
		assertThat(response).contains("Failed").doesNotContain("Connected");
	}

}
