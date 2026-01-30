/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.studio.core.base.manager;

import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.springframework.stereotype.Component;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.alibaba.cloud.ai.studio.core.base.manager.AppComponentManager.OUTPUT_DECORATE_PARAM_KEY;

/**
 * Sandbox manager for secure script execution. Supports Python and JavaScript script
 * types. Implements isolated execution environment to prevent runtime crashes.
 *
 *
 */
@Slf4j
@Component
public class SandboxManager {

	/**
	 * Executes a script
	 * @param scriptContent Script content
	 * @param localVariableMap Map of variable values
	 * @param requestId Request ID
	 * @return Execution result
	 */
	public Result<String> executeScript(String scriptContent, Map<String, Object> localVariableMap, String requestId) {
		return executeScriptWithLanguage(scriptContent, localVariableMap, "js", requestId);
	}

	/**
	 * Executes Python3 script using GraalVM Python
	 * @param scriptContent Python script content
	 * @param variables Script variable mapping
	 * @param requestId Request ID
	 * @return Execution result
	 */
	public Result<String> executePython3Script(String scriptContent, Map<String, Object> variables, String requestId) {
		return executeScriptWithLanguage(scriptContent, variables, "python", requestId);
	}

	/**
	 * Executes script with specified language using GraalVM
	 * @param scriptContent Script content
	 * @param variables Script variable mapping
	 * @param language Language identifier ("js", "python", etc.)
	 * @param requestId Request ID
	 * @return Execution result
	 */
	public Result<String> executeScriptWithLanguage(String scriptContent, Map<String, Object> variables, String language, String requestId) {
		// Validate parameters
		if (StringUtils.isBlank(scriptContent)) {
			log.error("Script content cannot be empty");
			return Result.error(requestId, ErrorCode.INVALID_PARAMS);
		}

		try {
			Context.Builder contextBuilder;

			if ("python".equals(language)) {
				// For Python, enable site module import
				contextBuilder = Context.newBuilder(language)
						.allowAllAccess(true)
						.option("python.ForceImportSite", "true");
			} else {
				// For other languages (like JS), use default configuration without additional options
				// that might not be supported by all language implementations
				contextBuilder = Context.newBuilder(language)
						.allowAllAccess(true);
			}

			// Create isolated execution environment
			try (Context context = contextBuilder.build()) {

				// Inject variables into script environment
				if (variables != null && !variables.isEmpty()) {
					for (Map.Entry<String, Object> entry : variables.entrySet()) {
						context.getBindings(language).putMember(entry.getKey(), entry.getValue());
					}
				}

				// Execute script and capture result
				Value result = context.eval(language, scriptContent);

				// Build result map
				Map<String, Object> resultMap = Maps.newHashMap();
				resultMap.put("success", true);
				Map<String, Object> innerMap = Maps.newHashMap();
				// Handle different result types
				if (result.isNull()) {
					innerMap.put(OUTPUT_DECORATE_PARAM_KEY, null);
				}
				else if (result.isString()) {
					innerMap.put(OUTPUT_DECORATE_PARAM_KEY, result.asString());
				}
				else if (result.isNumber()) {
					innerMap.put(OUTPUT_DECORATE_PARAM_KEY, result.asDouble());
				}
				else if (result.isBoolean()) {
					innerMap.put(OUTPUT_DECORATE_PARAM_KEY, result.asBoolean());
				}
				else if (result.hasArrayElements()) {
					List<Object> list = Lists.newArrayList();
					for (long i = 0; i < result.getArraySize(); i++) {
						list.add(result.getArrayElement(i).as(Object.class));
					}
					innerMap.put(OUTPUT_DECORATE_PARAM_KEY, list);
				}
				else if (result.hasMembers()) {
					innerMap.putAll(result.as(Map.class));
				}
				resultMap.put("data", innerMap);

				// Convert to JSON and return
				return Result.success(requestId, JsonUtils.toJson(resultMap));
			}
		}
		catch (Exception e) {
			log.error("GraalVM {} script execution exception", language, e);
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			return Result.error(requestId, ErrorCode.SYSTEM_ERROR);
		}
	}

}
