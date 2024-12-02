package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.action.code.CodeExecutorNodeAction;
import com.alibaba.cloud.ai.graph.action.code.LocalCommandlineCodeExecutor;
import com.alibaba.cloud.ai.graph.action.code.entity.CodeBlock;
import com.alibaba.cloud.ai.graph.action.code.entity.CodeExecutionConfig;
import com.alibaba.cloud.ai.graph.action.code.entity.CodeExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

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
		config = CodeExecutionConfig.builder().workDir(tempDir.toString()).build();
	}

	@Test
	void testExecutePythonSuccessfully() throws Exception {
		String code = """
				print('Hello, Python!')
				""";

		NodeAction<StateGraphTest.MessagesState> codeNode = CodeExecutorNodeAction.builder()
			.codeExecutor(new LocalCommandlineCodeExecutor())
			.build();
		List<CodeBlock> codeBlockList = new ArrayList<>();
		codeBlockList.add(new CodeBlock("python", code));
		Map<String, Object> map = new HashMap<>();
		map.put("codeBlockList", codeBlockList);
		map.put("codeExecutionConfig", config);

		StateGraphTest.MessagesState messagesState = new StateGraphTest.MessagesState(map);
		Map<String, Object> stateData = codeNode.apply(messagesState);

		CodeExecutionResult result = (CodeExecutionResult) stateData.get("codeExecutionResult");
		assertThat(result.exitCode()).isZero();
		assertThat(result.logs()).contains("Hello, Python!");
	}

}
