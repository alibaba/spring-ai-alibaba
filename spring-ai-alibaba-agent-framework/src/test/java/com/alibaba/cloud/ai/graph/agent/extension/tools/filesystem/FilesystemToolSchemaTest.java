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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.tool.ToolCallback;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilesystemToolSchemaTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void listFilesUsesObjectInputSchemaAndAcceptsJsonObject(@TempDir Path tempDir) throws Exception {
		Path file = Files.createFile(tempDir.resolve("example.txt"));
		ToolCallback callback = ListFilesTool.createListFilesToolCallback(ListFilesTool.DESCRIPTION);

		assertObjectSchema(callback, "path");
		String result = callback.call(objectMapper.writeValueAsString(Map.of("path", tempDir.toString())));

		assertTrue(result.contains(file.toString()));
	}

	@Test
	void globUsesObjectInputSchemaAndAcceptsJsonObject() throws Exception {
		ToolCallback callback = GlobTool.createGlobToolCallback(GlobTool.DESCRIPTION);

		assertObjectSchema(callback, "pattern");
		String result = callback.call(objectMapper.writeValueAsString(Map.of("pattern", "pom.xml")));

		assertTrue(result.contains("pom.xml"));
	}

	private void assertObjectSchema(ToolCallback callback, String propertyName) throws Exception {
		JsonNode schema = objectMapper.readTree(callback.getToolDefinition().inputSchema());
		assertEquals("object", schema.path("type").asText());
		assertEquals("string", schema.path("properties").path(propertyName).path("type").asText());
		assertTrue(schema.path("required").isArray());
		assertEquals(propertyName, schema.path("required").get(0).asText());
	}
}
