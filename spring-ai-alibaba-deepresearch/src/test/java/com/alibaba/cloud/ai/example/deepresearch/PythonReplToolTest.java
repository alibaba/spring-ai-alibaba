package com.alibaba.cloud.ai.example.deepresearch;

import com.alibaba.cloud.ai.example.deepresearch.tool.PythonReplTool;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

/**
 * Run Python Code in Docker Test
 *
 * @author vlsmb
 */
@SpringBootTest
@DisplayName("Run Python Code in Docker Test")
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

	@Test
	public void testNormalCode() {
		System.out.println(pythonReplTool.executePythonCode(NORMAL_CODE, null));
	}

	@Test
	public void testCodeWithoutDependency() {
		System.out.println(pythonReplTool.executePythonCode(CODE_WITH_DEPENDENCY, null));
	}

	@Test
	public void testCodeWithDependency() {
		System.out.println(pythonReplTool.executePythonCode(CODE_WITH_DEPENDENCY, "numpy==1.26.4"));
	}

}
