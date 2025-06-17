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
package com.alibaba.cloud.ai.example.manus.tool.split;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.alibaba.cloud.ai.example.manus.tool.ToolCallBiFunctionDef;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.function.FunctionToolCallback;

/**
 * 数据分割工具，用于MapReduce流程中的数据准备阶段
 * 负责验证文件存在性、识别表格头部信息并进行数据分割处理
 */
public class SplitTool implements ToolCallBiFunctionDef {

    private static final Logger log = LoggerFactory.getLogger(SplitTool.class);

    private static final String TOOL_NAME = "split_tool";
    private static final String TOOL_DESCRIPTION = "数据分割工具，用于MapReduce流程中的数据准备阶段。自动完成验证文件存在性、识别表格头部信息并进行数据分割处理。支持CSV、TSV、TXT等文本格式的数据文件。";

    private static final String PARAMETERS = """
            {
                "type": "object",
                "properties": {
                    "file_path": {
                        "type": "string",
                        "description": "要处理的文件或文件夹路径"
                    }
                },
                "required": ["file_path"]
            }
            """;

    private String planId;
    private String lastOperationResult = "";
    private String lastProcessedFile = "";
    private List<String> splitResults = new ArrayList<>();

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 常见的分隔符模式
    private static final List<String> COMMON_DELIMITERS = Arrays.asList(",", "\t", ";", "|");
    private static final Pattern CSV_HEADER_PATTERN = Pattern.compile("^[a-zA-Z_][\\w\\s,;|\\t-]*$");

    public SplitTool() {
    }

    public SplitTool(String planId) {
        this.planId = planId;
    }

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public String getDescription() {
        return TOOL_DESCRIPTION;
    }

    @Override
    public String getParameters() {
        return PARAMETERS;
    }

    @Override
    public Class<?> getInputType() {
        return String.class;
    }

    @Override
    public boolean isReturnDirect() {
        return false;
    }

    @Override
    public void setPlanId(String planId) {
        this.planId = planId;
    }

    @Override
    public String getServiceGroup() {
        return "data-processing";
    }

    public static OpenAiApi.FunctionTool getToolDefinition() {
        OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(TOOL_DESCRIPTION, TOOL_NAME, PARAMETERS);
        return new OpenAiApi.FunctionTool(function);
    }

    public static FunctionToolCallback<String, ToolExecuteResult> getFunctionToolCallback() {
        return FunctionToolCallback.builder(TOOL_NAME, new SplitTool())
                .description(TOOL_DESCRIPTION)
                .inputSchema(PARAMETERS)
                .inputType(String.class)
                .build();
    }

    public static FunctionToolCallback<String, ToolExecuteResult> getFunctionToolCallback(String planId) {
        return FunctionToolCallback.builder(TOOL_NAME, new SplitTool(planId))
                .description(TOOL_DESCRIPTION)
                .inputSchema(PARAMETERS)
                .inputType(String.class)
                .build();
    }

    public ToolExecuteResult run(String toolInput) {
        log.info("SplitTool toolInput: {}", toolInput);
        try {
            Map<String, Object> toolInputMap = objectMapper.readValue(toolInput, new TypeReference<Map<String, Object>>() {});
            
            String filePath = (String) toolInputMap.get("file_path");

            if (filePath == null) {
                return new ToolExecuteResult("错误：file_path参数是必需的");
            }

            return processFileOrDirectory(filePath);

        } catch (Exception e) {
            log.error("SplitTool执行失败", e);
            return new ToolExecuteResult("工具执行失败: " + e.getMessage());
        }
    }

