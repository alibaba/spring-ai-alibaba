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
