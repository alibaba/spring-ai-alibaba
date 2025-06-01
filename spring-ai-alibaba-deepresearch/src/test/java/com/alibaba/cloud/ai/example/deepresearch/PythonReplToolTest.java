package com.alibaba.cloud.ai.example.deepresearch;

import com.alibaba.cloud.ai.example.deepresearch.tool.PythonReplTool;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

	private static final String TEST_NORMAL_CODE = """
			def func(x: int):
			    if x <= 0:
			        return 1;
			    else:
			        return x * func(x-1)
			if __name__ == "__main__":
			    print(func(10))
			""";

	@Test
	public void testCode() throws IOException {
		Path tempDir = Files.createTempDirectory("nioTemp_");
		System.out.println("临时目录路径: " + tempDir.toAbsolutePath());
		Files.delete(tempDir);
	}

}
