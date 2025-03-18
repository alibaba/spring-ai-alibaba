/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.internal.edge;

import com.alibaba.cloud.ai.graph.action.AsyncSendEdgeAction;

import java.util.Map;

/**
 * The type Send edge condition.
 */
public class SendEdgeCondition extends EdgeCondition {

	private AsyncSendEdgeAction asyncSendEdgeAction;

	/**
	 * Represents a condition associated with an edge in a graph.
	 * @param action The action to be performed asynchronously when the edge condition is
	 * met.
	 * @param mappings A map of string key-value pairs representing additional mappings
	 * for the edge condition.
	 */
	public SendEdgeCondition(AsyncSendEdgeAction action, Map<String, String> mappings) {
		super(null, mappings);
		this.asyncSendEdgeAction = action;
	}

	/**
	 * Send edge action async send edge action.
	 * @return the async send edge action
	 */
	public AsyncSendEdgeAction sendEdgeAction() {
		return asyncSendEdgeAction;
	}

}
