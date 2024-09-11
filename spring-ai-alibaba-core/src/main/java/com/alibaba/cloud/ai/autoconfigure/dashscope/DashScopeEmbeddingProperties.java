/*
 * Copyright 2023 - 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.autoconfigure.dashscope;

import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;

import org.springframework.ai.document.MetadataMode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(DashScopeEmbeddingProperties.CONFIG_PREFIX)
public class DashScopeEmbeddingProperties extends DashScopeParentProperties {

	public static final String CONFIG_PREFIX = "spring.ai.dashscope.embedding";

	public static final String DEFAULT_EMBEDDING_MODEL = "text-embedding-v1";

	/**
	 * Enable Dashscope embedding client.
	 */
	private boolean enabled = true;

	private MetadataMode metadataMode = MetadataMode.EMBED;

	@NestedConfigurationProperty
	private DashScopeEmbeddingOptions options = DashScopeEmbeddingOptions.builder()
		.withModel(DEFAULT_EMBEDDING_MODEL)
		.build();

	public DashScopeEmbeddingOptions getOptions() {
		return this.options;
	}

	public void setOptions(DashScopeEmbeddingOptions options) {
		this.options = options;
	}

	public MetadataMode getMetadataMode() {
		return this.metadataMode;
	}

	public void setMetadataMode(MetadataMode metadataMode) {
		this.metadataMode = metadataMode;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}
