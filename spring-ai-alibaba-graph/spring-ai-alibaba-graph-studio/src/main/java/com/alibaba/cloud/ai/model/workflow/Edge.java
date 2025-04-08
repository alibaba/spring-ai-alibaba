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
package com.alibaba.cloud.ai.model.workflow;

import java.util.Map;

public class Edge {

	private String id;

	private String source;

	private String target;

	private String sourceHandle;

	private String targetHandle;

	private Map<String, Object> data;

	private Integer zIndex = 0;

	public String getId() {
		return id;
	}

	public Edge setId(String id) {
		this.id = id;
		return this;
	}

	public String getSource() {
		return source;
	}

	public Edge setSource(String source) {
		this.source = source;
		return this;
	}

	public String getTarget() {
		return target;
	}

	public Edge setTarget(String target) {
		this.target = target;
		return this;
	}

	public String getSourceHandle() {
		return sourceHandle;
	}

	public Edge setSourceHandle(String sourceHandle) {
		this.sourceHandle = sourceHandle;
		return this;
	}

	public String getTargetHandle() {
		return targetHandle;
	}

	public Edge setTargetHandle(String targetHandle) {
		this.targetHandle = targetHandle;
		return this;
	}

	public Map<String, Object> getData() {
		return data;
	}

	public Edge setData(Map<String, Object> data) {
		this.data = data;
		return this;
	}

	public Integer getzIndex() {
		return zIndex;
	}

	public Edge setzIndex(Integer zIndex) {
		this.zIndex = zIndex;
		return this;
	}

}
