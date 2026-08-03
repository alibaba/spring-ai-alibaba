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

package com.alibaba.cloud.ai.studio.core.agent.skill;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Zip helpers for skill package extraction with path traversal protection.
 *
 * @since 1.0.0.3
 */
public final class SkillPackageUtils {

	public static final String SKILL_MD = "SKILL.md";

	private SkillPackageUtils() {
	}

	public static void extractZip(InputStream zipStream, Path targetDir) throws IOException {
		Files.createDirectories(targetDir);
		try (ZipInputStream zis = new ZipInputStream(zipStream)) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				String name = entry.getName();
				if (StringUtils.isBlank(name) || name.contains("..")) {
					throw new IOException("Illegal zip entry path: " + name);
				}
				Path dest = targetDir.resolve(name).normalize();
				if (!dest.startsWith(targetDir.normalize())) {
					throw new IOException("Zip entry escapes target directory: " + name);
				}
				if (entry.isDirectory()) {
					Files.createDirectories(dest);
				}
				else {
					Files.createDirectories(dest.getParent());
					try (OutputStream os = Files.newOutputStream(dest)) {
						zis.transferTo(os);
					}
				}
			}
		}
	}

	/**
	 * Resolves the skill root directory that contains SKILL.md.
	 */
	public static Path resolveSkillRoot(Path extractDir) throws IOException {
		Path direct = extractDir.resolve(SKILL_MD);
		if (Files.isRegularFile(direct)) {
			return extractDir;
		}

		Path nested = null;
		try (Stream<Path> stream = Files.list(extractDir)) {
			for (Path child : stream.toList()) {
				if (Files.isDirectory(child) && Files.isRegularFile(child.resolve(SKILL_MD))) {
					if (nested != null) {
						throw new IOException("Multiple skill directories found; package must contain exactly one skill");
					}
					nested = child;
				}
			}
		}
		if (nested == null) {
			throw new IOException("SKILL.md not found in package root or a single subdirectory");
		}
		return nested;
	}

	public static void deleteRecursively(Path path) throws IOException {
		if (path == null || !Files.exists(path)) {
			return;
		}
		try (Stream<Path> walk = Files.walk(path)) {
			walk.sorted(Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.deleteIfExists(p);
				}
				catch (IOException ignored) {
					// best effort
				}
			});
		}
	}

}
