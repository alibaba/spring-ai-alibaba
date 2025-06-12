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
package com.alibaba.cloud.ai.graph.utils;

/**
 * @author HeYQ
 * @since 2025-06-01 20:35
 */

public class CodeUtils {

	public static String getExecutableForLanguage(String language) throws Exception {
		return switch (language) {
			case "python3", "python" -> language;
			case "shell", "bash", "sh", "powershell" -> "sh";
			case "nodejs" -> "node";
			case "java" -> "java";
			default -> throw new Exception("Language not recognized in code execution:" + language);
		};
	}

	public static String getFileExtForLanguage(String language) throws Exception {
		return switch (language) {
			case "python3", "python" -> "py";
			case "shell", "bash", "sh", "powershell" -> "sh";
			case "nodejs" -> "js";
			case "java" -> "java";
			default -> throw new Exception("Language not recognized in code execution:" + language);
		};
	}

}
