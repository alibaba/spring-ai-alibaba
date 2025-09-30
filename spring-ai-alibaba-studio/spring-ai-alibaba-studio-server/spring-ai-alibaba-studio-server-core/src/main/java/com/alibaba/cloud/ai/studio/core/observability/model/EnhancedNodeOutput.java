package com.alibaba.cloud.ai.studio.core.observability.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Enhanced node output model, including node metadata and business data.
 *
 * <p>
 * Provides complete node execution information, including:
 * <ul>
 * <li>Basic node information (ID, name, type)</li>
 * <li>Execution status and time information</li>
 * <li>Business data output</li>
 * <li>Execution context information</li>
 * </ul>
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnhancedNodeOutput {

	/**
	 * Node ID.
	 */
	@JsonProperty("node_id")
	private String nodeId;

	/**
	 * Execution status:
	 * <ul>
	 * <li>EXECUTING - In progress</li>
	 * <li>SUCCESS - Execution successful</li>
	 * <li>FAILED - Execution failed</li>
	 * <li>SKIPPED - Execution skipped</li>
	 * </ul>
	 */
	@JsonProperty("execution_status")
	private String executionStatus;

	/**
	 * Start execution time.
	 */
	@JsonProperty("start_time")
	private LocalDateTime startTime;

	/**
	 * Completion time.
	 */
	@JsonProperty("end_time")
	private LocalDateTime endTime;

	/**
	 * Execution time in milliseconds.
	 */
	@JsonProperty("duration_ms")
	private Long durationMs;

	/**
	 * Node business data output.
	 */
	@JsonProperty("data")
	private Map<String, Object> data;

	/**
	 * Error message (if execution fails).
	 */
	@JsonProperty("error_message")
	private String errorMessage;

	/**
	 * Input data (optional, for debugging).
	 */
	@JsonProperty("input_data")
	private Map<String, Object> inputData;

	/**
	 * Execution sequence number (execution order in the entire process).
	 */
	@JsonProperty("execution_order")
	private Integer executionOrder;

	/**
	 * Whether it is the final node.
	 */
	@JsonProperty("is_final")
	private Boolean isFinal;

	/**
	 * List of parent node IDs (dependent upstream nodes).
	 */
	@JsonProperty("parent_nodes")
	private java.util.List<String> parentNodes;

	/**
	 * Creates an output indicating that node execution has started.
	 */
	public static EnhancedNodeOutput createStarting(String nodeId, String nodeName, String nodeType) {
		return EnhancedNodeOutput.builder()
			.nodeId(nodeId)
			.executionStatus("EXECUTING")
			.startTime(LocalDateTime.now())
			.build();
	}

	/**
	 * Creates an output indicating that node execution has completed.
	 */
	public static EnhancedNodeOutput createCompleted(String nodeId, String nodeName, String nodeType,
			Map<String, Object> data, LocalDateTime startTime) {
		LocalDateTime endTime = LocalDateTime.now();
		long duration = java.time.Duration.between(startTime, endTime).toMillis();

		return EnhancedNodeOutput.builder()
			.nodeId(nodeId)
			.executionStatus("SUCCESS")
			.startTime(startTime)
			.endTime(endTime)
			.durationMs(duration)
			.data(data)
			.build();
	}

	/**
	 * Creates an output indicating that node execution has failed.
	 */
	public static EnhancedNodeOutput createFailed(String nodeId, String nodeName, String nodeType, String errorMessage,
			LocalDateTime startTime) {
		LocalDateTime endTime = LocalDateTime.now();
		long duration = startTime != null ? java.time.Duration.between(startTime, endTime).toMillis() : 0;

		return EnhancedNodeOutput.builder()
			.nodeId(nodeId)
			.executionStatus("FAILED")
			.startTime(startTime)
			.endTime(endTime)
			.durationMs(duration)
			.errorMessage(errorMessage)
			.build();
	}

}
