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

import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.permission.ToolPermissionPolicy.Decision;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.Normalizer;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Path rules for the local tools registered by FilesystemInterceptor. Use these tool names
 * only for callbacks that follow its argument and filesystem contracts.
 */
public final class FilesystemPermissions {

	public enum Access {
		READ, WRITE
	}

	/** A rule on one absolute file path or on a directory and all of its descendants. */
	public record Rule(Access access, Path path, boolean recursive, Decision decision) {

		public Rule {
			Objects.requireNonNull(access, "access");
			Objects.requireNonNull(path, "path");
			Objects.requireNonNull(decision, "decision");
			if (!path.isAbsolute()) {
				throw new IllegalArgumentException("Filesystem rule paths must be absolute");
			}
			path = path.normalize();
		}

		public static Rule file(Access access, Path path, Decision decision) {
			return new Rule(access, path, false, decision);
		}

		public static Rule tree(Access access, Path path, Decision decision) {
			return new Rule(access, path, true, decision);
		}

	}

	private static final ObjectReader ARGUMENTS = new ObjectMapper().reader()
			.with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
			.with(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);

	private FilesystemPermissions() {
	}

	/** Creates an ordered rule for the built-in filesystem tools, with its own fallback decision. */
	public static ToolPermissionPolicy.Rule rule(Decision fallback, Rule... rules) {
		Objects.requireNonNull(fallback, "fallback");
		List<Rule> orderedRules = List.of(rules);
		return request -> {
			String name = request.getToolName();
			if (name == null) {
				return Optional.empty();
			}
			return switch (name) {
				case "read_file", "write_file", "edit_file", "ls", "glob", "grep" ->
					Optional.of(evaluate(request, orderedRules, fallback));
				default -> Optional.empty();
			};
		};
	}

	private static Decision evaluate(ToolCallRequest request, List<Rule> rules, Decision fallback) {
		String name = request.getToolName();
		Access access = name.equals("write_file") || name.equals("edit_file") ? Access.WRITE : Access.READ;
		boolean bulk = name.equals("ls") || name.equals("glob") || name.equals("grep");
		Path target;
		try {
			target = resolveTarget(name, bulk, request.getArguments());
		}
		catch (IOException | IllegalArgumentException | SecurityException ex) {
			return Decision.DENY;
		}
		Decision partial = Decision.ALLOW;
		for (Rule rule : rules) {
			if (rule.access() != access) {
				continue;
			}
			boolean conservative = rule.decision() != Decision.ALLOW;
			boolean startsAtRule = startsWith(target, rule.path(), conservative);
			if (startsAtRule && (rule.recursive() || (!bulk && target.getNameCount() == rule.path().getNameCount()))) {
				return mostRestrictive(partial, rule.decision());
			}
			if (bulk && conservative && startsWith(rule.path(), target, true)) {
				partial = mostRestrictive(partial, rule.decision());
			}
		}
		return mostRestrictive(partial, fallback);
	}

	private static Decision mostRestrictive(Decision first, Decision second) {
		if (first == Decision.DENY || second == Decision.DENY) {
			return Decision.DENY;
		}
		return first == Decision.REVIEW || second == Decision.REVIEW ? Decision.REVIEW : Decision.ALLOW;
	}

	private static boolean startsWith(Path path, Path prefix, boolean conservative) {
		if (path.startsWith(prefix)) {
			return true;
		}
		if (!conservative || !Objects.equals(path.getRoot(), prefix.getRoot())
				|| path.getNameCount() < prefix.getNameCount()) {
			return false;
		}
		for (int i = 0; i < prefix.getNameCount(); i++) {
			// Restrictive rules also protect alternate spellings on local filesystems
			// that compare names without case or Unicode normalization differences.
			String actual = Normalizer.normalize(path.getName(i).toString(), Normalizer.Form.NFD);
			String expected = Normalizer.normalize(prefix.getName(i).toString(), Normalizer.Form.NFD);
			if (!actual.equalsIgnoreCase(expected)) {
				return false;
			}
		}
		return true;
	}

	private static Path resolveTarget(String name, boolean bulk, String arguments) throws IOException {
		JsonNode json = ARGUMENTS.readTree(arguments);
		if (json == null || !json.isObject()) {
			throw new IllegalArgumentException("Filesystem tool arguments must be an object");
		}
		JsonNode path = json.get(bulk ? "path" : "file_path");
		Path requested;
		if (name.equals("glob") || (name.equals("grep") && (path == null || path.isNull()))) {
			requested = Path.of(System.getProperty("user.dir"));
		}
		else if (path != null && path.isTextual() && !path.textValue().isBlank()) {
			requested = Path.of(path.textValue());
		}
		else {
			throw new IllegalArgumentException("Filesystem tool path must be a non-blank string");
		}
		Path absolute = requested.toAbsolutePath();
		Path current = absolute.getRoot();
		boolean missing = false;
		for (Path segment : absolute) {
			if (segment.toString().equals("..")) {
				throw new InvalidPathException(requested.toString(), "Parent segments are forbidden");
			}
			current = current.resolve(segment);
			if (missing) {
				continue;
			}
			try {
				if (Files.readAttributes(current, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS).isSymbolicLink()) {
					throw new InvalidPathException(requested.toString(), "Symbolic links are forbidden");
				}
			}
			catch (NoSuchFileException ex) {
				missing = true;
			}
		}
		return absolute.normalize();
	}

}
