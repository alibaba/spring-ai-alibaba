package com.alibaba.cloud.ai.mcp.nacos.model;

/**
 * @author Sunrisea
 */
public class RemoteServerConfigInfo {

	private ServiceRefInfo serviceRef;

	private String exportPath;

	private String backendProtocol;

	public ServiceRefInfo getServiceRef() {
		return serviceRef;
	}

	public void setServiceRef(ServiceRefInfo serviceRef) {
		this.serviceRef = serviceRef;
	}

	public String getExportPath() {
		return exportPath;
	}

	public void setExportPath(String exportPath) {
		this.exportPath = exportPath;
	}

	public String getBackendProtocol() {
		return backendProtocol;
	}

	public void setBackendProtocol(String backendProtocol) {
		this.backendProtocol = backendProtocol;
	}

}
