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
package com.alibaba.cloud.ai.toolcalling.githubtoolkit;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Yeaury
 */
@ConfigurationProperties(prefix = GithubToolKitConstants.CONFIG_PREFIX)
public class GithubToolKitProperties extends CommonToolCallProperties {

	public static final String X_GitHub_Api_Version = "2022-11-28";

	private String owner;

	private String repository;

	public GithubToolKitProperties() {
		super("https://api.github.com");
		setPropertiesFromEnv(null, null, null, GithubToolKitConstants.TOKEN_ENV);
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getRepository() {
		return repository;
	}

	public void setRepository(String repository) {
		this.repository = repository;
	}

}
