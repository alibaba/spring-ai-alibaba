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
package com.alibaba.cloud.ai.graph.agent;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.internal.node.Node;

import java.lang.reflect.Type;

public abstract class BaseAgent extends Agent {

	protected String inputSchema;
	protected Type inputType;

	protected String outputSchema;
	protected Class<?> outputType;

	/** The output key for the agent's result */
	protected String outputKey;

	protected KeyStrategy outputKeyStrategy;

	protected boolean includeContents;

	protected boolean returnReasoningContents;

	public BaseAgent(String name, String description, boolean includeContents, boolean returnReasoningContents, String outputKey,
			KeyStrategy outputKeyStrategy) {
		super(name, description);
		this.includeContents = includeContents;
		this.returnReasoningContents = returnReasoningContents;
		this.outputKey = outputKey;
		this.outputKeyStrategy = outputKeyStrategy;
	}

	public abstract Node asNode(boolean includeContents, boolean returnReasoningContents, String outputKeyToParent);

	public boolean isIncludeContents() {
		return includeContents;
	}

	public String getOutputKey() {
		return outputKey;
	}

	public void setOutputKey(String outputKey) {
		this.outputKey = outputKey;
	}

	public KeyStrategy getOutputKeyStrategy() {
		return outputKeyStrategy;
	}

	public void setOutputKeyStrategy(KeyStrategy outputKeyStrategy) {
		this.outputKeyStrategy = outputKeyStrategy;
	}

	String getInputSchema() {
		return inputSchema;
	}

	void setInputSchema(String inputSchema) {
		this.inputSchema = inputSchema;
	}

	Type getInputType() {
		return inputType;
	}

	void setInputType(Type inputType) {
		this.inputType = inputType;
	}

	String getOutputSchema() {
		return outputSchema;
	}

	void setOutputSchema(String outputSchema) {
		this.outputSchema = outputSchema;
	}

	void setIncludeContents(boolean includeContents) {
		this.includeContents = includeContents;
	}

	public boolean isReturnReasoningContents() {
		return returnReasoningContents;
	}

	public void setReturnReasoningContents(boolean returnReasoningContents) {
		this.returnReasoningContents = returnReasoningContents;
	}
}
