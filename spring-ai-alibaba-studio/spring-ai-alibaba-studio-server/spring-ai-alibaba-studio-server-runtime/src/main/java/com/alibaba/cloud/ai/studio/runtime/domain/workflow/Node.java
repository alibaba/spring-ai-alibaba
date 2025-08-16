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
package com.alibaba.cloud.ai.studio.runtime.domain.workflow;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Node of The Graph 画布的节点声明
 *
 * @since 1.0.0.3
 */
@Data
public class Node implements Serializable {

	private String id;

	private String name;

	private String desc;

	private String type;

	private NodeCustomConfig config;

	/**
	 * Custom Config for Node 节点自定义配置
	 */
	@Data
	public static class NodeCustomConfig implements Serializable {

		private static final long serialVersionUID = -1L;

		@JsonProperty("input_params")
		private List<InputParam> inputParams;

		@JsonProperty("output_params")
		private List<OutputParam> outputParams;

		@JsonProperty("node_param")
		private Map<String, Object> nodeParam;

	}

	/**
	 * Input Param for Node 节点输入参数
	 */
	@EqualsAndHashCode(callSuper = true)
	@Data
	public static class InputParam extends CommonParam implements Serializable {

		private static final long serialVersionUID = -1L;

	}

	/**
	 * Output Param for Node 节点输出参数
	 */
	@EqualsAndHashCode(callSuper = true)
	@Data
	public static class OutputParam extends CommonParam implements Serializable {

		private static final long serialVersionUID = -1L;

		private List<OutputParam> properties;

	}

}
