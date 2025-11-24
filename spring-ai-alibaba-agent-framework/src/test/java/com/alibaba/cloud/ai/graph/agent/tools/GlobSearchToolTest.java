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
package com.alibaba.cloud.ai.graph.agent.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.model.ToolContext;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;


class GlobSearchToolTest {

	@TempDir
	Path tempDir;

	private GlobSearchTool globSearchTool;

	private ToolContext toolContext;

	@BeforeEach
	void setUp() {
		globSearchTool = new GlobSearchTool(tempDir.toString());
		toolContext = new ToolContext(Collections.emptyMap());
	}

	@Test
	void testValidGlobPattern() throws IOException {
		Files.createFile(tempDir.resolve("test.txt"));
		Files.createFile(tempDir.resolve("test.java"));

		GlobSearchTool.Request request = new GlobSearchTool.Request("*.txt", "/");
		String result = globSearchTool.apply(request, toolContext);

		assertNotNull(result);
		assertFalse(result.startsWith("Error:"));
		assertTrue(result.contains("test.txt"));
		assertFalse(result.contains("test.java"));
	}

	@Test
	void testInvalidGlobPattern() {
		GlobSearchTool.Request request = new GlobSearchTool.Request("[", "/");
		String result = globSearchTool.apply(request, toolContext);
		assertTrue(result.startsWith("Error:"));
		assertTrue(result.contains("Invalid glob pattern syntax"));
	}

	@Test
	void testPathTraversalAttack() {
		GlobSearchTool.Request request = new GlobSearchTool.Request("*.txt", "/../etc");
		String result = globSearchTool.apply(request, toolContext);
		assertTrue(result.startsWith("Error:"));
	}

	@Test
	void testNonExistentPath() {
		GlobSearchTool.Request request = new GlobSearchTool.Request("*.txt", "/nonexistent");
		String result = globSearchTool.apply(request, toolContext);
		assertEquals("No files found", result);
	}

	@Test
	void testEmptyDirectory() throws IOException {
		Path emptyDir = tempDir.resolve("empty");
		Files.createDirectory(emptyDir);

		GlobSearchTool.Request request = new GlobSearchTool.Request("*.txt", "/empty");
		String result = globSearchTool.apply(request, toolContext);

		assertEquals("No files found", result);
	}

	@Test
	void testAccessDeniedOnPosixSystems() throws IOException {
		if (!FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
			return;
		}

		Path restrictedDir = tempDir.resolve("restricted");
		Files.createDirectory(restrictedDir);

		Set<PosixFilePermission> noPerms = new HashSet<>();
		Files.setPosixFilePermissions(restrictedDir, noPerms);

		try {
			GlobSearchTool.Request request = new GlobSearchTool.Request("*.txt", "/restricted");
			String result = globSearchTool.apply(request, toolContext);

			assertTrue(result.equals("No files found") || result.startsWith("Error:"));
		}
		finally {
			Set<PosixFilePermission> restorePerms = new HashSet<>();
			restorePerms.add(PosixFilePermission.OWNER_READ);
			restorePerms.add(PosixFilePermission.OWNER_WRITE);
			restorePerms.add(PosixFilePermission.OWNER_EXECUTE);
			Files.setPosixFilePermissions(restrictedDir, restorePerms);
		}
	}

	@Test
	void testPatternMatchingAccuracy() throws IOException {
		Files.createFile(tempDir.resolve("test.txt"));
		Files.createFile(tempDir.resolve("test.java"));
		Files.createFile(tempDir.resolve("readme.md"));

		GlobSearchTool.Request request = new GlobSearchTool.Request("*.txt", "/");
		String result = globSearchTool.apply(request, toolContext);

		assertTrue(result.contains("test.txt"));
		assertFalse(result.contains("test.java"));
		assertFalse(result.contains("readme.md"));
	}

	@Test
	void testExceptionTypesExist() {
		assertNotNull(java.util.regex.PatternSyntaxException.class);
		assertNotNull(InvalidPathException.class);
		assertNotNull(IOException.class);

		try {
			throw new java.util.regex.PatternSyntaxException("test", "pattern", 0);
		}
		catch (java.util.regex.PatternSyntaxException e) {
			assertNotNull(e);
		}

		try {
			throw new InvalidPathException("test", "reason");
		}
		catch (InvalidPathException e) {
			assertNotNull(e);
		}
	}

}
