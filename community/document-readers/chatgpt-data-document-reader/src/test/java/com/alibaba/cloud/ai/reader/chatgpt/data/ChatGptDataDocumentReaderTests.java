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
package com.alibaba.cloud.ai.reader.chatgpt.data;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * ChatGPT数据文档阅读器的测试类
 * @author YunLong
 */
class ChatGptDataDocumentReaderTests {

    @Test
    void shouldLoadAllDocumentsWhenNumLogsIsZero() {
        ChatGptDataDocumentReader reader = new ChatGptDataDocumentReader(
            new ClassPathResource("conversations.json")
        );
        List<Document> documents = reader.get();
        assertThat(documents).isNotEmpty();
    }

    @Test
    void shouldLoadLimitedDocumentsWhenNumLogsIsPositive() {
        int numLogs = 2;
        ChatGptDataDocumentReader reader = new ChatGptDataDocumentReader(
            new ClassPathResource("conversations.json"),
            numLogs
        );
        List<Document> documents = reader.get();
        assertThat(documents).hasSize(numLogs);
    }

    @Test
    void shouldContainCorrectMetadata() {
        ChatGptDataDocumentReader reader = new ChatGptDataDocumentReader(
            new ClassPathResource("conversations.json")
        );
        List<Document> documents = reader.get();
        assertThat(documents.get(0).getMetadata())
            .containsKey("source")
            .containsValue("conversations.json");
    }

    @Test
    void shouldThrowExceptionWhenFileNotFound() {
        ChatGptDataDocumentReader reader = new ChatGptDataDocumentReader(
            new ClassPathResource("non-existent.json")
        );
        assertThrows(RuntimeException.class, reader::get);
    }
} 