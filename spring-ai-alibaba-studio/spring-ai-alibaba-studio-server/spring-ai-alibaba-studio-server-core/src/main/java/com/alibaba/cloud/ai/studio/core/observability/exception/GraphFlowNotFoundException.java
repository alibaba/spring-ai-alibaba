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
package com.alibaba.cloud.ai.studio.core.observability.exception;

/**
 * Exception thrown when a requested graph flow is not found in the system.
 *
 * <p>
 * This exception is typically thrown when trying to access a graph flow by its ID, but
 * no such flow exists in the registry. It includes the flow ID that was requested for
 * better error reporting and debugging.
 * </p>
 */
public class GraphFlowNotFoundException extends RuntimeException {

	private final String flowId;

	public GraphFlowNotFoundException(String flowId) {
		super("Graph flow with ID '" + flowId + "' not found");
		this.flowId = flowId;
	}

	public GraphFlowNotFoundException(String flowId, String message) {
		super(message);
		this.flowId = flowId;
	}

	public GraphFlowNotFoundException(String flowId, Throwable cause) {
		super("Graph flow with ID '" + flowId + "' not found", cause);
		this.flowId = flowId;
	}

	public String getFlowId() {
		return flowId;
	}

}
