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
package com.alibaba.cloud.ai.graph.agent.permission;

import com.alibaba.cloud.ai.graph.agent.extension.tools.filesystem.GrepTool;
import com.alibaba.cloud.ai.graph.agent.extension.tools.filesystem.GrepTool.GrepRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.permission.FilesystemPermissions.Access;
import com.alibaba.cloud.ai.graph.agent.permission.FilesystemPermissions.Rule;
import com.alibaba.cloud.ai.graph.agent.permission.ToolPermissionPolicy.Decision;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.model.ToolContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class FilesystemPermissionsTest {

	private static final ObjectMapper JSON = new ObjectMapper();

	@Test
	void supportsPointRulesAndProtectsSearchScopes(@TempDir Path directory) throws Exception {
		Path root = directory.toRealPath();
		Path privateFile = root.resolve("private.txt");
		Path publicArea = root.resolve("public");
		Path reports = root.resolve("reports");
		var policy = ToolPermissionPolicy.ordered(Decision.DENY, FilesystemPermissions.rule(Decision.DENY,
				Rule.tree(Access.READ, reports, Decision.REVIEW),
				Rule.file(Access.READ, privateFile, Decision.DENY),
				Rule.tree(Access.READ, root, Decision.ALLOW),
				Rule.tree(Access.WRITE, reports, Decision.REVIEW)));

		assertEquals(Decision.ALLOW, policy.evaluate(request("read_file", "file_path", publicArea.resolve("readme.md"))));
		assertEquals(Decision.DENY, policy.evaluate(request("read_file", "file_path", privateFile)));
		assertEquals(Decision.DENY, policy.evaluate(request("read_file", "file_path", root.resolve("PRIVATE.txt"))));
		assertEquals(Decision.REVIEW, policy.evaluate(request("write_file", "file_path", reports.resolve("new.txt"))));
		assertEquals(Decision.DENY, policy.evaluate(request("edit_file", "file_path", publicArea.resolve("readme.md"))));
		assertEquals(Decision.ALLOW, policy.evaluate(request("ls", "path", publicArea)));
		assertEquals(Decision.REVIEW, policy.evaluate(request("grep", "path", reports)));
		assertEquals(Decision.DENY, policy.evaluate(request("grep", "path", root)));
		assertEquals(Decision.DENY, policy.evaluate(request("ls", "path", root.resolveSibling(root.getFileName() + "-other"))));
		assertEquals(Decision.DENY, policy.evaluate(request("read_file", "file_path", publicArea.resolve("../private.txt"))));
		String allowedPath = JSON.writeValueAsString(publicArea.resolve("readme.md").toString());
		assertEquals(Decision.DENY, policy.evaluate(ToolCallRequest.builder().toolName("read_file")
				.arguments("{\"file_path\":" + allowedPath + ",\"file_path\":" + allowedPath + "}").build()));
		assertEquals(Decision.DENY, policy.evaluate(ToolCallRequest.builder().toolName("glob")
				.arguments("{\"pattern\":\"**/*\"}").build()));
	}

	@Test
	void rejectsSymlinkTargetsAndSearchDoesNotReadSymlinkFiles(@TempDir Path directory) throws Exception {
		Path root = directory.toRealPath();
		Path publicArea = Files.createDirectory(root.resolve("public"));
		Path privateFile = Files.writeString(root.resolve("private.txt"), "hidden text");
		try {
			Files.createSymbolicLink(publicArea.resolve("alias.txt"), privateFile);
			Files.createSymbolicLink(publicArea.resolve("parent"), root);
		}
		catch (IOException | UnsupportedOperationException | SecurityException ex) {
			assumeTrue(false, "Symbolic links unavailable in the temporary directory: " + ex);
		}

		var policy = ToolPermissionPolicy.ordered(Decision.DENY, FilesystemPermissions.rule(Decision.DENY,
				Rule.tree(Access.READ, publicArea, Decision.ALLOW), Rule.tree(Access.WRITE, publicArea, Decision.ALLOW)));
		assertEquals(Decision.DENY, policy.evaluate(request("read_file", "file_path", publicArea.resolve("alias.txt"))));
		assertEquals(Decision.DENY, policy.evaluate(request("write_file", "file_path", publicArea.resolve("parent/new.txt"))));
		assertEquals(Decision.ALLOW, policy.evaluate(request("grep", "path", publicArea)));
		String search = new GrepTool().apply(new GrepRequest("hidden text", publicArea.toString(), null, "content"),
				new ToolContext(Map.of()));
		assertFalse(search.contains("alias.txt"));
	}

	private static ToolCallRequest request(String name, String field, Path path) throws JsonProcessingException {
		return ToolCallRequest.builder().toolName(name).arguments(JSON.writeValueAsString(Map.of(field, path.toString())))
				.build();
	}

}
