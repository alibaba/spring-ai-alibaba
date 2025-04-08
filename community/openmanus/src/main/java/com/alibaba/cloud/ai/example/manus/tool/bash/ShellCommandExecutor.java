/**
 * Copyright (C) 2024 AIDC-AI
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.example.manus.tool.bash;

import java.util.List;

/**
 * Shell命令执行器接口 提供跨平台(Windows/Linux/Mac)的shell命令执行能力
 */
public interface ShellCommandExecutor {

	/**
	 * 执行shell命令
	 * @param commands 要执行的命令列表
	 * @param workingDir 工作目录
	 * @return 命令执行结果列表
	 */
	List<String> execute(List<String> commands, String workingDir);

	/**
	 * 终止当前正在执行的进程
	 */
	void terminate();

	/**
	 * 获取当前系统类型
	 * @return 系统类型(windows/linux/mac)
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
