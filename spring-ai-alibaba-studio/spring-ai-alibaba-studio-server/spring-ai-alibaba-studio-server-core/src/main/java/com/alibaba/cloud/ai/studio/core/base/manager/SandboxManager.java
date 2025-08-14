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

	/** Nashorn engine name constant */
	private static final String ENGINE_NAME_NASHORN = "nashorn";

	/** Script engine manager */
	@Resource
	private ScriptEngineManager engineManager;

	/** Cache for ScriptEngine instances to avoid repeated creation */
	private final Map<String, ScriptEngine> engineCache = new ConcurrentHashMap<>();

	/**
	 * Executes a script
	 * @param scriptContent Script content
	 * @param localVariableMap Map of variable values
	 * @param requestId Request ID
	 * @return Execution result
	 */
	public Result<String> executeScript(String scriptContent, Map<String, Object> localVariableMap, String requestId) {
		// Validate parameters
		if (StringUtils.isBlank(scriptContent)) {
			log.error("Script content cannot be empty");
			return Result.error(requestId, ErrorCode.INVALID_PARAMS);
		}

		try {
			// Get or create script engine
			ScriptEngine engine = getScriptEngine(ENGINE_NAME_NASHORN);
			if (engine == null) {
				return Result.error(requestId, ErrorCode.SYSTEM_ERROR);
			}

			// Create context
			ScriptContext context = new SimpleScriptContext();

			// Inject variables
			Bindings bindings = engine.createBindings();
			if (localVariableMap != null) {
				bindings.putAll(localVariableMap);
			}
			context.setBindings(bindings, ScriptContext.ENGINE_SCOPE);

			// Execute script
			Object result;
			try {
				result = engine.eval(scriptContent, context);
			}
			catch (ScriptException e) {
				log.error("Script execution error: {}", e.getMessage());
				Map<String, Object> errorResult = Maps.newHashMap();
				errorResult.put("success", false);
				errorResult.put("message", e.getMessage());
				errorResult.put("code", "SCRIPT_ERROR");
				return Result.success(requestId, JsonUtils.toJson(errorResult));
			}

			// Build result map
			Map<String, Object> resultMap = Maps.newHashMap();
			resultMap.put("success", true);
			resultMap.put("data", result);

			// Convert to JSON and return
			return Result.success(requestId, JsonUtils.toJson(resultMap));
		}
		catch (Exception e) {
			log.error("Script execution exception", e);
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			return Result.error(requestId, ErrorCode.SYSTEM_ERROR);
		}
	}

	/**
	 * Executes Python3 script using GraalVM Python
	 * @param scriptContent Python script content
	 * @param variables Script variable mapping
	 * @param requestId Request ID
	 * @return Execution result
	 */
	public Result<String> executePython3Script(String scriptContent, Map<String, Object> variables, String requestId) {
		try {
			// Use GraalVM Python API to execute Python script directly
			Context.Builder contextBuilder = Context.newBuilder("python")
				.allowAllAccess(true)
				.option("python.ForceImportSite", "true");

			// Create isolated execution environment
			try (Context context = contextBuilder.build()) {

				// Inject variables into Python environment
				if (variables != null && !variables.isEmpty()) {
					for (Map.Entry<String, Object> entry : variables.entrySet()) {
						context.getBindings("python").putMember(entry.getKey(), entry.getValue());
					}
				}

				// Execute script and capture result
				Value result = context.eval("python", scriptContent);

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
			log.error("GraalVM Python script execution exception", e);
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			return Result.error(requestId, ErrorCode.SYSTEM_ERROR);
		}
	}

	/**
	 * Gets or creates a script engine instance
	 * @param engineName Name of the script engine
	 * @return ScriptEngine instance
	 */
	private synchronized ScriptEngine getScriptEngine(String engineName) {
		ScriptEngine engine = engineCache.get(engineName);
		if (engine == null) {
			// For Nashorn engine, use ES6 configuration
			if (ENGINE_NAME_NASHORN.equals(engineName)) {
				try {
					engine = new NashornScriptEngineFactory().getScriptEngine("--language=es6");
					log.info("Successfully created Nashorn script engine (ES6 mode): {}", engine.getClass().getName());
				}
				catch (Exception e) {
					log.error("Failed to create Nashorn engine, trying standard engine", e);
					engine = engineManager.getEngineByName(engineName);
				}
			}
			else {
				engine = engineManager.getEngineByName(engineName);
			}

			if (engine != null) {
				engineCache.put(engineName, engine);
				log.info("Successfully created script engine: {}, implementation class: {}", engineName,
						engine.getClass().getName());
			}
			else {
				log.error("Unsupported script engine: {}", engineName);
			}
		}
		return engine;
	}

}
