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

/**
 * Shell executor factory class responsible for creating shell executors for corresponding
 * operating systems
 */
public class ShellExecutorFactory {

	/**
	 * Create shell executor for current operating system
	 * @return ShellCommandExecutor implementation
	 */
	public static ShellCommandExecutor createExecutor() {
		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("win")) {
			return new WindowsShellExecutor();
		}
		else if (os.contains("mac")) {
			return new MacShellExecutor();
		}
		else {
			return new LinuxShellExecutor();
		}
	}

	/**
	 * Create shell executor for specified operating system type
	 * @param osType Operating system type: windows/mac/linux
	 * @return ShellCommandExecutor implementation
	 */
	public static ShellCommandExecutor createExecutor(String osType) {
		switch (osType.toLowerCase()) {
			case "windows":
				return new WindowsShellExecutor();
			case "mac":
				return new MacShellExecutor();
			case "linux":
				return new LinuxShellExecutor();
			default:
				throw new IllegalArgumentException("Unsupported OS type: " + osType);
		}
	}

}
