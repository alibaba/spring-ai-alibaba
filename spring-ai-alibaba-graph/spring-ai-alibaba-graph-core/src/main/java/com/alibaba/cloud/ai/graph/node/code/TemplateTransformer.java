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

import com.alibaba.cloud.ai.graph.node.code.entity.RunnerAndPreload;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author HeYQ
 * @since 2025-01-06 22:14
 */
public abstract class TemplateTransformer {

	protected static final String CODE_PLACEHOLDER = "{{code}}";

	protected static final String INPUTS_PLACEHOLDER = "{{inputs}}";

	protected static final String RESULT_TAG = "<<RESULT>>";

	public RunnerAndPreload transformCaller(String code, List<Object> inputs) throws Exception {
		String runnerScript = assembleRunnerScript(code, inputs);
		String preloadScript = getPreloadScript();

		return new RunnerAndPreload(runnerScript, preloadScript);
	}

	public Map<String, Object> transformResponse(String response) throws Exception {
		String resultStr = extractResultStrFromResponse(response);
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(resultStr,
				mapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
	}

	public abstract String getRunnerScript();

	private String extractResultStrFromResponse(String response) {
		Pattern pattern = Pattern.compile(RESULT_TAG + "(.*?)" + RESULT_TAG, Pattern.DOTALL);
		Matcher matcher = pattern.matcher(response);

		if (matcher.find()) {
			return matcher.group(1).trim();
		}
		else {
			throw new IllegalArgumentException("Failed to parse result");
		}
	}

	private String serializeInputs(List<Object> inputs) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		String inputsJsonStr = mapper.writeValueAsString(inputs);
		return Base64.getEncoder().encodeToString(inputsJsonStr.getBytes(StandardCharsets.UTF_8));
	}

	private String assembleRunnerScript(String code, List<Object> inputs) throws Exception {
		String script = getRunnerScript();
		script = script.replace(CODE_PLACEHOLDER, code);
		script = script.replace(INPUTS_PLACEHOLDER, serializeInputs(inputs));
		return script;
	}

	private String getPreloadScript() {
		return "";
	}

}
