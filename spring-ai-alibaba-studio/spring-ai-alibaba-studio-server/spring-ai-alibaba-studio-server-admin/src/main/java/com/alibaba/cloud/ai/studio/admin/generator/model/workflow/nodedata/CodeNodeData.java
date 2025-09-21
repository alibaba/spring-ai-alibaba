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
package com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata;

import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.studio.admin.generator.model.Variable;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeData;
import com.alibaba.cloud.ai.studio.admin.generator.utils.ObjectToCodeUtil;

public class CodeNodeData extends NodeData {

	public static Variable getDefaultOutputSchema() {
		return new Variable("output", VariableType.OBJECT);
	}

	private String code;

	private String codeLanguage;

	private List<CodeParam> inputParams;

	private String outputKey;

	private int maxRetryCount = 1;

	private int retryIntervalMs = 1000;

	// 运行失败时的默认值
	private Map<String, Object> defaultValue;

	private CodeStyle codeStyle = CodeStyle.EXPLICIT_PARAMETERS;

	public String getCode() {
		return code;
	}

	public CodeNodeData setCode(String code) {
		this.code = code;
		return this;
	}

	public String getCodeLanguage() {
		return codeLanguage;
	}

	public CodeNodeData setCodeLanguage(String codeLanguage) {
		this.codeLanguage = codeLanguage;
		return this;
	}

	public List<CodeParam> getInputParams() {
		return inputParams;
	}

	public CodeNodeData setInputParams(List<CodeParam> inputParams) {
		this.inputParams = inputParams;
		return this;
	}

	public String getOutputKey() {
		return outputKey;
	}

	public CodeNodeData setOutputKey(String outputKey) {
		this.outputKey = outputKey;
		return this;
	}

	public int getMaxRetryCount() {
		return maxRetryCount;
	}

	public CodeNodeData setMaxRetryCount(int maxRetryCount) {
		this.maxRetryCount = maxRetryCount;
		return this;
	}

	public int getRetryIntervalMs() {
		return retryIntervalMs;
	}

	public CodeNodeData setRetryIntervalMs(int retryIntervalMs) {
		this.retryIntervalMs = retryIntervalMs;
		return this;
	}

	public Map<String, Object> getDefaultValue() {
		return defaultValue;
	}

	public CodeNodeData setDefaultValue(Map<String, Object> defaultValue) {
		this.defaultValue = defaultValue;
		return this;
	}

	public CodeStyle getCodeStyle() {
		return codeStyle;
	}

	public CodeNodeData setCodeStyle(CodeStyle codeStyle) {
		this.codeStyle = codeStyle;
		return this;
	}

	public enum CodeStyle {

		/**
		 * Dify代码样式
		 */
		EXPLICIT_PARAMETERS,

		/**
		 * Studio代码样式
		 */
		GLOBAL_DICTIONARY

		;

		public String toString() {
			return "CodeStyle." + name();
		}

	}

	public record CodeParam(String argName, Object value, String stateKey) {
		public static CodeParam withValue(String argName, Object value) {
			return new CodeParam(argName, value, null);
		}

		public static CodeParam withKey(String argName, String stateKey) {
			return new CodeParam(argName, null, stateKey);
		}

		@Override
		public String toString() {
			if (argName == null) {
				throw new IllegalArgumentException("argName cannot be null");
			}
			if (value == null && stateKey != null) {
				return String.format("CodeParam.withKey(%s, %s)", ObjectToCodeUtil.toCode(argName()),
						ObjectToCodeUtil.toCode(stateKey()));
			}
			if (value != null && stateKey == null) {
				return String.format("CodeParam.withValue(%s, %s)", ObjectToCodeUtil.toCode(argName()),
						ObjectToCodeUtil.toCode(value()));
			}
			throw new IllegalArgumentException("value and stateKey must only one.");
		}
	}

}
