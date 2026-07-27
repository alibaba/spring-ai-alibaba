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

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses SKILL.md YAML frontmatter and body.
 *
 * @since 1.0.0.3
 */
public final class SkillMdParser {

	private static final Pattern FRONTMATTER = Pattern.compile("^---\\s*\\n(.*?)\\n---\\s*\\n?(.*)$", Pattern.DOTALL);

	private SkillMdParser() {
	}

	public static ParsedSkillMd parse(String content) {
		if (StringUtils.isBlank(content)) {
			throw new IllegalArgumentException("SKILL.md is empty");
		}

		Matcher matcher = FRONTMATTER.matcher(content.trim());
		if (!matcher.matches()) {
			throw new IllegalArgumentException("SKILL.md must start with YAML frontmatter (---)");
		}

		Map<String, String> meta = parseSimpleYaml(matcher.group(1));
		String name = StringUtils.trimToNull(meta.get("name"));
		String description = StringUtils.trimToNull(meta.get("description"));
		if (name == null) {
			throw new IllegalArgumentException("SKILL.md frontmatter missing required field: name");
		}
		if (description == null) {
			throw new IllegalArgumentException("SKILL.md frontmatter missing required field: description");
		}
		if (name.length() > 64) {
			throw new IllegalArgumentException("SKILL.md name exceeds 64 characters");
		}

		ParsedSkillMd parsed = new ParsedSkillMd();
		parsed.setName(name);
		parsed.setDescription(description);
		parsed.setBody(matcher.group(2) == null ? "" : matcher.group(2).trim());
		parsed.setRaw(content);
		return parsed;
	}

	/**
	 * Simple key: value YAML parser for flat string fields.
	 */
	private static Map<String, String> parseSimpleYaml(String yaml) {
		Map<String, String> result = new HashMap<>();
		String[] lines = yaml.split("\\n");
		String currentKey = null;
		StringBuilder multiline = null;
		for (String line : lines) {
			if (multiline != null) {
				if (line.startsWith("  ") || line.startsWith("\t") || line.isBlank()) {
					if (!line.isBlank()) {
						if (multiline.length() > 0) {
							multiline.append(' ');
						}
						multiline.append(line.trim());
					}
					continue;
				}
				result.put(currentKey, multiline.toString().trim());
				multiline = null;
				currentKey = null;
			}

			int idx = line.indexOf(':');
			if (idx <= 0) {
				continue;
			}
			String key = line.substring(0, idx).trim();
			String value = line.substring(idx + 1).trim();
			if (value.startsWith("|") || value.startsWith(">")) {
				currentKey = key;
				multiline = new StringBuilder();
				continue;
			}
			if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
				value = value.substring(1, value.length() - 1);
			}
			result.put(key, value);
		}
		if (multiline != null && currentKey != null) {
			result.put(currentKey, multiline.toString().trim());
		}
		return result;
	}

	@Data
	public static class ParsedSkillMd {

		private String name;

		private String description;

		private String body;

		private String raw;

	}

}
