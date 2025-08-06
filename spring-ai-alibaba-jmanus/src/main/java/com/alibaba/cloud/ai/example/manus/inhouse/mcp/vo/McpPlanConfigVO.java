package com.alibaba.cloud.ai.example.manus.inhouse.mcp.vo;

import java.util.List;

/**
 * McpPlanConfigVO输出数据结构
 */
public class McpPlanConfigVO {

	private String id; // 配置ID，对应planId

	private String name; // 配置名称，对应title

	private String description; // 配置描述，对应userRequest

	private List<McpPlanParameterVO> parameters; // 参数列表

	// 构造函数
	public McpPlanConfigVO() {
	}

	public McpPlanConfigVO(String id, String name, String description, List<McpPlanParameterVO> parameters) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.parameters = parameters;
	}

	// Getter和Setter方法
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<McpPlanParameterVO> getParameters() {
		return parameters;
	}

	public void setParameters(List<McpPlanParameterVO> parameters) {
		this.parameters = parameters;
	}

	@Override
	public String toString() {
		return "McpPlanConfigVO{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", description='" + description
				+ '\'' + ", parameters=" + parameters + '}';
	}

}
