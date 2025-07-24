/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.alibaba.cloud.ai.mcp.router.model;

import java.util.List;
import java.util.Objects;

public class McpServerInfo {

	private String name;

	private String description;

	private String protocol;

	private String version;

	private String endpoint;

	private Boolean enabled;

	private List<String> tags;

	// 临时分数字段，用于向量搜索结果排序，不参与 equals 和 hashCode
	private transient double score;

	public McpServerInfo() {
	}

	public McpServerInfo(String name, String description, String protocol, String version, String endpoint,
			Boolean enabled, List<String> tags) {
		this.name = name;
		this.description = description;
		this.protocol = protocol;
		this.version = version;
		this.endpoint = endpoint;
		this.enabled = enabled;
		this.tags = tags;
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

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	@Override
	public String toString() {
		return "McpServerInfo{" + "name='" + name + '\'' + ", description='" + description + '\'' + ", protocol='"
				+ protocol + '\'' + ", version='" + version + '\'' + ", endpoint='" + endpoint + '\'' + ", enabled="
				+ enabled + ", tags=" + tags + ", score=" + score + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		McpServerInfo that = (McpServerInfo) o;
		return Objects.equals(name, that.name) && Objects.equals(description, that.description)
				&& Objects.equals(protocol, that.protocol) && Objects.equals(version, that.version)
				&& Objects.equals(endpoint, that.endpoint) && Objects.equals(enabled, that.enabled)
				&& Objects.equals(tags, that.tags);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, description, protocol, version, endpoint, enabled, tags);
	}

}
