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

package com.alibaba.cloud.ai.sandbox;

import com.alibaba.cloud.ai.sandbox.tools.base.SaaBasePythonRunner;
import com.alibaba.cloud.ai.sandbox.tools.base.SaaBaseShellRunner;
import com.alibaba.cloud.ai.sandbox.tools.browser.*;
import com.alibaba.cloud.ai.sandbox.tools.fs.*;
import com.alibaba.cloud.ai.sandbox.tools.mcp.SaaMCPTool;
import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import io.agentscope.runtime.sandbox.tools.MCPTool;
import io.agentscope.runtime.sandbox.tools.McpConfigConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.ai.tool.ToolCallback;

public class ToolkitInit {
    public static Logger logger = LoggerFactory.getLogger(ToolkitInit.class);

    public static List<ToolCallback> getAllTools(Sandbox sandbox) {
        return List.of(
                RunPythonCodeTool(sandbox),
                RunShellCommandTool(sandbox),
                ReadFileTool(sandbox),
                ReadMultipleFilesTool(sandbox),
                WriteFileTool(sandbox),
                EditFileTool(sandbox),
                CreateDirectoryTool(sandbox),
                ListDirectoryTool(sandbox),
                DirectoryTreeTool(sandbox),
                MoveFileTool(sandbox),
                SearchFilesTool(sandbox),
                GetFileInfoTool(sandbox),
                ListAllowedDirectoriesTool(sandbox),
                BrowserNavigateTool(sandbox),
                BrowserClickTool(sandbox),
                BrowserTypeTool(sandbox),
                BrowserTakeScreenshotTool(sandbox),
                BrowserSnapshotTool(sandbox),
                BrowserTabNewTool(sandbox),
                BrowserTabSelectTool(sandbox),
                BrowserTabCloseTool(sandbox),
                BrowserWaitForTool(sandbox),
                BrowserResizeTool(sandbox),
                BrowserCloseTool(sandbox),
                BrowserConsoleMessagesTool(sandbox),
                BrowserHandleDialogTool(sandbox),
                BrowserFileUploadTool(sandbox),
                BrowserPressKeyTool(sandbox),
                BrowserNavigateBackTool(sandbox),
                BrowserNavigateForwardTool(sandbox),
                BrowserNetworkRequestsTool(sandbox),
                BrowserPdfSaveTool(sandbox),
                BrowserDragTool(sandbox),
                BrowserHoverTool(sandbox),
                BrowserSelectOptionTool(sandbox),
                BrowserTabListTool(sandbox)
        );
    }

    // Base tools
    public static ToolCallback RunPythonCodeTool(Sandbox sandbox) {
        SaaBasePythonRunner saaBasePythonRunner = new SaaBasePythonRunner();
        saaBasePythonRunner.setSandbox(sandbox);
        return saaBasePythonRunner.buildTool();
    }

    public static ToolCallback RunShellCommandTool(Sandbox sandbox) {
        SaaBaseShellRunner saaBaseShellRunner = new SaaBaseShellRunner();
        saaBaseShellRunner.setSandbox(sandbox);
        return saaBaseShellRunner.buildTool();
    }

    // Browser tools
    public static ToolCallback BrowserNavigateTool(Sandbox sandbox) {
        SaaBrowserNavigator saaBrowserNavigator = new SaaBrowserNavigator();
        saaBrowserNavigator.setSandbox(sandbox);
        return saaBrowserNavigator.buildTool();
    }

    public static ToolCallback BrowserClickTool(Sandbox sandbox) {
        SaaBrowserClicker saaBrowserClicker = new SaaBrowserClicker();
        saaBrowserClicker.setSandbox(sandbox);
        return saaBrowserClicker.buildTool();
    }

    public static ToolCallback BrowserTypeTool(Sandbox sandbox) {
        SaaBrowserTyper saaBrowserTyper = new SaaBrowserTyper();
        saaBrowserTyper.setSandbox(sandbox);
        return saaBrowserTyper.buildTool();
    }

    public static ToolCallback BrowserSnapshotTool(Sandbox sandbox) {
        SaaBrowserSnapshotTaker saaBrowserSnapshotTaker = new SaaBrowserSnapshotTaker();
        saaBrowserSnapshotTaker.setSandbox(sandbox);
        return saaBrowserSnapshotTaker.buildTool();
    }

