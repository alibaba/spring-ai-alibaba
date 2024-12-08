package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.action.code.CodeExecutorNodeAction;
import com.alibaba.cloud.ai.graph.action.code.LocalCommandlineCodeExecutor;
import com.alibaba.cloud.ai.graph.action.code.entity.CodeBlock;
import com.alibaba.cloud.ai.graph.action.code.entity.CodeExecutionConfig;
import com.alibaba.cloud.ai.graph.action.code.entity.CodeExecutionResult;
import com.alibaba.cloud.ai.graph.state.AgentState;
import com.alibaba.cloud.ai.graph.state.AppenderChannel;
import com.alibaba.cloud.ai.graph.state.Channel;
import com.alibaba.cloud.ai.graph.utils.CollectionsUtils;
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

		NodeAction<MessagesState> codeNode = CodeExecutorNodeAction.builder()
			.codeExecutor(new LocalCommandlineCodeExecutor())
			.build();
		List<CodeBlock> codeBlockList = new ArrayList<>();
		codeBlockList.add(new CodeBlock("python", code));
		Map<String, Object> map = new HashMap<>();
		map.put("codeBlockList", codeBlockList);
		map.put("codeExecutionConfig", config);

		MessagesState messagesState = new MessagesState(map);
		Map<String, Object> stateData = codeNode.apply(messagesState);

		CodeExecutionResult result = (CodeExecutionResult) stateData.get("codeExecutionResult");
		assertThat(result.exitCode()).isZero();
		assertThat(result.logs()).contains("Hello, Python!");
		System.out.println(result.logs());
	}

	static class MessagesState extends AgentState {

		static Map<String, Channel<?>> SCHEMA = CollectionsUtils.mapOf("messages",
				AppenderChannel.<String>of(ArrayList::new));

		public MessagesState(Map<String, Object> initData) {
			super(initData);
		}

		int steps() {
			return value("steps", 0);
		}

		List<String> messages() {
			return this.<List<String>>value("messages").orElseThrow(() -> new RuntimeException("messages not found"));
		}

	}


}
