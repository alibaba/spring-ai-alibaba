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
package com.alibaba.cloud.ai.example.manus.planning.model.vo.mapreduce;

import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionStep;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

/**
 * 执行节点接口，定义了所有执行节点的公共行为
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type",
		visible = true)
@JsonSubTypes({ @JsonSubTypes.Type(value = SequentialNode.class, name = "sequential"),
		@JsonSubTypes.Type(value = MapReduceNode.class, name = "mapreduce") })
public interface ExecutionNode {

	/**
	 * 获取节点类型
	 * @return 节点类型
	 */
	MapReduceStepType getType();

	/**
	 * 获取节点中的所有执行步骤
	 * @return 执行步骤列表
	 */
	List<ExecutionStep> getAllSteps();

	/**
	 * 获取节点的总步骤数量
	 * @return 总步骤数
	 */
	int getTotalStepCount();

	/**
	 * 获取节点的字符串表示
	 * @return 节点字符串
	 */
	String getNodeInStr();

}
