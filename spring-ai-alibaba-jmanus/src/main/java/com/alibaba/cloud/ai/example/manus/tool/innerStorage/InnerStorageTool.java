// /*
// * Copyright 2025 the original author or authors.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// * https://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
// package com.alibaba.cloud.ai.example.manus.tool.innerStorage;

// import java.io.IOException;
// import java.nio.file.*;
// import java.util.List;

// import com.alibaba.cloud.ai.example.manus.tool.AbstractBaseTool;
// import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
// import com.alibaba.cloud.ai.example.manus.tool.filesystem.UnifiedDirectoryManager;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.ai.chat.model.ToolContext;
// import org.springframework.ai.openai.api.OpenAiApi;

// /**
// *
// * 内部存储工具，用于MapReduce流程中的中间数据管理 自动管理基于planID和Agent的目录结构，提供简化的文件操作
// * 支持智能内容管理：当返回内容过长时自动存储并返回摘要
// *
// */
// public class InnerStorageTool extends
// AbstractBaseTool<InnerStorageTool.InnerStorageInput> {

// private static final Logger log = LoggerFactory.getLogger(InnerStorageTool.class);

// /**
// * 内部输入类，用于定义内部存储工具的输入参数
// */
// public static class InnerStorageInput {

// private String action;

// @com.fasterxml.jackson.annotation.JsonProperty("file_name")
// private String fileName;

// private String content;

// @com.fasterxml.jackson.annotation.JsonProperty("source_text")
// private String sourceText;

// @com.fasterxml.jackson.annotation.JsonProperty("target_text")
// private String targetText;

// @com.fasterxml.jackson.annotation.JsonProperty("start_line")
// private Integer startLine;

// @com.fasterxml.jackson.annotation.JsonProperty("end_line")
// private Integer endLine;

// @com.fasterxml.jackson.annotation.JsonProperty("target_file_name")
// private String targetFileName;

// public InnerStorageInput() {
// }

// public String getAction() {
// return action;
// }

// public void setAction(String action) {
// this.action = action;
// }

// public String getFileName() {
// return fileName;
// }

// public void setFileName(String fileName) {
// this.fileName = fileName;
// }

// public String getContent() {
// return content;
// }

// public void setContent(String content) {
// this.content = content;
// }

// public String getSourceText() {
// return sourceText;
// }

// public void setSourceText(String sourceText) {
// this.sourceText = sourceText;
// }

// public String getTargetText() {
// return targetText;
// }

// public void setTargetText(String targetText) {
// this.targetText = targetText;
// }

// public Integer getStartLine() {
// return startLine;
// }

// public void setStartLine(Integer startLine) {
// this.startLine = startLine;
// }

// public Integer getEndLine() {
// return endLine;
// }

// public void setEndLine(Integer endLine) {
// this.endLine = endLine;
// }

// public String getTargetFileName() {
// return targetFileName;
// }

// public void setTargetFileName(String targetFileName) {
// this.targetFileName = targetFileName;
// }

// }

// private final UnifiedDirectoryManager directoryManager;

// // Plan ID fields for directory management
// private String rootPlanId;

// private String currentPlanId;

// // get_lines 操作的最大行数限制
// private static final int MAX_LINES_LIMIT = 500;

// public InnerStorageTool(UnifiedDirectoryManager directoryManager) {
// this.directoryManager = directoryManager;
// }

// private static final String TOOL_NAME = "inner_storage_tool";

// private static final String TOOL_DESCRIPTION = """
// 内部存储工具，Agent流程内的数据管理。
// 自动管理基于planID，提供简化的文件操作：
// - append: 向特定文件文件追加内容（自动创建文件和目录）
// - replace: 替换文件中的特定文本
// - get_lines: 获取文件的指定行号范围内容（单次最多%d行）
// - export: 将内部存储文件导出到工作目录，供外部使用

// 当返回内容过长时，工具会自动存储详细内容并返回摘要和内容ID，以降低上下文压力。

// """.formatted(MAX_LINES_LIMIT);

