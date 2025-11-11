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

import java.util.Map;

/**
 * Result of edit operation.
 */
public class EditResult {
	private final String path;
	private final int occurrences;
	private final String error;
	private final Map<String, FileData> filesUpdate;

	public EditResult(String path, int occurrences, String error, Map<String, FileData> filesUpdate) {
		this.path = path;
		this.occurrences = occurrences;
		this.error = error;
		this.filesUpdate = filesUpdate;
	}

	public String getPath() {
		return path;
	}

	public int getOccurrences() {
		return occurrences;
	}

	public String getError() {
		return error;
	}

	public Map<String, FileData> getFilesUpdate() {
		return filesUpdate;
	}
}

