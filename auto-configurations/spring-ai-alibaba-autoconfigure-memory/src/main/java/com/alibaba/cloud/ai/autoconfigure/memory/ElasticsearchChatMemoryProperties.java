/*
 * Copyright 2024-2025 the original author or authors.
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
 */

package com.alibaba.cloud.ai.autoconfigure.memory;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for ElasticSearch chat memory.
 */
@ConfigurationProperties(prefix = "spring.ai.memory.elasticsearch")
public class ElasticsearchChatMemoryProperties {

	/**
	 * Elasticsearch host URL
	 */
	private String host = "localhost";

	/**
	 * Elasticsearch port
	 */
	private int port = 9200;

	/**
	 * List of cluster nodes in format: hostname:port
	 */
	private List<String> nodes = new ArrayList<>();

	/**
	 * Index name to query
	 */
	private String index;

	/**
	 * Query field to search in
	 */
	private String queryField = "content";

	/**
	 * Username for authentication (optional)
	 */
	private String username;

	/**
	 * Password for authentication (optional)
	 */
	private String password;

	/**
	 * Maximum number of documents to retrieve
	 */
	private int maxResults = 10;

	/**
	 * Connection scheme (http/https)
	 */
	private String scheme = "http";

	public String getHost() {
		return host;
	}

	public void setHost(final String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(final int port) {
		this.port = port;
	}

	public List<String> getNodes() {
		return nodes;
	}

	public void setNodes(final List<String> nodes) {
		this.nodes = nodes;
	}

	public String getIndex() {
		return index;
	}

	public void setIndex(final String index) {
		this.index = index;
	}

	public String getQueryField() {
		return queryField;
	}

	public void setQueryField(final String queryField) {
		this.queryField = queryField;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(final String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(final String password) {
		this.password = password;
	}

	public int getMaxResults() {
		return maxResults;
	}

	public void setMaxResults(final int maxResults) {
		this.maxResults = maxResults;
	}

	public String getScheme() {
		return scheme;
	}

	public void setScheme(final String scheme) {
		this.scheme = scheme;
	}

}
