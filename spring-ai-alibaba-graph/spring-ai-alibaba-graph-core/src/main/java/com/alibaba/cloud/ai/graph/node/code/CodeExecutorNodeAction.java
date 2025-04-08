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

package com.alibaba.cloud.ai.graph.node.code;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.node.code.entity.CodeBlock;
import com.alibaba.cloud.ai.graph.node.code.entity.CodeExecutionConfig;
import com.alibaba.cloud.ai.graph.node.code.entity.CodeExecutionResult;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;

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
