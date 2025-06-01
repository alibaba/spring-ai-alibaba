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

package com.alibaba.cloud.ai.example.deepresearch;

import com.alibaba.cloud.ai.example.deepresearch.tool.PythonReplTool;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

/**
 * Run Python Code in Docker Test
 *
 * @author vlsmb
 */
@SpringBootTest
@DisplayName("Run Python Code in Docker Test")
@ActiveProfiles("test")
public class PythonReplToolTest {

	@Autowired
	private PythonReplTool pythonReplTool;

	private static final String NORMAL_CODE = """
			def func(x: int):
			    if x <= 0:
			        return 1;
			    else:
			        return x * func(x-1)
			if __name__ == "__main__":
			    print(func(10))
			""";

	private static final String CODE_WITH_DEPENDENCY = """
			import numpy as np

			matrix = np.array([[1, 2], [3, 4]])
			inverse_matrix = np.linalg.inv(matrix)

			print(matrix)
			print(inverse_matrix)
			""";

	private static final String TIMEOUT_CODE = """
			while True:
				continue
			""";

	private static final String ERROR_CODE = """
			void main() {}
			""";

	private static final String NETWORK_CHECK = """
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
	public void testNormalCode() {
		assertThat(pythonReplTool.executePythonCode(NORMAL_CODE, null)).contains("3628800");
	}

	@Test
	public void testCodeWithoutDependency() {
		assertThat(pythonReplTool.executePythonCode(CODE_WITH_DEPENDENCY, null)).contains("ModuleNotFoundError");
	}

	@Test
	public void testCodeWithDependency() {
		assertThat(pythonReplTool.executePythonCode(CODE_WITH_DEPENDENCY, "numpy==2.2.6"))
			.doesNotContain("ModuleNotFoundError");
	}

	@Test
	public void testTimeoutCode() {
		assertThat(pythonReplTool.executePythonCode(TIMEOUT_CODE, null)).contains("Error");
	}

	@Test
	public void testErrorCode() {
		assertThat(pythonReplTool.executePythonCode(ERROR_CODE, null)).contains("SyntaxError");
	}

	@Test
	public void testNetworkCheck() {
		assertThat(pythonReplTool.executePythonCode(NETWORK_CHECK, null)).contains("Failed")
			.doesNotContain("Connected");
	}

}
