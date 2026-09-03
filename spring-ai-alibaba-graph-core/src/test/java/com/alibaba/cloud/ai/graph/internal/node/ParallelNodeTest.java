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
package com.alibaba.cloud.ai.graph.internal.node;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParallelNodeTest {

	@Test
	void initializesWithMoreThanOneHundredProcessors() throws Exception {
		String javaCommand = ProcessHandle.current().info().command().orElseThrow();
		String classPath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
		Process process = new ProcessBuilder(javaCommand, "-XX:ActiveProcessorCount=128", "-cp",
				classPath, ParallelNodeLoader.class.getName()).redirectErrorStream(true)
				.start();

		boolean exited = process.waitFor(30, TimeUnit.SECONDS);
		if (!exited) {
			process.destroyForcibly();
		}
		assertTrue(exited, "child JVM did not exit in time");
		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		assertEquals(0, process.exitValue(), output);
	}

	static class ParallelNodeLoader {

		public static void main(String[] args) throws ClassNotFoundException {
			Class.forName(ParallelNode.class.getName());
		}

	}

}
