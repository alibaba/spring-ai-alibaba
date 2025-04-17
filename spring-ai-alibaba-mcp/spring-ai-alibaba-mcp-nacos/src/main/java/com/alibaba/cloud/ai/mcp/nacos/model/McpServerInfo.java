package com.alibaba.cloud.ai.mcp.nacos.model;

import java.util.Map;

/**
 * @author Sunrisea
 */
public class McpServerInfo {

	private String type;

	private String name;

	private String description;

	private String version;

	private Boolean enable;

	private RemoteServerConfigInfo remoteServerConfig;

	private Map<Object, Object> localServerConfig;

	private String toolsDescriptionRef;

	private String promptDescriptionRef;

	private String resourceDescriptionRef;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
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

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Boolean getEnable() {
		return enable;
	}

	public void setEnable(Boolean enable) {
		this.enable = enable;
	}

	public RemoteServerConfigInfo getRemoteServerConfig() {
		return remoteServerConfig;
	}

	public void setRemoteServerConfig(RemoteServerConfigInfo remoteServerConfig) {
		this.remoteServerConfig = remoteServerConfig;
	}

	public Map<Object, Object> getLocalServerConfig() {
		return localServerConfig;
	}

	public void setLocalServerConfig(Map<Object, Object> localServerConfig) {
		this.localServerConfig = localServerConfig;
	}

	public String getToolsDescriptionRef() {
		return toolsDescriptionRef;
	}

	public void setToolsDescriptionRef(String toolsDescriptionRef) {
		this.toolsDescriptionRef = toolsDescriptionRef;
	}

	public String getPromptDescriptionRef() {
		return promptDescriptionRef;
	}

	public void setPromptDescriptionRef(String promptDescriptionRef) {
		this.promptDescriptionRef = promptDescriptionRef;
	}

	public String getResourceDescriptionRef() {
		return resourceDescriptionRef;
	}

	public void setResourceDescriptionRef(String resourceDescriptionRef) {
		this.resourceDescriptionRef = resourceDescriptionRef;
	}

}
