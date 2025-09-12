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
package com.alibaba.cloud.ai.manus.tool.bash;

import java.util.List;

/**
 * Shell command executor interface. Provides cross-platform (Windows/Linux/Mac) shell
 * command execution capability
 */
public interface ShellCommandExecutor {

	/**
	 * Execute shell commands
	 * @param commands List of commands to execute
	 * @param workingDir Working directory
	 * @return List of command execution results
	 */
	List<String> execute(List<String> commands, String workingDir);

	/**
	 * Terminate the currently executing process
	 */
	void terminate();

	/**
	 * Get the current system type
	 * @return System type (windows/linux/mac)
	 */
	default String getOsType() {
		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("win")) {
			return "windows";
		}
		else if (os.contains("mac")) {
			return "mac";
		}
		else {
			return "linux";
		}
	}

}
