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
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.internal.node.Node;

public abstract class BaseAgent extends Agent {

	/** The output key for the agent's result */
	protected String outputKey;

	protected KeyStrategy outputKeyStrategy;

	protected boolean includeContents;

	public BaseAgent(String name, String description, boolean includeContents, String outputKey,
			KeyStrategy outputKeyStrategy) throws GraphStateException {
		super(name, description);
		this.includeContents = includeContents;
		this.outputKey = outputKey;
		this.outputKeyStrategy = outputKeyStrategy;
	}

	public abstract Node asNode(boolean includeContents, String outputKeyToParent);

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

}
