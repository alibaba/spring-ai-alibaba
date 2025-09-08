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
