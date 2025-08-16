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
package com.alibaba.cloud.ai.example.manus.tool.fileSandbox;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Represents a sandbox instance for a specific plan
 */
public class SandboxInstance {

	private String planId;

	private Path sandboxRoot;

	private Path uploadsDir;

	private Path processedDir;

	private Path tempDir;

	private LocalDateTime createdTime;

	private Map<String, SandboxFile> files;

	public SandboxInstance(String planId, Path sandboxRoot, Path uploadsDir, Path processedDir, Path tempDir) {
		this.planId = planId;
		this.sandboxRoot = sandboxRoot;
		this.uploadsDir = uploadsDir;
		this.processedDir = processedDir;
		this.tempDir = tempDir;
		this.createdTime = LocalDateTime.now();
		this.files = new ConcurrentHashMap<>();
	}

	public void addFile(SandboxFile file) {
		files.put(file.getName(), file);
	}

	public SandboxFile getFile(String fileName) {
		return files.get(fileName);
	}

	public boolean hasFile(String fileName) {
		return files.containsKey(fileName);
	}

	public void removeFile(String fileName) {
		files.remove(fileName);
	}

	// Getters
	public String getPlanId() {
		return planId;
	}

	public Path getSandboxRoot() {
		return sandboxRoot;
	}

	public Path getUploadsDir() {
		return uploadsDir;
	}

	public Path getProcessedDir() {
		return processedDir;
	}

	public Path getTempDir() {
		return tempDir;
	}

	public LocalDateTime getCreatedTime() {
		return createdTime;
	}

	public Map<String, SandboxFile> getFiles() {
		return files;
	}

	@Override
	public String toString() {
		return "SandboxInstance{" + "planId='" + planId + '\'' + ", fileCount=" + files.size() + ", createdTime="
				+ createdTime + '}';
	}

}
