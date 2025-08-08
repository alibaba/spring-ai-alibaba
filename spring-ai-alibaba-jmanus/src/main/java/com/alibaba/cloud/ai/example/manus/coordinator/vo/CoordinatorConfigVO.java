package com.alibaba.cloud.ai.example.manus.coordinator.vo;

import java.util.List;

/**
 * CoordinatorConfigVO输出数据结构
 */
public class CoordinatorConfigVO {

	private String id; // 配置ID，对应planId

	private String name; // 配置名称，对应title

	private String description; // 配置描述，对应userRequest

	private String endpoint; // 端点地址

	private List<CoordinatorParameterVO> parameters; // 参数列表

	// 构造函数
	public CoordinatorConfigVO() {
	}

	public CoordinatorConfigVO(String id, String name, String description, List<CoordinatorParameterVO> parameters) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.parameters = parameters;
	}

	public CoordinatorConfigVO(String id, String name, String description, String endpoint,
			List<CoordinatorParameterVO> parameters) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.endpoint = endpoint;
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

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public List<CoordinatorParameterVO> getParameters() {
		return parameters;
	}

	public void setParameters(List<CoordinatorParameterVO> parameters) {
		this.parameters = parameters;
	}

	@Override
	public String toString() {
		return "CoordinatorConfigVO{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", description='" + description
				+ '\'' + ", endpoint='" + endpoint + '\'' + ", parameters=" + parameters + '}';
	}

}