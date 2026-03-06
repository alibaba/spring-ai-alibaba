/*
 * Copyright 2024-2026 the original author or authors.
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

package com.alibaba.cloud.ai.sandbox.config;

import com.alibaba.cloud.ai.sandbox.ToolkitInit;
import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.box.BrowserSandbox;
import io.agentscope.runtime.sandbox.box.FilesystemSandbox;
import io.agentscope.runtime.sandbox.box.Sandbox;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SandboxToolRegistry {

	private final List<ToolCallback> tools;

	private final ToolMatchStrategy toolMatchStrategy;

	public SandboxToolRegistry(List<Sandbox> sandboxes, ToolMatchStrategy toolMatchStrategy) {
		this.toolMatchStrategy = toolMatchStrategy;
		this.tools = buildTools(sandboxes);
	}

	public List<ToolCallback> match(String pattern) {
		if (!StringUtils.hasText(pattern)) {
			return List.of();
		}
		if (!hasGlob(pattern)) {
			return this.tools.stream().filter(tool -> toolName(tool).equals(pattern)).toList();
		}
		return this.tools.stream().filter(tool -> this.toolMatchStrategy.matches(pattern, toolName(tool))).toList();
	}

	public List<ToolCallback> getTools() {
		return this.tools;
	}

	private List<ToolCallback> buildTools(List<Sandbox> sandboxes) {
		if (sandboxes == null || sandboxes.isEmpty()) {
			return List.of();
		}
		List<ToolCallback> callbacks = new ArrayList<>();
		for (Sandbox sandbox : sandboxes) {
			callbacks.addAll(buildToolsForSandbox(sandbox));
		}
		callbacks.sort(Comparator.comparing(this::toolName));
		return Collections.unmodifiableList(callbacks);
	}

	private List<ToolCallback> buildToolsForSandbox(Sandbox sandbox) {
		if (sandbox instanceof BaseSandbox) {
			return List.of(ToolkitInit.RunPythonCodeTool(sandbox), ToolkitInit.RunShellCommandTool(sandbox),
					ToolkitInit.WebFetchTool(sandbox));
		}
		if (sandbox instanceof BrowserSandbox) {
			return List.of(ToolkitInit.BrowserNavigateTool(sandbox), ToolkitInit.BrowserClickTool(sandbox),
					ToolkitInit.BrowserTypeTool(sandbox), ToolkitInit.BrowserTakeScreenshotTool(sandbox),
					ToolkitInit.BrowserSnapshotTool(sandbox), ToolkitInit.BrowserTabNewTool(sandbox),
					ToolkitInit.BrowserTabSelectTool(sandbox), ToolkitInit.BrowserTabCloseTool(sandbox),
					ToolkitInit.BrowserWaitForTool(sandbox), ToolkitInit.BrowserResizeTool(sandbox),
					ToolkitInit.BrowserCloseTool(sandbox), ToolkitInit.BrowserConsoleMessagesTool(sandbox),
					ToolkitInit.BrowserHandleDialogTool(sandbox), ToolkitInit.BrowserFileUploadTool(sandbox),
					ToolkitInit.BrowserPressKeyTool(sandbox), ToolkitInit.BrowserNavigateBackTool(sandbox),
					ToolkitInit.BrowserNavigateForwardTool(sandbox), ToolkitInit.BrowserNetworkRequestsTool(sandbox),
					ToolkitInit.BrowserPdfSaveTool(sandbox), ToolkitInit.BrowserDragTool(sandbox),
					ToolkitInit.BrowserHoverTool(sandbox), ToolkitInit.BrowserSelectOptionTool(sandbox),
					ToolkitInit.BrowserTabListTool(sandbox));
		}
		if (sandbox instanceof FilesystemSandbox) {
			return List.of(ToolkitInit.ReadFileTool(sandbox), ToolkitInit.ReadMultipleFilesTool(sandbox),
					ToolkitInit.WriteFileTool(sandbox), ToolkitInit.EditFileTool(sandbox),
					ToolkitInit.CreateDirectoryTool(sandbox), ToolkitInit.ListDirectoryTool(sandbox),
					ToolkitInit.DirectoryTreeTool(sandbox), ToolkitInit.MoveFileTool(sandbox),
					ToolkitInit.SearchFilesTool(sandbox), ToolkitInit.GetFileInfoTool(sandbox),
					ToolkitInit.ListAllowedDirectoriesTool(sandbox));
		}
		return ToolkitInit.getAllTools(sandbox);
	}

	private String toolName(ToolCallback toolCallback) {
		return toolCallback.getToolDefinition().name();
	}

	private boolean hasGlob(String pattern) {
		return pattern.contains("*") || pattern.contains("?");
	}

}
