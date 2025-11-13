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

/**
 * Structured file listing information.
 * Minimal contract used across backends. Only "path" is required.
 * Other fields are best-effort and may be absent depending on backend.
 */
public class FileInfo {
	private final String path;
	private final Boolean isDir;
	private final Long size;
	private final String modifiedAt;

	public FileInfo(String path, Boolean isDir, Long size, String modifiedAt) {
		this.path = path;
		this.isDir = isDir;
		this.size = size;
		this.modifiedAt = modifiedAt;
	}

	public String getPath() {
		return path;
	}

	public Boolean getIsDir() {
		return isDir;
	}

	public Long getSize() {
		return size;
	}

	public String getModifiedAt() {
		return modifiedAt;
	}
}

