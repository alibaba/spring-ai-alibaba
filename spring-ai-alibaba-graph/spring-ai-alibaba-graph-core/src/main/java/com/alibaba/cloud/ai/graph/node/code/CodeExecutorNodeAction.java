package com.alibaba.cloud.ai.graph.node.code;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.node.code.entity.CodeBlock;
import com.alibaba.cloud.ai.graph.node.code.entity.CodeExecutionConfig;
import com.alibaba.cloud.ai.graph.node.code.entity.CodeExecutionResult;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author HeYQ
 * @since 2024-11-28 11:47
 */
public class CodeExecutorNodeAction implements NodeAction {

	private final CodeExecutor codeExecutor;

	private final String codeLanguage;

	private final String code;

	private final CodeExecutionConfig codeExecutionConfig;

	public CodeExecutorNodeAction(CodeExecutor codeExecutor, String codeLanguage, String code,
			CodeExecutionConfig config) {
		this.codeExecutor = codeExecutor;
		this.codeLanguage = codeLanguage;
		this.code = code;
		this.codeExecutionConfig = config;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		List<CodeBlock> codeBlockList = new ArrayList<>(10);
		codeBlockList.add(new CodeBlock(codeLanguage, code));
		CodeExecutionResult codeExecutionResult = codeExecutor.executeCodeBlocks(codeBlockList,
				this.codeExecutionConfig);
		if (codeExecutionResult.exitCode() != 0) {
			throw new RuntimeException("code execution failed, exit code: " + codeExecutionResult.exitCode()
					+ ", logs: " + codeExecutionResult.logs());
		}
		return JSONObject.parseObject(codeExecutionResult.logs(), new TypeReference<Map<String, Object>>() {
		});
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private CodeExecutor codeExecutor;

		private String codeLanguage;

		private String code;

		private CodeExecutionConfig config;

		public Builder() {
		}

		public Builder codeExecutor(CodeExecutor codeExecutor) {
			this.codeExecutor = codeExecutor;
			return this;
		}

		public Builder codeLanguage(String codeLanguage) {
			this.codeLanguage = codeLanguage;
			return this;
		}

		public Builder code(String code) {
			this.code = code;
			return this;
		}

		public Builder config(CodeExecutionConfig config) {
			this.config = config;
			return this;
		}

		public CodeExecutorNodeAction build() {
			return new CodeExecutorNodeAction(codeExecutor, codeLanguage, code, config);
		}

	}

}
