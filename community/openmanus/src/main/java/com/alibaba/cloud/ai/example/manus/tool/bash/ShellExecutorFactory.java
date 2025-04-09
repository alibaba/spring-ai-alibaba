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
package com.alibaba.cloud.ai.example.manus.tool.bash;

/**
 * Shell执行器工厂类 负责创建对应操作系统的Shell执行器
 */
public class ShellExecutorFactory {

	/**
	 * 创建对应当前操作系统的Shell执行器
	 * @return ShellCommandExecutor实现
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
	 * 创建指定操作系统类型的Shell执行器
	 * @param osType 操作系统类型：windows/mac/linux
	 * @return ShellCommandExecutor实现
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
