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
package com.alibaba.cloud.ai.graph.store.stores;

import com.alibaba.cloud.ai.graph.store.StoreItem;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class FileSystemStoreEncodingTest {

	@TempDir
	Path directory;

	@Test
	void persistsUtf8WhenTheWriterUsesALegacyDefaultCharset() throws Exception {
		String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
		String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
		Process writer = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", executable).toString(),
				"-Xmx128m", "-Dfile.encoding=GBK", "-cp", classpath, LegacyCharsetWriter.class.getName(),
				directory.toString())
			.inheritIO()
			.start();
		try {
			assertThat(writer.waitFor(30, TimeUnit.SECONDS)).isTrue();
			assertThat(writer.exitValue()).isZero();
		}
		finally {
			writer.destroyForcibly();
		}

		assertThat(Files.readString(directory.resolve("users/profile.json"))).contains("你好 🌍");
		FileSystemStore reader = new FileSystemStore(directory);
		assertThat(reader.getItem(List.of("users"), "profile")).isPresent();
		assertThat(reader.getItem(List.of("users"), "profile").orElseThrow().getValue()).containsEntry("greeting",
				"你好 🌍");
	}

	public static class LegacyCharsetWriter {

		public static void main(String[] args) {
			if (!Charset.defaultCharset().equals(Charset.forName("GBK"))) {
				throw new IllegalStateException("Expected GBK writer default charset");
			}
			new FileSystemStore(Path.of(args[0]))
				.putItem(StoreItem.of(List.of("users"), "profile", Map.of("greeting", "你好 🌍")));
		}

	}

}
