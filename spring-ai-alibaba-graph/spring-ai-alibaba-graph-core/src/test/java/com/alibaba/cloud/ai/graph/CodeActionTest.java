package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.node.code.CodeExecutorNodeAction;
import com.alibaba.cloud.ai.graph.node.code.LocalCommandlineCodeExecutor;
import com.alibaba.cloud.ai.graph.node.code.entity.CodeExecutionConfig;
import com.alibaba.cloud.ai.graph.state.NodeState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

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

	private LLMNodeActionTest.MockState mockState() {
		Map<String, Object> initData = new HashMap<>();
		return new LLMNodeActionTest.MockState(initData);
	}

	@Test
	void testExecutePythonSuccessfully() throws Exception {
		String code = """
				print({"result":'Hello, Python!'})
				""";

		NodeAction codeNode = CodeExecutorNodeAction.builder()
			.codeExecutor(new LocalCommandlineCodeExecutor())
			.code(code)
			.codeLanguage("python")
			.config(config)
			.build();

		Map<String, Object> stateData = codeNode.apply(mockState());

		// assertThat(result.exitCode()).isZero();
		// assertThat(result.logs()).contains("Hello, Python!");
		System.out.println(stateData);
	}

	static class MockState extends NodeState {

		/**
		 * Constructs an AgentState with the given initial data.
		 * @param initData the initial data for the agent state
		 */
		public MockState(Map<String, Object> initData) {
			super(initData);
		}

	}

}
