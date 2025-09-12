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
package com.alibaba.cloud.ai.manus.runtime.entity.vo.mapreduce;

import com.alibaba.cloud.ai.manus.runtime.entity.vo.ExecutionStep;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

/**
 * Execution node interface defining common behaviors for all execution nodes
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type",
		visible = true)
@JsonSubTypes({ @JsonSubTypes.Type(value = SequentialNode.class, name = "sequential"),
		@JsonSubTypes.Type(value = MapReduceNode.class, name = "mapreduce") })
public interface ExecutionNode {

	/**
	 * Get node type
	 * @return Node type
	 */
	MapReduceStepType getType();

	/**
	 * Get all execution steps in the node
	 * @return Execution step list
	 */
	List<ExecutionStep> getAllSteps();

	/**
	 * Get total number of steps in the node
	 * @return Total step count
	 */
	int getTotalStepCount();

	/**
	 * Get string representation of the node
	 * @return Node string
	 */
	String getNodeInStr();

	/**
	 * Get the result of the node execution
	 * @return Node execution result
	 */
	String getResult();

}
