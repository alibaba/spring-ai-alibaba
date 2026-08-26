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
package com.alibaba.cloud.ai.graph.agent.extension.tools.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EditFileToolTest {

	@TempDir
	Path tempDir;

	private Path testFile;

	@BeforeEach
	void setUp() throws IOException {
		testFile = tempDir.resolve("test_edit.txt");
	}

	@Test
	void testEditEmptyFileWithEmptyOldString() throws IOException {
		Files.createFile(testFile);

		String result = EditFileTool.editFileContent(testFile, "", "new content", false);

		assertTrue(result.contains("Successfully wrote to empty file"));
		assertEquals("new content", Files.readString(testFile));
	}

	@Test
	void testEditNonEmptyFileWithEmptyOldString() throws IOException {
		Files.writeString(testFile, "existing content");

		String result = EditFileTool.editFileContent(testFile, "", "new content", false);

		assertTrue(result.contains("String not found"));
		assertEquals("existing content", Files.readString(testFile));
	}

	@Test
	void testNormalReplace() throws IOException {
		Files.writeString(testFile, "hello world");

		String result = EditFileTool.editFileContent(testFile, "hello", "hi", false);

		assertTrue(result.contains("Successfully edited file"));
		assertEquals("hi world", Files.readString(testFile));
	}

	@Test
	void testNormalReplaceAll() throws IOException {
		Files.writeString(testFile, "aaa bbb aaa");

		String result = EditFileTool.editFileContent(testFile, "aaa", "ccc", true);

		assertTrue(result.contains("Successfully edited file"));
		assertEquals("ccc bbb ccc", Files.readString(testFile));
	}
}
