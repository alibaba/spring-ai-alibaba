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

import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleScriptContext;

import com.alibaba.cloud.ai.graph.node.code.entity.CodeBlock;
import com.alibaba.cloud.ai.graph.node.code.entity.CodeExecutionConfig;
import com.alibaba.cloud.ai.graph.node.code.entity.CodeExecutionResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author XenoAmess
 * @since 2025-08-30 10:00
 */
public class InJvmCodeExecutor implements CodeExecutor {

	private static final Logger logger = LoggerFactory.getLogger(InJvmCodeExecutor.class);

	@Override
	public CodeExecutionResult executeCodeBlocks(List<CodeBlock> codeBlockList, CodeExecutionConfig codeExecutionConfig)
			throws Exception {
		StringBuilder allLogs = new StringBuilder();
		List<Object> results = new ArrayList<>(codeBlockList.size());
		CodeExecutionResult result;
		for (int i = 0; i < codeBlockList.size(); i++) {
			CodeBlock codeBlock = codeBlockList.get(i);
			logger.info("\n>>>>>>>> IN JVM EXECUTING CODE BLOCK {} (inferred language is {})...", i + 1,
					codeBlock.language());
			result = executeCode(codeBlock, codeExecutionConfig);
			allLogs.append("\n").append(result.logs());
			if (result.result() != null) {
				results.addAll(result.result());
			}
			if (result.exitCode() != 0) {
				return new CodeExecutionResult(result.exitCode(), allLogs.toString(), null, results);
			}
		}
		return new CodeExecutionResult(0, allLogs.toString(), null, results);
	}

	@Override
	public void restart() {

		logger.warn("Restarting local command line code executor is not supported. No action is taken.");
	}

	public CodeExecutionResult executeCode(CodeBlock codeBlock, CodeExecutionConfig config) throws Exception {
		String language = codeBlock.language();
		String code = codeBlock.code();
		if (Objects.isNull(language) || Objects.isNull(code)) {
			throw new Exception("Either language or code must be provided.");
		}
		switch (language) {
			case "groovy":
				return executeCodeJsr223("groovy", codeBlock, config);
			case "nashorn":
				return executeCodeJsr223("nashorn", codeBlock, config);
			default:
				throw new Exception("Unsupported language for in-JVM execution: " + language);
		}
	}

	private CodeExecutionResult executeCodeJsr223(String engineName, CodeBlock codeBlock, CodeExecutionConfig config) {
		CodeExecutionResult codeExecutionResult = null;
		try (ByteArrayOutputStream baosOut = new ByteArrayOutputStream();
				ByteArrayOutputStream baosErr = new ByteArrayOutputStream();
				PrintStream psOut = new PrintStream(baosOut, true, StandardCharsets.UTF_8);
				PrintStream psErr = new PrintStream(baosErr, true, StandardCharsets.UTF_8);) {
			try {
				ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
				ScriptEngine scriptEngine = scriptEngineManager.getEngineByName(engineName);
				ScriptContext ctx = new SimpleScriptContext();
				ctx.setWriter(new OutputStreamWriter(psOut));
				ctx.setErrorWriter(new OutputStreamWriter(psErr));
				scriptEngine.setContext(ctx);
				scriptEngine.put("args", codeBlock.inputs());

				String consolePrefix = "nashorn".equals(engineName) ? """
						var console = {
						    log: print,
						    warn: print,
						    error: print
						};

						""" : "";

				@Nullable
				Object result = scriptEngine.eval(consolePrefix + codeBlock.code());
				if (result == null) {
					Invocable inv = (Invocable) scriptEngine;
					result = inv.invokeFunction("main", codeBlock.inputs());
				}
				List<Object> results = new ArrayList<>(1);
				results.add(result);

				String out = baosOut.toString(StandardCharsets.UTF_8);
				String err = baosErr.toString(StandardCharsets.UTF_8);
				codeExecutionResult = new CodeExecutionResult(0, out + (err.isEmpty() ? "" : ("\n[stderr]:\n" + err)),
						null, results);
			}
			catch (Exception e) {
				e.printStackTrace(psErr);
				psErr.flush();
				codeExecutionResult = new CodeExecutionResult(-1, baosErr.toString());
			}
		}
		catch (IOException e) {
			logger.error("error on PrintStream close", e);
		}
		return codeExecutionResult;
	}

}