    public static ToolCallback BrowserTakeScreenshotTool(Sandbox sandbox) {
        SaaBrowserScreenshotTaker saaBrowserScreenshotTaker = new SaaBrowserScreenshotTaker();
        saaBrowserScreenshotTaker.setSandbox(sandbox);
        return saaBrowserScreenshotTaker.buildTool();
    }

    public static ToolCallback BrowserCloseTool(Sandbox sandbox) {
        SaaBrowserCloser saaBrowserCloser = new SaaBrowserCloser();
        saaBrowserCloser.setSandbox(sandbox);
        return saaBrowserCloser.buildTool();
    }

    public static ToolCallback BrowserHoverTool(Sandbox sandbox) {
        SaaBrowserHoverer saaBrowserHoverer = new SaaBrowserHoverer();
        saaBrowserHoverer.setSandbox(sandbox);
        return saaBrowserHoverer.buildTool();
    }

    public static ToolCallback BrowserDragTool(Sandbox sandbox) {
        SaaBrowserDragger saaBrowserDragger = new SaaBrowserDragger();
        saaBrowserDragger.setSandbox(sandbox);
        return saaBrowserDragger.buildTool();
    }

    public static ToolCallback BrowserConsoleMessagesTool(Sandbox sandbox) {
        SaaBrowserConsoleMessagesRetriever saaBrowserConsoleMessagesRetriever = new SaaBrowserConsoleMessagesRetriever();
        saaBrowserConsoleMessagesRetriever.setSandbox(sandbox);
        return saaBrowserConsoleMessagesRetriever.buildTool();
    }

    public static ToolCallback BrowserFileUploadTool(Sandbox sandbox) {
        SaaBrowserFileUploader saaBrowserFileUploader = new SaaBrowserFileUploader();
        saaBrowserFileUploader.setSandbox(sandbox);
        return saaBrowserFileUploader.buildTool();
    }

    public static ToolCallback BrowserHandleDialogTool(Sandbox sandbox) {
        SaaBrowserDialogHandler saaBrowserDialogHandler = new SaaBrowserDialogHandler();
        saaBrowserDialogHandler.setSandbox(sandbox);
        return saaBrowserDialogHandler.buildTool();
    }

    public static ToolCallback BrowserNavigateBackTool(Sandbox sandbox) {
        SaaBrowserBackNavigator saaBrowserBackNavigator = new SaaBrowserBackNavigator();
        saaBrowserBackNavigator.setSandbox(sandbox);
        return saaBrowserBackNavigator.buildTool();
    }

    public static ToolCallback BrowserNavigateForwardTool(Sandbox sandbox) {
        SaaBrowserForwardNavigator saaBrowserForwardNavigator = new SaaBrowserForwardNavigator();
        saaBrowserForwardNavigator.setSandbox(sandbox);
        return saaBrowserForwardNavigator.buildTool();
    }

    public static ToolCallback BrowserNetworkRequestsTool(Sandbox sandbox) {
        SaaBrowserNetworkRequestsRetriever saaBrowserNetworkRequestsRetriever = new SaaBrowserNetworkRequestsRetriever();
        saaBrowserNetworkRequestsRetriever.setSandbox(sandbox);
        return saaBrowserNetworkRequestsRetriever.buildTool();
    }

    public static ToolCallback BrowserPdfSaveTool(Sandbox sandbox) {
        SaaBrowserPdfSaver saaBrowserPdfSaver = new SaaBrowserPdfSaver();
        saaBrowserPdfSaver.setSandbox(sandbox);
        return saaBrowserPdfSaver.buildTool();
    }

    public static ToolCallback BrowserPressKeyTool(Sandbox sandbox) {
        SaaBrowserKeyPresser saaBrowserKeyPresser = new SaaBrowserKeyPresser();
        saaBrowserKeyPresser.setSandbox(sandbox);
        return saaBrowserKeyPresser.buildTool();
    }

    public static ToolCallback BrowserResizeTool(Sandbox sandbox) {
        SaaBrowserWindowResizer saaBrowserWindowResizer = new SaaBrowserWindowResizer();
        saaBrowserWindowResizer.setSandbox(sandbox);
        return saaBrowserWindowResizer.buildTool();
    }