// private static final String PARAMETERS = """
// {
// "oneOf": [
// {
// "type": "object",
// "properties": {
// "action": {
// "type": "string",
// "const": "append"
// },
// "file_name": {
// "type": "string",
// "description": "文件名（带扩展名）"
// },
// "content": {
// "type": "string",
// "description": "要追加的内容"
// }
// },
// "required": ["action", "file_name", "content"],
// "additionalProperties": false
// },
// {
// "type": "object",
// "properties": {
// "action": {
// "type": "string",
// "const": "replace"
// },
// "file_name": {
// "type": "string",
// "description": "文件名（带扩展名）"
// },
// "source_text": {
// "type": "string",
// "description": "要被替换的文本"
// },
// "target_text": {
// "type": "string",
// "description": "替换后的文本"
// }
// },
// "required": ["action", "file_name", "source_text", "target_text"],
// "additionalProperties": false
// },
// {
// "type": "object",
// "properties": {
// "action": {
// "type": "string",
// "const": "get_lines"
// },
// "file_name": {
// "type": "string",
// "description": "文件名（带扩展名）"
// },
// "start_line": {
// "type": "integer",
// "description": "起始行号，默认为1"
// },
// "end_line": {
// "type": "integer",
// "description": "结束行号，默认为文件末尾"
// }
// },
// "required": ["action", "file_name"],
// "additionalProperties": false
// },
// {
// "type": "object",
// "properties": {
// "action": {
// "type": "string",
// "const": "export"
// },
// "file_name": {
// "type": "string",
// "description": "要导出的内部存储文件名（带扩展名）"
// },
// "target_file_name": {
// "type": "string",
// "description": "导出后的目标文件名（可选，默认使用原文件名）"
// }
// },
// "required": ["action", "file_name"],
// "additionalProperties": false
// }
// ]
// }
// """;

// @Override
// public String getName() {
// return TOOL_NAME;
// }

// @Override
// public String getDescription() {
// return TOOL_DESCRIPTION;
// }

// @Override
// public String getParameters() {
// return PARAMETERS;
// }

// @Override
// public Class<InnerStorageInput> getInputType() {
// return InnerStorageInput.class;
// }

// @Override
// public String getServiceGroup() {
// return "default-service-group";
// }

// public static OpenAiApi.FunctionTool getToolDefinition() {
// OpenAiApi.FunctionTool.Function function = new
// OpenAiApi.FunctionTool.Function(TOOL_DESCRIPTION, TOOL_NAME,
// PARAMETERS);
// return new OpenAiApi.FunctionTool(function);
// }

// /**
// * 执行内部存储操作，接受强类型输入对象
// */
// @Override
// public ToolExecuteResult run(InnerStorageInput input) {
// log.info("InnerStorageTool input: action={}, fileName={}", input.getAction(),
// input.getFileName());
// try {
// String action = input.getAction();
// if (action == null) {
// return new ToolExecuteResult("错误：action参数是必需的");
// }

// return switch (action) {
// case "append" -> {
// String fileName = input.getFileName();
// String content = input.getContent();
// yield appendToFile(fileName, content);
// }
// case "replace" -> {
// String fileName = input.getFileName();
// String sourceText = input.getSourceText();
// String targetText = input.getTargetText();
// yield replaceInFile(fileName, sourceText, targetText);
// }
// case "get_lines" -> {
// String fileName = input.getFileName();
// Integer startLine = input.getStartLine();
// Integer endLine = input.getEndLine();
// yield getFileLines(fileName, startLine, endLine);
// }
// default -> new ToolExecuteResult("未知操作: " + action + "。支持的操作: append, replace,
// get_lines, export");
// };

// }
// catch (Exception e) {
// log.error("InnerStorageTool执行失败", e);
// return new ToolExecuteResult("工具执行失败: " + e.getMessage());
// }
// }

