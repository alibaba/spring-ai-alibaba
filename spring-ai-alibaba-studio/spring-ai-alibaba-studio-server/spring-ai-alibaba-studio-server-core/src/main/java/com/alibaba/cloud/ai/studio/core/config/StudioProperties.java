/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.studio.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for SAA Studio application.
 *
 * @since 1.0.0.3
 */

@ConfigurationProperties(StudioProperties.CONFIG_PREFIX)
@Data
public class StudioProperties {

	/** Configuration prefix for studio properties */
	public static final String CONFIG_PREFIX = "spring.ai.alibaba.studio";

	/** Storage path for studio data */
	private String storagePath;

	/** Connection timeout in seconds */
	private Integer connectTimeout = 30;

	/** Read timeout in seconds */
	private Integer readTimeout = 300;

	/** Stream read timeout in seconds */
	private Integer streamReadTimeout = 60;

	/** Maximum number of connections */
	private Integer maxConnections = 200;

	/** Maximum number of connections per route */
	private Integer maxConnectionsPerRoute = 100;

	/** Type of vector store to use */
	private String vectorStoreType = "elasticsearch";

	/** login method, like github oauth2 login */
	private String loginMethod = "third_party";

	/** upload method like oss, local file */
	private String uploadMethod = "file";

	/** oss config */
	private Oss oss;

	/**
	 * Gets the storage path. If not set, defaults to user home directory.
	 * @return the storage path
	 */
	public String getStoragePath() {
		if (storagePath == null) {
			storagePath = System.getProperty("user.home") + "/saa/storage";
		}

		return storagePath;
	}

	@Data
	public static class Oss {

		/** oss endpoint */
		private String endpoint;

		/** internal endpoint */
		private String internalEndpoint;

		/** oss access key id */
		private String accessKeyId;

		/** oss access key secret */
		private String accessKeySecret;

		/** oss bucket name */
		private String bucket;

		private String region;

	}

}