    public static ToolCallback BrowserSelectOptionTool(Sandbox sandbox) {
        SaaBrowserOptionSelector saaBrowserOptionSelector = new SaaBrowserOptionSelector();
        saaBrowserOptionSelector.setSandbox(sandbox);
        return saaBrowserOptionSelector.buildTool();
    }

    public static ToolCallback BrowserTabCloseTool(Sandbox sandbox) {
        SaaBrowserTabCloser saaBrowserTabCloser = new SaaBrowserTabCloser();
        saaBrowserTabCloser.setSandbox(sandbox);
        return saaBrowserTabCloser.buildTool();
    }

    public static ToolCallback BrowserTabListTool(Sandbox sandbox) {
        SaaBrowserTabLister saaBrowserTabLister = new SaaBrowserTabLister();
        saaBrowserTabLister.setSandbox(sandbox);
        return saaBrowserTabLister.buildTool();
    }

    public static ToolCallback BrowserTabNewTool(Sandbox sandbox) {
        SaaBrowserTabCreator saaBrowserTabCreator = new SaaBrowserTabCreator();
        saaBrowserTabCreator.setSandbox(sandbox);
        return saaBrowserTabCreator.buildTool();
    }

    public static ToolCallback BrowserTabSelectTool(Sandbox sandbox) {
        SaaBrowserTabSelector saaBrowserTabSelector = new SaaBrowserTabSelector();
        saaBrowserTabSelector.setSandbox(sandbox);
        return saaBrowserTabSelector.buildTool();
    }

    public static ToolCallback BrowserWaitForTool(Sandbox sandbox) {
        SaaBrowserWaiter saaBrowserWaiter = new SaaBrowserWaiter();
        saaBrowserWaiter.setSandbox(sandbox);
        return saaBrowserWaiter.buildTool();
    }

    // Filesystem tools
    public static ToolCallback ReadFileTool(Sandbox sandbox) {
        SaaFsFileReader saaFsFileReader = new SaaFsFileReader();
        saaFsFileReader.setSandbox(sandbox);
        return saaFsFileReader.buildTool();
    }

    public static ToolCallback WriteFileTool(Sandbox sandbox) {
        SaaFsFileWriter saaFsFileWriter = new SaaFsFileWriter();
        saaFsFileWriter.setSandbox(sandbox);
        return saaFsFileWriter.buildTool();
    }

    public static ToolCallback ListDirectoryTool(Sandbox sandbox) {
        SaaFsDirectoryLister saaFsDirectoryLister = new SaaFsDirectoryLister();
        saaFsDirectoryLister.setSandbox(sandbox);
        return saaFsDirectoryLister.buildTool();
    }

    public static ToolCallback CreateDirectoryTool(Sandbox sandbox) {
        SaaFsDirectoryCreator saaFsDirectoryCreator = new SaaFsDirectoryCreator();
        saaFsDirectoryCreator.setSandbox(sandbox);
        return saaFsDirectoryCreator.buildTool();
    }

    public static ToolCallback DirectoryTreeTool(Sandbox sandbox) {
        SaaFsTreeBuilder saaFsTreeBuilder = new SaaFsTreeBuilder();
        saaFsTreeBuilder.setSandbox(sandbox);
        return saaFsTreeBuilder.buildTool();
    }

    public static ToolCallback EditFileTool(Sandbox sandbox) {
        SaaFsFileEditor saaFsFileEditor = new SaaFsFileEditor();
        saaFsFileEditor.setSandbox(sandbox);
        return saaFsFileEditor.buildTool();
    }

    public static ToolCallback GetFileInfoTool(Sandbox sandbox) {
        SaaFsFileInfoRetriever saaFsFileInfoRetriever = new SaaFsFileInfoRetriever();
        saaFsFileInfoRetriever.setSandbox(sandbox);
        return saaFsFileInfoRetriever.buildTool();
    }

    public static ToolCallback ListAllowedDirectoriesTool(Sandbox sandbox) {
        SaaFsAllowedDirectoriesLister saaFsAllowedDirectoriesLister = new SaaFsAllowedDirectoriesLister();
        saaFsAllowedDirectoriesLister.setSandbox(sandbox);
        return saaFsAllowedDirectoriesLister.buildTool();
    }

