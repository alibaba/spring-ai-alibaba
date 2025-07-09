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
package com.alibaba.cloud.ai.example.manus.dynamic.model.model.enums;

/**
 * @author lizhenning
 * @date 2025/7/8
 */
public enum ModelType {
    /**
     * 通用模型：具备多种能力，可处理多种任务（如文本生成、推理、代码等）
     */
    GENERAL,
    /**
     * 推理模型：用于逻辑推理、决策等场景
     */
    REASONING,

    /**
     * 规划模型：用于任务分解、计划制定等场景
     */
    PLANNER,

    /**
     * 视觉模型：用于图像识别、OCR、目标检测等视觉相关任务
     */
    VISION,

    /**
     * 代码模型：用于代码生成、理解、修复等编程任务
     */
    CODE,

    /**
     * 文本生成模型：用于自然语言文本生成（如对话、文章生成）
     */
    TEXT_GENERATION,

    /**
     * 嵌入模型：用于文本向量化、语义编码等
     */
    EMBEDDING,

    /**
     * 分类模型：用于文本分类、情感分析等
     */
    CLASSIFICATION,

    /**
     * 摘要模型：用于长文本摘要生成
     */
    SUMMARIZATION,

    /**
     * 多模态模型：结合文本、图像等多种模态进行处理
     */
    MULTIMODAL,

    /**
     * 语音模型：用于语音识别、合成等任务
     */
    SPEECH,

    /**
     * 翻译模型：用于跨语言翻译
     */
    TRANSLATION;
}
