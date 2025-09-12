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

import java.util.function.BiFunction;

import org.springframework.ai.chat.model.ToolContext;
import com.alibaba.cloud.ai.manus.tool.code.ToolExecuteResult;

/**
 * Interface for tool definitions, providing unified tool definition methods
 *
 * @param <I> Tool input type
 */
public interface ToolCallBiFunctionDef<I> extends BiFunction<I, ToolContext, ToolExecuteResult> {

	/**
	 * Get the name of the tool group
	 * @return Returns the unique identifier name of the tool
	 */
	String getServiceGroup();

	/**
	 * Get the name of the tool
	 * @return Returns the unique identifier name of the tool
	 */
	String getName();

	/**
	 * Get the description information of the tool
	 * @return Returns the functional description of the tool
	 */
	String getDescription();

	/**
	 * Get the parameter definition schema of the tool
	 * @return Returns JSON format parameter definition schema
	 */
	String getParameters();

	/**
	 * Get the input type of the tool
	 * @return Returns the input parameter type Class that the tool accepts
	 */
	Class<I> getInputType();

	/**
	 * Determine whether the tool returns results directly
	 * @return Returns true if the tool returns results directly, otherwise false
	 */
	boolean isReturnDirect();

	/**
	 * Set the associated Agent instance
	 * @param planId The plan ID to associate
	 */
	public void setCurrentPlanId(String planId);

	/**
	 * root plan id is the global parent of the whole execution plan id .
	 * @param rootPlanId
	 */
	public void setRootPlanId(String rootPlanId);

	/**
	 * Get the current status string of the tool
	 * @return Returns a string describing the current status of the tool
	 */
	String getCurrentToolStateString();

	/**
	 * Clean up all related resources for the specified planId
	 * @param planId Plan ID
	 */
	void cleanup(String planId);

}
