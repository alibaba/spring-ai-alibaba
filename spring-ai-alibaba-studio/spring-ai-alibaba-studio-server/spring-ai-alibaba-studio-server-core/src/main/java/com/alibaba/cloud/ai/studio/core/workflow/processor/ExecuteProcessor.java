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

package com.alibaba.cloud.ai.studio.core.workflow.processor;

import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Edge;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Node;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowContext;
import com.google.common.collect.Lists;
import lombok.Data;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.io.Serializable;
import java.util.List;

/**
 * Interface for workflow execution processing
 *
 * @since 1.0.0.3
 */
public interface ExecuteProcessor {

	/**
	 * Returns the type of the node
	 */
	String getNodeType();

	/**
	 * Returns the description of the node
	 */
	String getNodeDescription();

	/**
	 * Executes the node in the workflow
	 * @param graph The directed acyclic graph representing the workflow
	 * @param node The node to be executed
	 * @param context The workflow execution context
	 */
	void execute(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context);

	/**
	 * Validates the node parameters
	 * @param graph The directed acyclic graph representing the workflow
	 * @param node The node to be validated
	 * @return Result of the parameter validation
	 */
	CheckNodeParamResult checkNodeParam(DirectedAcyclicGraph<String, Edge> graph, Node node);

	/**
	 * Result of node parameter validation
	 */
	@Data
	class CheckNodeParamResult implements Serializable {

		/** Whether the validation was successful */
		private boolean success;

		/** List of error messages */
		private List<String> errorInfos = Lists.newArrayList();

		/** ID of the validated node */
		private String nodeId;

		/** Name of the validated node */
		private String nodeName;

		/** Type of the validated node */
		private String nodeType;

		public static CheckNodeParamResult success() {
			CheckNodeParamResult checkNodeParamResult = new CheckNodeParamResult();
			checkNodeParamResult.setSuccess(true);
			return checkNodeParamResult;
		}

	}

	/**
	 * Result of workflow parameter validation
	 */
	@Data
	class CheckFlowParamResult implements Serializable {

		/** Whether the validation was successful */
		private boolean success;

		/** List of node validation results */
		private List<CheckNodeParamResult> checkNodeParamResults = Lists.newArrayList();

		public static CheckFlowParamResult success() {
			CheckFlowParamResult checkFlowParamResult = new CheckFlowParamResult();
			checkFlowParamResult.setSuccess(true);
			return checkFlowParamResult;
		}

	}

}
