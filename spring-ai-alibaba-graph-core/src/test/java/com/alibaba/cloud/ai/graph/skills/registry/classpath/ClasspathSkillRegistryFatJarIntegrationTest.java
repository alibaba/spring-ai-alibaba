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
package com.alibaba.cloud.ai.graph.skills.registry.classpath;

import org.springframework.boot.loader.tools.Layouts;
import org.springframework.boot.loader.tools.Library;
import org.springframework.boot.loader.tools.LibraryScope;
import org.springframework.boot.loader.tools.Repackager;
import org.springframework.boot.loader.tools.RepackagingLayout;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClasspathSkillRegistryFatJarIntegrationTest {

	private static final String SKILL_CONTENT = """
			---
			name: fat-jar-skill
			description: Verifies loading skills from a Spring Boot executable JAR.
			---

			# Fat JAR Skill
			""";

	@Test
	void loadsSkillFromSpringBootExecutableJar(@TempDir Path tempDir) throws Exception {
		Path executableJar = tempDir.resolve("classpath-skill-test.jar");
		Path dependencyJar = tempDir.resolve("duplicate-skill-dependency.jar");
		Path extractedRoot = tempDir.resolve("extracted");
		Path obsoleteResource = extractedRoot
			.resolve("fat-jar-skills/fat-jar-skill/references/obsolete.md");
		Files.createDirectories(obsoleteResource.getParent());
		Files.writeString(obsoleteResource, "obsolete");
		createApplicationJar(executableJar);
		createDependencyJar(dependencyJar);

		Repackager repackager = new Repackager(executableJar.toFile());
		repackager.setMainClass(ClasspathSkillRegistryFatJarTestApplication.class.getName());
		repackager.setLayout(new PropertiesLauncherJarLayout());
		repackager.repackage(callback -> callback.library(new Library(dependencyJar.toFile(), LibraryScope.COMPILE)));

		Process process = new ProcessBuilder(javaExecutable(), "-Dloader.path=" + testClasspath(), "-jar",
				executableJar.toString(), extractedRoot.toString()).redirectErrorStream(true)
				.start();
		boolean finished = process.waitFor(30, TimeUnit.SECONDS);
		if (!finished) {
			process.destroyForcibly();
		}
		assertTrue(finished, "Executable JAR did not exit within 30 seconds");

		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		assertEquals(0, process.exitValue(), output);
		assertTrue(output.contains("FAT_JAR_SKILLS=[fat-jar-skill]"), output);
		assertTrue(output.contains("FAT_JAR_RELOAD_STAGED=true"), output);
	}

	private void createDependencyJar(Path jarPath) throws IOException {
		try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jarPath))) {
			writeEntry(output, "fat-jar-skills/fat-jar-skill/SKILL.md",
					new java.io.ByteArrayInputStream(SKILL_CONTENT.getBytes(StandardCharsets.UTF_8)));
			writeEntry(output, "fat-jar-skills/fat-jar-skill/references/dependency-only.md",
					new java.io.ByteArrayInputStream("# Dependency only".getBytes(StandardCharsets.UTF_8)));
		}
	}

	private void createApplicationJar(Path jarPath) throws IOException {
		Manifest manifest = new Manifest();
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

		try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jarPath), manifest)) {
			String applicationClass = ClasspathSkillRegistryFatJarTestApplication.class.getName()
					.replace('.', '/')
					+ ".class";
			writeEntry(output, applicationClass,
					ClasspathSkillRegistryFatJarTestApplication.class.getClassLoader()
							.getResourceAsStream(applicationClass));
			writeEntry(output, "fat-jar-skills/fat-jar-skill/SKILL.md",
					new java.io.ByteArrayInputStream(SKILL_CONTENT.getBytes(StandardCharsets.UTF_8)));
			writeEntry(output, "fat-jar-skills/fat-jar-skill/references/reference.md",
					new java.io.ByteArrayInputStream("# Reference".getBytes(StandardCharsets.UTF_8)));
			writeEntry(output, "fat-jar-skills/fat-jar-skill/references/fat-jar-skills/note.md",
					new java.io.ByteArrayInputStream("# Repeated root".getBytes(StandardCharsets.UTF_8)));
		}
	}

	private void writeEntry(JarOutputStream output, String name, InputStream input) throws IOException {
		if (input == null) {
			throw new IOException("Missing test resource: " + name);
		}
		try (input) {
			output.putNextEntry(new JarEntry(name));
			input.transferTo(output);
			output.closeEntry();
		}
	}

	private String javaExecutable() {
		return Path.of(System.getProperty("java.home"), "bin", "java").toString();
	}

	private String testClasspath() {
		String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
		return Arrays.stream(classpath.split(Pattern.quote(File.pathSeparator)))
				.map(Path::of)
				.map(Path::toAbsolutePath)
				.map(Path::toString)
				.reduce((left, right) -> left + "," + right)
				.orElseThrow();
	}

	private static final class PropertiesLauncherJarLayout implements RepackagingLayout {

		private final Layouts.Jar delegate = new Layouts.Jar();

		@Override
		public String getLauncherClassName() {
			return "org.springframework.boot.loader.launch.PropertiesLauncher";
		}

		@Override
		public String getLibraryLocation(String libraryName,
				org.springframework.boot.loader.tools.LibraryScope scope) {
			return delegate.getLibraryLocation(libraryName, scope);
		}

		@Override
		public String getClassesLocation() {
			return delegate.getClassesLocation();
		}

		@Override
		public String getRepackagedClassesLocation() {
			return delegate.getRepackagedClassesLocation();
		}

		@Override
		public boolean isExecutable() {
			return true;
		}

	}

}
