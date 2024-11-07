package com.alibaba.cloud.ai.graph;

/**
 * Exception thrown when there is an error related to the state of a graph.
 */
public class GraphStateException extends Exception {

	/**
	 * Constructs a new GraphStateException with the specified error message.
	 * @param errorMessage the detail message
	 */
	public GraphStateException(String errorMessage) {
		super(errorMessage);
	}

}
