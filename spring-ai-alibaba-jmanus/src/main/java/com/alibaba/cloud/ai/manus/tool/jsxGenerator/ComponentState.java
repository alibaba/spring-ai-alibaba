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
package com.alibaba.cloud.ai.manus.tool.jsxGenerator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Component state class for storing current component file path, last operation result,
 * and component metadata
 */
public class ComponentState {

	private String currentFilePath = "";

	private String lastOperationResult = "";

	private String componentType = "";

	private Map<String, Object> componentMetadata = new ConcurrentHashMap<>();

	private String lastGeneratedCode = "";

	private final Object componentLock = new Object();

	public String getCurrentFilePath() {
		return currentFilePath;
	}

	public void setCurrentFilePath(String currentFilePath) {
		this.currentFilePath = currentFilePath;
	}

	public String getLastOperationResult() {
		return lastOperationResult;
	}

	public void setLastOperationResult(String lastOperationResult) {
		this.lastOperationResult = lastOperationResult;
	}

	public String getComponentType() {
		return componentType;
	}

	public void setComponentType(String componentType) {
		this.componentType = componentType;
	}

	public Map<String, Object> getComponentMetadata() {
		return componentMetadata;
	}

	public void setComponentMetadata(Map<String, Object> componentMetadata) {
		this.componentMetadata = componentMetadata;
	}

	public void addComponentMetadata(String key, Object value) {
		this.componentMetadata.put(key, value);
	}

	public String getLastGeneratedCode() {
		return lastGeneratedCode;
	}

	public void setLastGeneratedCode(String lastGeneratedCode) {
		this.lastGeneratedCode = lastGeneratedCode;
	}

	public Object getComponentLock() {
		return componentLock;
	}

}
