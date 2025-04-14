package com.alibaba.cloud.ai.mcp.nacos;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.ai.alibaba.mcp.nacos")
public class NacosMcpRegistryProperties {

	String serverAddr;

	String namespace;

	String getServerAddr() {
		return serverAddr;
	}

	void setServerAddr(String serverAddr) {
		this.serverAddr = serverAddr;
	}

	String getNamespace() {
		return namespace;
	}

	void setNamespace(String namespace) {
		this.namespace = namespace;
	}
}