    public static ToolCallback MoveFileTool(Sandbox sandbox) {
        SaaFsFileMover saaFsFileMover = new SaaFsFileMover();
        saaFsFileMover.setSandbox(sandbox);
        return saaFsFileMover.buildTool();
    }

    public static ToolCallback ReadMultipleFilesTool(Sandbox sandbox) {
        SaaFsMultiFileReader saaFsMultiFileReader = new SaaFsMultiFileReader();
        saaFsMultiFileReader.setSandbox(sandbox);
        return saaFsMultiFileReader.buildTool();
    }

    public static ToolCallback SearchFilesTool(Sandbox sandbox) {
        SaaFsFileSearcher saaFsFileSearcher = new SaaFsFileSearcher();
        saaFsFileSearcher.setSandbox(sandbox);
        return saaFsFileSearcher.buildTool();
    }

    public static List<ToolCallback> getMcpTools(String serverConfigs,
                                              String sandboxType,
                                              SandboxService sandboxService) {
        return getMcpTools(serverConfigs, sandboxType, sandboxService, null, null);
    }

    public static List<ToolCallback> getMcpTools(Map<String, Object> serverConfigs,
                                              String sandboxType,
                                              SandboxService sandboxService) {
        return getMcpTools(serverConfigs, sandboxType, sandboxService, null, null);
    }

    public static List<ToolCallback> getMcpTools(String serverConfigs,
                                              String sandboxType,
                                              SandboxService sandboxService,
                                              Set<String> whitelist,
                                              Set<String> blacklist) {
        McpConfigConverter converter = McpConfigConverter.builder()
                .serverConfigs(serverConfigs)
                .sandboxType(sandboxType)
                .sandboxService(sandboxService)
                .whitelist(whitelist)
                .blacklist(blacklist)
                .build();

        return buildMcpAgentTools(converter);
    }

    public static List<ToolCallback> getMcpTools(Map<String, Object> serverConfigs,
                                              String sandboxType,
                                              SandboxService sandboxService,
                                              Set<String> whitelist,
                                              Set<String> blacklist) {
        McpConfigConverter converter = McpConfigConverter.builder()
                .serverConfigs(serverConfigs)
                .sandboxType(sandboxType)
                .sandboxService(sandboxService)
                .whitelist(whitelist)
                .blacklist(blacklist)
                .build();

        return buildMcpAgentTools(converter);
    }

    public static List<ToolCallback> getMcpTools(String serverConfigs,
                                              SandboxService sandboxService) {
        return getMcpTools(serverConfigs, null, sandboxService, null, null);
    }

    public static List<ToolCallback> getMcpTools(Map<String, Object> serverConfigs,
                                              SandboxService sandboxService) {
        return getMcpTools(serverConfigs, null, sandboxService, null, null);
    }

    public static List<MCPTool> createMcpToolInstances(String serverConfigs,
                                                       String sandboxType,
                                                       SandboxService sandboxService) {
        McpConfigConverter converter = McpConfigConverter.builder()
                .serverConfigs(serverConfigs)
                .sandboxType(sandboxType)
                .sandboxService(sandboxService)
                .build();

        return converter.toBuiltinTools();
    }

    public static List<MCPTool> createMcpToolInstances(Map<String, Object> serverConfigs,
                                                       String sandboxType,
                                                       SandboxService sandboxService) {
        McpConfigConverter converter = McpConfigConverter.builder()
                .serverConfigs(serverConfigs)
                .sandboxType(sandboxType)
                .sandboxService(sandboxService)
                .build();

        return converter.toBuiltinTools();
    }

    private static List<ToolCallback> buildMcpAgentTools(McpConfigConverter converter) {
        try {
            logger.info("Creating MCP tools from server configuration");

            List<MCPTool> mcpTools = converter.toBuiltinTools();
            List<ToolCallback> agentTools = new ArrayList<>(mcpTools.size());
            for (MCPTool mcpTool : mcpTools) {
                agentTools.add(new SaaMCPTool(mcpTool).buildTool());
            }

            logger.info("Created {} MCP tools", agentTools.size());
            return agentTools;
        } catch (Exception e) {
            logger.error("Failed to create MCP tools: {}", e.getMessage());
            throw new RuntimeException("Failed to create MCP tools", e);
        }
    }
}
