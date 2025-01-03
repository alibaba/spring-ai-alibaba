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

package com.alibaba.cloud.ai.model;

import java.util.Map;

import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.model.AbstractResponseMetadata;
import org.springframework.ai.model.ResponseMetadata;

/**
 * Title rerank response metadata.<br>
 * Description rerank response metadata.<br>
 *
 * @author yuanci.ytb
 * @since 1.0.0-M2
 */

public class RerankResponseMetadata extends AbstractResponseMetadata implements ResponseMetadata {

	private Usage usage = new EmptyUsage();

	public RerankResponseMetadata() {
	}

	public RerankResponseMetadata(Usage usage) {
		this.usage = usage;
	}

	public RerankResponseMetadata(Usage usage, Map<String, Object> metadata) {
		this.usage = usage;
		this.map.putAll(metadata);
	}

	public Usage getUsage() {
		return usage;
	}

	public void setUsage(Usage usage) {
		this.usage = usage;
	}

}
