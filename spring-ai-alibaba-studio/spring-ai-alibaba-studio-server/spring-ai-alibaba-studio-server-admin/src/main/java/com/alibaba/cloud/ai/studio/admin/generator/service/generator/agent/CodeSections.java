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
package com.alibaba.cloud.ai.studio.admin.generator.service.generator.agent;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author yHong
 * @version 1.0
 * @since 2025/8/28 17:55
 */
public class CodeSections {

	private final Set<String> imports = new LinkedHashSet<>();

	private String code = "";

	private String varName = "";

	private boolean hasResolver = false;

	public Set<String> getImports() {
		return imports;
	}

	public String getCode() {
		return code;
	}

	public String getVarName() {
		return varName;
	}

	public boolean isHasResolver() {
		return hasResolver;
	}

	public CodeSections imports(String... lines) {
		for (String l : lines) {
			if (l != null && !l.isBlank())
				imports.add(l);
		}
		return this;
	}

	public CodeSections code(String code) {
		this.code = code;
		return this;
	}

	public CodeSections var(String varName) {
		this.varName = varName;
		return this;
	}

	public CodeSections resolver(boolean v) {
		this.hasResolver = v;
		return this;
	}

	public CodeSections merge(CodeSections other) {
		this.imports.addAll(other.imports);
		// 合并 code 时留给上层按序拼接，这里只保留当前节点的片段
		if (other.hasResolver)
			this.hasResolver = true;
		return this;
	}

}
