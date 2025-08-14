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
 * @since 1.0.0-beta
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
