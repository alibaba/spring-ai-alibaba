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

package com.alibaba.cloud.ai.graph.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author 014-code
 * @since 2026-05-05
 */
class FileUtilsTest {

	@TempDir
	Path tempDir;

	/**
	 * Verify that JAR files under lib/ can be copied successfully when the resource is
	 * loaded via jar protocol.
	 */
	@Test
	void testCopyResourceJarToWorkDirFromJarProtocol() throws Exception {
		Path resourceJar = tempDir.resolve("resource-bundle.jar");
		createJarWithLibEntry(resourceJar, "lib/mock-dependency.jar");

		Path workDir = tempDir.resolve("work");
		try (URLClassLoader classLoader = new URLClassLoader(new java.net.URL[] { resourceJar.toUri().toURL() },
				null)) {
			FileUtils.copyResourceJarToWorkDir(workDir.toString(), classLoader);
		}

		assertTrue(Files.exists(workDir.resolve("mock-dependency.jar")));
	}

	/**
	 * Creates a temporary JAR file containing the given lib entry for resource loading
	 * tests.
	 * @param jarPath The output JAR path.
	 * @param entryName The entry path inside JAR.
	 * @throws IOException if writing JAR entries fails.
	 */
	private static void createJarWithLibEntry(Path jarPath, String entryName) throws IOException {
		try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarPath))) {
			jarOutputStream.putNextEntry(new JarEntry("lib/"));
			jarOutputStream.closeEntry();
			jarOutputStream.putNextEntry(new JarEntry(entryName));
			jarOutputStream.write(new byte[] { 0x01, 0x02, 0x03 });
			jarOutputStream.closeEntry();
		}
	}

}
