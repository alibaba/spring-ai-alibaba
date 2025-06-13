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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.node.code.entity.CodeBlock;
import com.alibaba.cloud.ai.graph.node.code.entity.CodeExecutionConfig;
import com.alibaba.cloud.ai.graph.node.code.entity.CodeExecutionResult;
import com.alibaba.cloud.ai.graph.node.code.entity.CodeLanguage;
import com.alibaba.cloud.ai.graph.node.code.entity.RunnerAndPreload;
import com.alibaba.cloud.ai.graph.node.code.javascript.NodeJsTemplateTransformer;
import com.alibaba.cloud.ai.graph.node.code.python3.Python3TemplateTransformer;
import com.alibaba.cloud.ai.graph.node.code.java.JavaTemplateTransformer;

/**
 * @author HeYQ
 * @since 2024-11-28 11:47
 */
public class CodeExecutorNodeAction implements NodeAction {

	private final CodeExecutor codeExecutor;

	private final String codeLanguage;

	private final String code;

	private final CodeExecutionConfig codeExecutionConfig;

	private Map<String, Object> params;

	private static final Map<CodeLanguage, TemplateTransformer> CODE_TEMPLATE_TRANSFORMERS = Map.of(
			CodeLanguage.PYTHON3, new Python3TemplateTransformer(), CodeLanguage.PYTHON,
			new Python3TemplateTransformer(), CodeLanguage.JAVASCRIPT, new NodeJsTemplateTransformer(),
			CodeLanguage.JAVA, new JavaTemplateTransformer());

	private static final Map<CodeLanguage, String> CODE_LANGUAGE_TO_RUNNING_LANGUAGE = Map.of(CodeLanguage.JAVASCRIPT,
			"nodejs", CodeLanguage.JINJA2, CodeLanguage.PYTHON3.getValue(), CodeLanguage.PYTHON3,
			CodeLanguage.PYTHON3.getValue(), CodeLanguage.PYTHON, CodeLanguage.PYTHON.getValue(), CodeLanguage.JAVA,
			CodeLanguage.JAVA.getValue());

	public CodeExecutorNodeAction(CodeExecutor codeExecutor, String codeLanguage, String code,
			CodeExecutionConfig config, Map<String, Object> params) {
		this.codeExecutor = codeExecutor;
		this.codeLanguage = codeLanguage;
		this.code = code;
		this.codeExecutionConfig = config;
		this.params = params;
	}

	private Map<String, Object> executeWorkflowCodeTemplate(CodeLanguage language, String code, List<Object> inputs)
			throws Exception {
		TemplateTransformer templateTransformer = CODE_TEMPLATE_TRANSFORMERS.get(language);
		if (templateTransformer == null) {
			throw new RuntimeException("Unsupported language: " + language);
		}

		RunnerAndPreload runnerAndPreload = templateTransformer.transformCaller(code, inputs);
		String response = executeCode(language, runnerAndPreload.preloadScript(), runnerAndPreload.runnerScript());

		return templateTransformer.transformResponse(response);
	}

	private String executeCode(CodeLanguage language, String preloadScript, String code) throws Exception {
		List<CodeBlock> codeBlockList = new ArrayList<>(10);
		codeBlockList.add(new CodeBlock(CODE_LANGUAGE_TO_RUNNING_LANGUAGE.get(language), code));

		CodeExecutionResult codeExecutionResult = codeExecutor.executeCodeBlocks(codeBlockList,
				this.codeExecutionConfig);
		if (codeExecutionResult.exitCode() != 0) {
			throw new RuntimeException("code execution failed, exit code: " + codeExecutionResult.exitCode()
					+ ", logs: " + codeExecutionResult.logs());
		}

		return codeExecutionResult.logs();
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		List<Object> inputs = new ArrayList<>(10);
		if (params != null && !params.isEmpty()) {
			for (String key : params.keySet()) {
				inputs.add(state.data().get((String) params.get(key)));
			}
		}
		return executeWorkflowCodeTemplate(CodeLanguage.fromValue(codeLanguage), code, inputs);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private CodeExecutor codeExecutor;

		private String codeLanguage;

		private String code;

		private CodeExecutionConfig config;

		private Map<String, Object> params;

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

		public Builder params(Map<String, String> params) {
			this.params = new LinkedHashMap<>(params);
			return this;
		}

		public CodeExecutorNodeAction build() {
			return new CodeExecutorNodeAction(codeExecutor, codeLanguage, code, config, params);
		}

	}

}