// /**
// * 追加内容到文件
// */
// private ToolExecuteResult appendToFile(String fileName, String content) {
// try {
// if (fileName == null || fileName.trim().isEmpty()) {
// return new ToolExecuteResult("错误：file_name参数是必需的");
// }
// if (content == null) {
// content = "";
// }
// // 使用统一目录管理器获取子任务目录
// Path subTaskDir = directoryManager.getSubTaskDirectory(rootPlanId, currentPlanId);
// directoryManager.ensureDirectoryExists(subTaskDir);

// // 获取文件路径并追加内容 - 直接在子任务目录下创建文件
// Path filePath = subTaskDir.resolve(fileName);

// // 如果文件不存在，创建新文件
// if (!Files.exists(filePath)) {
// Files.writeString(filePath, content);
// return new ToolExecuteResult(String.format("文件创建成功并添加内容: %s", fileName));
// }
// else {
// // 追加内容（添加换行符）
// Files.writeString(filePath, "\n" + content, StandardOpenOption.APPEND);
// return new ToolExecuteResult(String.format("内容追加成功: %s", fileName));
// }

// }
// catch (IOException e) {
// log.error("追加文件失败", e);
// return new ToolExecuteResult("追加文件失败: " + e.getMessage());
// }
// }

// /**
// * 替换文件中的文本
// */
// private ToolExecuteResult replaceInFile(String fileName, String sourceText, String
// targetText) {
// try {
// if (fileName == null || fileName.trim().isEmpty()) {
// return new ToolExecuteResult("错误：file_name参数是必需的");
// }
// if (sourceText == null || targetText == null) {
// return new ToolExecuteResult("错误：source_text和target_text参数都是必需的");
// }

// Path subTaskDir = directoryManager.getSubTaskDirectory(rootPlanId, currentPlanId);
// Path filePath = subTaskDir.resolve(fileName);

// if (!Files.exists(filePath)) {
// return new ToolExecuteResult("错误：文件不存在: " + fileName);
// }

// String content = Files.readString(filePath);
// String newContent = content.replace(sourceText, targetText);
// Files.writeString(filePath, newContent);

// return new ToolExecuteResult(String.format("文本替换成功: %s", fileName));

// }
// catch (IOException e) {
// log.error("替换文件文本失败", e);
// return new ToolExecuteResult("替换文件文本失败: " + e.getMessage());
// }
// }

// /**
// * 获取文件的指定行号内容
// */
// private ToolExecuteResult getFileLines(String fileName, Integer startLine, Integer
// endLine) {
// try {
// if (fileName == null || fileName.trim().isEmpty()) {
// return new ToolExecuteResult("错误：file_name参数是必需的");
// }

// Path subTaskDir = directoryManager.getSubTaskDirectory(rootPlanId, currentPlanId);
// Path filePath = subTaskDir.resolve(fileName);

// if (!Files.exists(filePath)) {
// return new ToolExecuteResult("错误：文件不存在: " + fileName);
// }

// List<String> lines = Files.readAllLines(filePath);

// if (lines.isEmpty()) {
// return new ToolExecuteResult("文件为空");
// }

// // 设置默认值
// int start = (startLine != null && startLine > 0) ? startLine - 1 : 0;
// int end = (endLine != null && endLine > 0) ? Math.min(endLine, lines.size()) :
// lines.size();

// // 验证范围
// if (start >= lines.size()) {
// return new ToolExecuteResult("起始行号超出文件范围");
// }

// if (start >= end) {
// return new ToolExecuteResult("起始行号不能大于或等于结束行号");
// }

// // 检查行数限制
// int requestedLines = end - start;
// if (requestedLines > MAX_LINES_LIMIT) {
// return new ToolExecuteResult(
// String.format("请求的行数 %d 超过最大限制 %d 行。请减少行数范围或使用多次调用获取内容。", requestedLines,
// MAX_LINES_LIMIT));
// }

// StringBuilder result = new StringBuilder();
// result.append(String.format("文件: %s (第%d-%d行，共%d行)\n", fileName, start + 1, end,
// lines.size()));
// result.append("=".repeat(50)).append("\n");

// for (int i = start; i < end; i++) {
// result.append(String.format("%4d: %s\n", i + 1, lines.get(i)));
// }

// return new ToolExecuteResult(result.toString());

