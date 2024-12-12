package com.alibaba.cloud.ai.graph.node.code;

import com.alibaba.cloud.ai.graph.node.code.entity.CodeBlock;
import com.alibaba.cloud.ai.graph.node.code.entity.CodeExecutionConfig;
import com.alibaba.cloud.ai.graph.node.code.entity.CodeExecutionResult;

import java.util.List;

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
