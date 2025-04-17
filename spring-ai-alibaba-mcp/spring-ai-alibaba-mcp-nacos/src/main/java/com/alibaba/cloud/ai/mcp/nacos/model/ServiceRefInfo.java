package com.alibaba.cloud.ai.mcp.nacos.model;

/**
 * @author Sunrisea
 */
public class ServiceRefInfo {

	private String namespace;

	private String group;

	private String service;

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

}
