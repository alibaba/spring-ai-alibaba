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

import java.util.List;

import com.alibaba.cloud.ai.graph.node.code.entity.CodeBlock;
import com.alibaba.cloud.ai.graph.node.code.entity.CodeExecutionConfig;
import com.alibaba.cloud.ai.graph.node.code.entity.CodeExecutionResult;

/**
 * @author HeYQ
 * @since 2024-12-02 17:15
 */
public interface CodeExecutor {

	/**
	 * Execute code blocks and return the result. This method should be implemented by the
	 * code executor.
	 * @param codeBlockList The code blocks to execute.
	 * @param codeExecutionConfig The configuration of the code execution.
	 * @return CodeExecutionResult The result of the code execution.
	 * @throws Exception ValueError: Errors in user inputs
	 */

	CodeExecutionResult executeCodeBlocks(List<CodeBlock> codeBlockList, CodeExecutionConfig codeExecutionConfig)
			throws Exception;

	/**
	 * Restart the code executor. This method should be implemented by the code executor.
	 * This method is called when the agent is reset.
	 */
	void restart();

}
