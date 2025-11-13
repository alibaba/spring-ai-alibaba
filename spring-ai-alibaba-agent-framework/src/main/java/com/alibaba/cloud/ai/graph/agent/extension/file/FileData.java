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
package com.alibaba.cloud.ai.graph.agent.extension.file;

import java.util.List;

/**
 * FileData structure for storing file contents with metadata.
 * Equivalent to Python's FileData TypedDict.
 */
public class FileData {
	private final List<String> content;
	private final String createdAt;
	private final String modifiedAt;

	public FileData(List<String> content, String createdAt, String modifiedAt) {
		this.content = content;
		this.createdAt = createdAt;
		this.modifiedAt = modifiedAt;
	}

	public List<String> getContent() {
		return content;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public String getModifiedAt() {
		return modifiedAt;
	}
}

