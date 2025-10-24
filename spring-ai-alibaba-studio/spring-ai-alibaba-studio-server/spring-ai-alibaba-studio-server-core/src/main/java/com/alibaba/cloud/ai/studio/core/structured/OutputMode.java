package com.alibaba.cloud.ai.studio.core.structured;

public enum OutputMode {
    NATIVE,    // 模型原生responseFormat
    TOOL_CALL, // 工具调用方式
    PROMPT     // Prompt工程方式
}
