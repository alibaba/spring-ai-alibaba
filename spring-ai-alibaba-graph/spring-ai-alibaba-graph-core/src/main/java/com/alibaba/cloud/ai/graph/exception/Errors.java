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
 * Enum representing various error messages related to graph state.
 */
public enum Errors {

	invalidNodeIdentifier("END is not a valid node id!"), invalidEdgeIdentifier("END is not a valid edge sourceId!"),
	duplicateNodeError("node with id: %s already exist!"), duplicateEdgeError("edge with id: %s already exist!"),
	duplicateConditionalEdgeError("conditional edge from '%s' already exist!"),
	edgeMappingIsEmpty("edge mapping is empty!"), missingEntryPoint("missing Entry Point"),
	entryPointNotExist("entryPoint: %s doesn't exist!"), finishPointNotExist("finishPoint: %s doesn't exist!"),
	missingNodeReferencedByEdge("edge sourceId '%s' refers to undefined node!"),
	missingNodeInEdgeMapping("edge mapping for sourceId: %s contains a not existent nodeId %s!"),
	invalidEdgeTarget("edge sourceId: %s has an initialized target value!"),
	duplicateEdgeTargetError("edge [%s] has duplicate targets %s!"),
	unsupportedConditionalEdgeOnParallelNode(
			"parallel node doesn't support conditional branch, but on [%s] a conditional branch on %s have been found!"),
	illegalMultipleTargetsOnParallelNode("parallel node [%s] must have only one target, but %s have been found!"),
	interruptionNodeNotExist("node '%s' configured as interruption doesn't exist!");

	private final String errorMessage;

	Errors(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	/**
	 * Creates a new GraphStateException with the formatted error message.
	 * @param args the arguments to format the error message
	 * @return a new GraphStateException
	 */
	public GraphStateException exception(Object... args) {
		return new GraphStateException(format(errorMessage, (Object[]) args));
	}

}
