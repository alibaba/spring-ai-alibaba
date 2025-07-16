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
package com.alibaba.cloud.ai.graph.exception;

import static java.lang.String.format;

/**
 * Enum representing various error messages related to graph runner.
 */
public enum RunnableErrors {

	missingNodeInEdgeMapping("cannot find edge mapping for id: '%s' in conditional edge with sourceId: '%s' "),
	missingNode("node with id: '%s' doesn't exist!"), missingEdge("edge with sourceId: '%s' doesn't exist!"),
	executionError("%s"), initializationError("%s isn't included in the keyStrategies"), subGraphInterrupt("%s"),
	nodeInterrupt("%s");

	private final String errorMessage;

	RunnableErrors(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	/**
	 * Creates a new GraphRunnerException with the formatted error message.
	 * @param args the arguments to format the error message
	 * @return a new GraphRunnerException
	 */
	public GraphRunnerException exception(String... args) {
		return new GraphRunnerException(format(errorMessage, (Object[]) args));
	}

}
