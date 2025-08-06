package com.alibaba.cloud.ai.example.manus.inhouse.mcp.vo;

/**
 * McpPlanParameterVO参数对象
 */
public class McpPlanParameterVO {

	private String name; // 参数名称

	private String type; // 参数类型，默认"String"

	private String description; // 参数描述

	private boolean required; // 是否必需，默认true

	// 构造函数
	public McpPlanParameterVO() {
		this.type = "String";
		this.required = true;
	}

	public McpPlanParameterVO(String name, String type, String description, boolean required) {
		this.name = name;
		this.type = type != null ? type : "String";
		this.description = description;
		this.required = required;
	}

	public McpPlanParameterVO(String name, String description) {
		this.name = name;
		this.type = "String";
		this.description = description;
		this.required = true;
	}

	// Getter和Setter方法
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type != null ? type : "String";
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	@Override
	public String toString() {
		return "McpPlanParameterVO{" + "name='" + name + '\'' + ", type='" + type + '\'' + ", description='"
				+ description + '\'' + ", required=" + required + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		McpPlanParameterVO that = (McpPlanParameterVO) o;
		return name != null ? name.equals(that.name) : that.name == null;
	}

	@Override
	public int hashCode() {
		return name != null ? name.hashCode() : 0;
	}

}
