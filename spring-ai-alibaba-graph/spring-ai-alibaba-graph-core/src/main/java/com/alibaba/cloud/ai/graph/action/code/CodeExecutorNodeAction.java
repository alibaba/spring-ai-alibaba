package com.alibaba.cloud.ai.graph.action.code;

import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.action.code.entity.CodeBlock;
import com.alibaba.cloud.ai.graph.action.code.entity.CodeExecutionConfig;
import com.alibaba.cloud.ai.graph.action.code.entity.CodeExecutionResult;
import com.alibaba.cloud.ai.graph.state.AgentState;
import java.util.List;
import java.util.Map;

/**
 * @author HeYQ
 * @since 2024-11-28 11:47
 */

public class CodeExecutorNodeAction<State extends AgentState> implements NodeAction<State> {

	private final CodeExecutor codeExecutor;

	public CodeExecutorNodeAction(CodeExecutor codeExecutor) {
		this.codeExecutor = codeExecutor;
	}

	@Override
	public Map<String, Object> apply(State state) throws Exception {
		CodeExecutionConfig config = (CodeExecutionConfig) state.data().get("codeExecutionConfig");

		List<CodeBlock> codeBlocks = (List<CodeBlock>) state.data().get("codeBlockList");

		CodeExecutionResult executionResult = codeExecutor.executeCodeBlocks(codeBlocks, config);

		return Map.of("codeExecutionResult", executionResult);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private CodeExecutor codeExecutor;

		public Builder() {
		}

		public Builder codeExecutor(CodeExecutor codeExecutor) {
			this.codeExecutor = codeExecutor;
			return this;
		}

		public <State extends AgentState> CodeExecutorNodeAction<State> build() {
			return new CodeExecutorNodeAction<>(codeExecutor);
		}

	}

}
