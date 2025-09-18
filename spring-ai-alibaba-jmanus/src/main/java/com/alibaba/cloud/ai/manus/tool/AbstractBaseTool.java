/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.manus.tool;

import org.springframework.ai.chat.model.ToolContext;
import com.alibaba.cloud.ai.manus.tool.code.ToolExecuteResult;

/**
 * Abstract base class for tools providing common functionality All concrete tool
 * implementations should extend this class
 *
 * @param <I> Tool input type
 */
public abstract class AbstractBaseTool<I> implements ToolCallBiFunctionDef<I> {

	/**
	 * Current plan ID for the tool execution context
	 */
	protected String currentPlanId;

	/**
	 * Root plan ID is the global parent of the whole execution plan
	 */
	protected String rootPlanId;

	@Override
	public boolean isReturnDirect() {
		return false;
	}

	@Override
	public void setCurrentPlanId(String planId) {
		this.currentPlanId = planId;
	}

	@Override
	public void setRootPlanId(String rootPlanId) {
		this.rootPlanId = rootPlanId;
	}

	/**
	 * Default implementation delegates to run method Subclasses can override this method
	 * if needed
	 */
	@Override
	public ToolExecuteResult apply(I input, ToolContext toolContext) {
		return run(input);
	}

	/**
	 * Abstract method that subclasses must implement to define tool-specific execution
	 * logic
	 * @param input Tool input parameters
	 * @return Tool execution result
	 */
	public abstract ToolExecuteResult run(I input);

}
