/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent.tools;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GrepSearchToolIncludePatternTest {

	@TempDir
	Path root;

	@Test
	void includeGlobTreatsRegexMetacharactersAsLiteral() throws Exception {
		Files.writeString(root.resolve("report+(1).txt"), "needle\n");
		Files.writeString(root.resolve("reporttt1.txt"), "needle\n");
		GrepSearchTool tool = new GrepSearchTool(root.toString(), false, 10);

		assertEquals("/report+(1).txt", tool.apply(
				new GrepSearchTool.Request("needle", "/", "report+(1).*", "files_with_matches"), null));
	}

	@Test
	void includeGlobPreservesBraceAlternativesAndCharacterClasses() throws Exception {
		Files.writeString(root.resolve("file1.ts"), "needle\n");
		Files.writeString(root.resolve("file2.tsx"), "needle\n");
		Files.writeString(root.resolve("file3.js"), "needle\n");
		GrepSearchTool tool = new GrepSearchTool(root.toString(), false, 10);

		assertEquals("/file1.ts\n/file2.tsx", tool.apply(
				new GrepSearchTool.Request("needle", "/", "file[12].{ts,tsx}", "files_with_matches"), null));
	}

}
