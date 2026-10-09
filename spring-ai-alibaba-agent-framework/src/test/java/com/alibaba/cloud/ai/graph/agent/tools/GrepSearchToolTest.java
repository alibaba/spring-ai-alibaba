/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.agent.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.model.ToolContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrepSearchToolTest {

	@TempDir
	Path tempDir;

	private GrepSearchTool grepSearchTool;

	private ToolContext toolContext;

	@BeforeEach
	void setUp() {
		grepSearchTool = new GrepSearchTool(tempDir.toString(), false, 1);
		toolContext = new ToolContext(Collections.emptyMap());
	}

	@Test
	void nullPatternReturnsError() {
		String result = grepSearchTool.apply(new GrepSearchTool.Request(null, "/", null, null), toolContext);

		assertEquals("Error: Pattern is required", result);
	}

	@Test
	void emptyPatternReturnsError() {
		String result = grepSearchTool.apply(new GrepSearchTool.Request("", "/", null, null), toolContext);

		assertEquals("Error: Pattern is required", result);
	}

	@Test
	void whitespacePatternSearchesFiles() throws Exception {
		Files.writeString(tempDir.resolve("example.txt"), "hello world");

		String result = grepSearchTool.apply(new GrepSearchTool.Request(" ", "/", null, null), toolContext);

		assertTrue(result.contains("example.txt"));
	}

	@Test
	void validPatternSearchesFiles() throws Exception {
		Files.writeString(tempDir.resolve("example.txt"), "hello world");

		String result = grepSearchTool.apply(new GrepSearchTool.Request("hello", "/", null, null), toolContext);

		assertTrue(result.contains("example.txt"));
	}

}
