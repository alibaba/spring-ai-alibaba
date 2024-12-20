package com.alibaba.cloud.ai.dashscope.rag;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class for the Alibaba OpenSearch. This class holds the necessary
 * configuration parameters such as instance ID, endpoint, access user name, and access
 * password required to interact with the HA3 engine.
 *
 * @author ming（fuyou.lxm）
 * @since 1.0.0-M3
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenSearchConfig {

	private String instanceId;

	private String endpoint;

	private String accessUserName;

	private String accessPassWord;

	public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	public String getEndpoint() { // 新增
		return endpoint;
	}

	public void setEndpoint(String endpoint) { // 新增
		this.endpoint = endpoint;
	}

	public String getAccessUserName() { // 新增
		return accessUserName;
	}

	public void setAccessUserName(String accessUserName) { // 新增
		this.accessUserName = accessUserName;
	}

	public String getAccessPassWord() { // 新增
		return accessPassWord;
	}

	public void setAccessPassWord(String accessPassWord) { // 新增
		this.accessPassWord = accessPassWord;
	}

	public Map<String, ?> toClientParams() {
		Map<String, Object> params = new HashMap<>();
		params.put("instanceId", this.getInstanceId());
		params.put("endpoint", this.getEndpoint());
		params.put("accessUserName", this.getAccessUserName());
		params.put("accessPassWord", this.getAccessPassWord());
		return params;
	}

}