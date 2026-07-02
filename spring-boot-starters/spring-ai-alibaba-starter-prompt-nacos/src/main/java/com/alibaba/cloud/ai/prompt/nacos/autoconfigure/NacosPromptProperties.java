package com.alibaba.cloud.ai.prompt.nacos.autoconfigure;

import java.util.Properties;

import com.alibaba.nacos.api.PropertyKeyConst;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = NacosPromptProperties.PREFIX)
public class NacosPromptProperties {

	public static final String PREFIX = "spring.ai.alibaba.prompt.nacos";

	private String namespace = "public";

	private String serverAddr = "127.0.0.1:8848";

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getServerAddr() {
		return serverAddr;
	}

	public void setServerAddr(String serverAddr) {
		this.serverAddr = serverAddr;
	}

	public Properties getNacosProperties() {
		Properties properties = new Properties();
		properties.put(PropertyKeyConst.NAMESPACE, this.namespace);
		properties.put(PropertyKeyConst.SERVER_ADDR, this.serverAddr);
		return properties;
	}

}