    /**
     * 处理文件或目录的完整流程：验证存在性 -> 检测头部 -> 分割数据
     */
    private ToolExecuteResult processFileOrDirectory(String filePath) {
        StringBuilder finalResult = new StringBuilder();
        
        try {
            // 步骤1: 验证文件或文件夹存在性
            finalResult.append("=== 步骤1: 验证文件存在性 ===\n");
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return new ToolExecuteResult("错误：文件或目录不存在: " + filePath);
            }
            
            boolean isFile = Files.isRegularFile(path);
            boolean isDirectory = Files.isDirectory(path);
            
            finalResult.append("- 路径: ").append(filePath).append("\n");
            finalResult.append("- 类型: ").append(isFile ? "文件" : (isDirectory ? "文件夹" : "其他")).append("\n");
            
            if (isFile) {
                long size = Files.size(path);
                finalResult.append("- 文件大小: ").append(formatFileSize(size)).append("\n");
            } else if (isDirectory) {
                long fileCount = Files.list(path).count();
                finalResult.append("- 包含文件数: ").append(fileCount).append("\n");
            }
            finalResult.append("\n");

            // 步骤2: 检测表格头部信息
            finalResult.append("=== 步骤2: 检测表格头部信息 ===\n");
            String detectedHeaders = null;
            String delimiter = null;
            
            if (isFile) {
                if (isTextFile(filePath)) {
                    try (BufferedReader reader = Files.newBufferedReader(path)) {
                        char[] buffer = new char[2000];
                        int charsRead = reader.read(buffer);
                        String content = new String(buffer, 0, charsRead);
                        
                        String[] lines = content.split("\n");
                        if (lines.length > 0) {
                            String firstLine = lines[0].trim();
                            delimiter = detectDelimiter(firstLine);
                            finalResult.append("- 检测到的分隔符: ").append(getDelimiterName(delimiter)).append("\n");
                            
                            if (CSV_HEADER_PATTERN.matcher(firstLine).matches()) {
                                detectedHeaders = firstLine;
                                String[] headers = firstLine.split(Pattern.quote(delimiter));
                                finalResult.append("- 表格头部: ").append(Arrays.toString(headers)).append("\n");
                                finalResult.append("- 列数: ").append(headers.length).append("\n");
                            } else {
                                finalResult.append("- 表格头部: 未检测到标准表格头部\n");
                            }
                            
                            finalResult.append("- 前").append(Math.min(3, lines.length)).append("行预览:\n");
                            for (int i = 0; i < Math.min(3, lines.length); i++) {
                                finalResult.append("  ").append(i + 1).append(": ").append(lines[i]).append("\n");
                            }
                        }
                    }
                } else {
                    finalResult.append("- 不是支持的文本文件格式\n");
                }
            } else if (isDirectory) {
                List<Path> textFiles = Files.list(path)
                        .filter(Files::isRegularFile)
                        .filter(p -> isTextFile(p.toString()))
                        .collect(Collectors.toList());
                
                finalResult.append("- 找到可处理的文本文件数: ").append(textFiles.size()).append("\n");
                
                if (!textFiles.isEmpty()) {
                    // 使用第一个文件来检测头部信息
                    Path firstFile = textFiles.get(0);
                    try (BufferedReader reader = Files.newBufferedReader(firstFile)) {
                        char[] buffer = new char[2000];
                        int charsRead = reader.read(buffer);
                        String content = new String(buffer, 0, charsRead);
                        
                        String[] lines = content.split("\n");
                        if (lines.length > 0) {
                            String firstLine = lines[0].trim();
                            delimiter = detectDelimiter(firstLine);
                            finalResult.append("- 样本文件分隔符: ").append(getDelimiterName(delimiter)).append("\n");
                            
                            if (CSV_HEADER_PATTERN.matcher(firstLine).matches()) {
                                detectedHeaders = firstLine;
                                String[] headers = firstLine.split(Pattern.quote(delimiter));
                                finalResult.append("- 样本文件头部: ").append(Arrays.toString(headers)).append("\n");
                            }
                        }
                    }
                }
            }
            finalResult.append("\n");

            // 步骤3: 执行数据分割
            finalResult.append("=== 步骤3: 执行数据分割 ===\n");
            
            // 确定输出目录
            Path outputPath = path.getParent() != null ? path.getParent().resolve("split_output") : Paths.get("split_output");
            Files.createDirectories(outputPath);
            finalResult.append("- 输出目录: ").append(outputPath.toString()).append("\n");

            List<String> allSplitFiles = new ArrayList<>();
            int totalProcessedLines = 0;
            
            if (isFile && isTextFile(filePath)) {
                // 处理单个文件
                SplitResult result = splitSingleFile(path, detectedHeaders, 1000, outputPath, delimiter);
                allSplitFiles.addAll(result.splitFiles);
                totalProcessedLines += result.totalLines;
                
            } else if (isDirectory) {
                // 处理文件夹中的所有文本文件
                List<Path> textFiles = Files.list(path)
                        .filter(Files::isRegularFile)
                        .filter(p -> isTextFile(p.toString()))
                        .collect(Collectors.toList());
                
                for (Path file : textFiles) {
                    SplitResult result = splitSingleFile(file, detectedHeaders, 1000, outputPath, delimiter);
                    allSplitFiles.addAll(result.splitFiles);
                    totalProcessedLines += result.totalLines;
                }
            }
            
            finalResult.append("- 总处理行数: ").append(totalProcessedLines).append("\n");
            finalResult.append("- 生成分割文件数: ").append(allSplitFiles.size()).append("\n");
            finalResult.append("- 分割文件列表:\n");
            for (String file : allSplitFiles) {
                finalResult.append("  ").append(file).append("\n");
            }
            
            splitResults = allSplitFiles;
            lastOperationResult = finalResult.toString();
            return new ToolExecuteResult(lastOperationResult);
            
        } catch (Exception e) {
            String error = "处理失败: " + e.getMessage();
            log.error(error, e);
            return new ToolExecuteResult(error);
        }
    }

    /**
     * 分割结果类
     */
    private static class SplitResult {
        List<String> splitFiles;
        int totalLines;
        
        SplitResult(List<String> splitFiles, int totalLines) {
            this.splitFiles = splitFiles;
            this.totalLines = totalLines;
        }
    }

    /**
     * 分割单个文件
     */
    private SplitResult splitSingleFile(Path filePath, String headers, int splitSize, Path outputPath, String delimiter) throws IOException {
        List<String> splitFiles = new ArrayList<>();
        String fileName = filePath.getFileName().toString();
        String baseName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        String extension = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.')) : ".txt";

        int totalLineCount = 0;
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            int lineCount = 0;
            int fileIndex = 1;
            BufferedWriter writer = null;
            String currentOutputFile = null;

            while ((line = reader.readLine()) != null) {
                // 创建新的分割文件
                if (lineCount % splitSize == 0) {
                    if (writer != null) {
                        writer.close();
                    }
                    
                    currentOutputFile = outputPath.resolve(baseName + "_part" + fileIndex + extension).toString();
                    splitFiles.add(currentOutputFile);
                    writer = Files.newBufferedWriter(Paths.get(currentOutputFile));
                    
                    // 如果有头部信息，每个文件都写入头部
                    if (headers != null && !headers.trim().isEmpty() && totalLineCount > 0) {
                        writer.write(headers);
                        writer.newLine();
                    }
                    
                    fileIndex++;
                }

                writer.write(line);
                writer.newLine();
                lineCount++;
                totalLineCount++;
            }

            if (writer != null) {
                writer.close();
            }
        }
        
        return new SplitResult(splitFiles, totalLineCount);
    }



    /**
     * 检测分隔符
     */
    private String detectDelimiter(String line) {
        Map<String, Integer> delimiterCounts = new HashMap<>();
        
        for (String delimiter : COMMON_DELIMITERS) {
            int count = line.split(Pattern.quote(delimiter), -1).length - 1;
            delimiterCounts.put(delimiter, count);
        }
        
        return delimiterCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(",");
    }

    /**
     * 获取分隔符的友好名称
     */
    private String getDelimiterName(String delimiter) {
        return switch (delimiter) {
            case "," -> "逗号(,)";
            case "\t" -> "制表符(\\t)";
            case ";" -> "分号(;)";
            case "|" -> "竖线(|)";
            default -> "其他(" + delimiter + ")";
        };
    }

    /**
     * 判断是否为文本文件
     */
    private boolean isTextFile(String fileName) {
        String lowercaseFileName = fileName.toLowerCase();
        return lowercaseFileName.endsWith(".csv") ||
               lowercaseFileName.endsWith(".tsv") ||
               lowercaseFileName.endsWith(".txt") ||
               lowercaseFileName.endsWith(".dat") ||
               lowercaseFileName.endsWith(".log");
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024.0));
        return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
    }

    @Override
    public String getCurrentToolStateString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SplitTool 当前状态:\n");
        sb.append("- Plan ID: ").append(planId != null ? planId : "未设置").append("\n");
        sb.append("- 最后处理文件: ").append(lastProcessedFile.isEmpty() ? "无" : lastProcessedFile).append("\n");
        sb.append("- 最后操作结果: ").append(lastOperationResult.isEmpty() ? "无" : "已完成").append("\n");
        sb.append("- 分割文件数: ").append(splitResults.size()).append("\n");
        
        return sb.toString();
    }

    @Override
    public void cleanup(String planId) {
        // 清理资源
        splitResults.clear();
        lastOperationResult = "";
        lastProcessedFile = "";
        log.info("SplitTool cleanup completed for planId: {}", planId);
    }

    @Override
    public ToolExecuteResult apply(String s, ToolContext toolContext) {
        return run(s);
    }
}