// }
// catch (IOException e) {
// log.error("读取文件行失败", e);
// return new ToolExecuteResult("读取文件行失败: " + e.getMessage());
// }
// }

// @Override
// public String getCurrentToolStateString() {
// try {
// StringBuilder sb = new StringBuilder();
// // sb.append("InnerStorage 当前状态:\n");
// // sb.append("- Plan ID: ").append(planId != null ? planId :
// // "未设置").append("\n");
// // sb.append("- 存储根目录:
// // ").append(innerStorageService.getInnerStorageRoot()).append("\n");

// // // 获取当前目录下的所有文件信息
// // List<InnerStorageService.FileInfo> files =
// // innerStorageService.getDirectoryFiles(planId);

// // if (files.isEmpty()) {
// // sb.append("- 内部文件: 无\n");
// // }
// // else {
// // sb.append("- 内部文件 (").append(files.size()).append("个):\n");
// // for (InnerStorageService.FileInfo file : files) {
// // sb.append(" ").append(file.toString()).append("\n");
// // }
// // }

// return sb.toString();
// }
// catch (Exception e) {
// log.error("获取工具状态失败", e);
// return "InnerStorage 状态获取失败: " + e.getMessage();
// }
// }

// @Override
// public void cleanup(String planId) {
// // planId here is rootPlanId, currentPlanId为subTaskId
// // if (planId != null && currentPlanId != null) {
// // try {
// // directoryManager.cleanupSubTaskDirectory(planId, currentPlanId);
// // log.info("Cleaned up subtask directory: rootPlanId={}, subTaskId={}", planId,
// // currentPlanId);
// // } catch (IOException e) {
// // log.error("Failed to clean up subtask directory", e);
// // }
// // }
// }

// @Override
// public ToolExecuteResult apply(InnerStorageInput input, ToolContext toolContext) {
// return run(input);
// }

// /**
// * 将内部存储文件导出到工作目录
// */
// private ToolExecuteResult exportToWorkingDirectory(String fileName, String
// targetFileName) {
// try {
// if (fileName == null || fileName.trim().isEmpty()) {
// return new ToolExecuteResult("错误：file_name参数是必需的");
// }
// // 源目录为subTaskDir，目标目录为rootPlanDir
// Path subTaskDir = directoryManager.getSubTaskDirectory(rootPlanId, currentPlanId);
// Path rootPlanDir = directoryManager.getRootPlanDirectory(rootPlanId);
// directoryManager.ensureDirectoryExists(rootPlanDir);

// Path sourceFile = subTaskDir.resolve(fileName);
// if (!Files.exists(sourceFile)) {
// return new ToolExecuteResult("未找到文件: " + fileName + " 于子任务目录: " + subTaskDir);
// }
// String finalTargetName = (targetFileName != null && !targetFileName.trim().isEmpty()) ?
// targetFileName
// : fileName;
// Path targetFile = rootPlanDir.resolve(finalTargetName);
// Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
// String fileContent = Files.readString(sourceFile);
// log.info("成功导出文件：{} -> {}", sourceFile, targetFile);
// StringBuilder result = new StringBuilder();
// result.append("结果内容:\n");
// result.append("-".repeat(50)).append("\n");
// result.append(fileContent);
// if (!fileContent.endsWith("\n")) {
// result.append("\n");
// }
// result.append("-".repeat(50)).append("\n");
// result.append("- 也可以访问路径来获得详细文件内容 : ").append(targetFile.toString()).append("\n");
// return new ToolExecuteResult(result.toString());
// }
// catch (IOException e) {
// log.error("导出文件失败", e);
// return new ToolExecuteResult("导出文件失败: " + e.getMessage());
// }
// catch (Exception e) {
// log.error("导出操作异常", e);
// return new ToolExecuteResult("导出操作失败: " + e.getMessage());
// }
// }

// @Override
// public void setCurrentPlanId(String planId) {
// this.currentPlanId = planId;
// }

// public void setRootPlanId(String rootPlanId) {
// this.rootPlanId = rootPlanId;
// }

// }
