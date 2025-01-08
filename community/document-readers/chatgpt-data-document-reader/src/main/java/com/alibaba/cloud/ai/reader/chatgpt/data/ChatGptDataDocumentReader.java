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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 用于加载ChatGPT导出的对话数据的文档阅读器
 * @author YunLong
 */
public class ChatGptDataDocumentReader implements DocumentReader {

    private final Resource logFile;
    private final int numLogs;

    /**
     * 初始化ChatGPT数据加载器
     * @param logFile 日志文件资源
     * @param numLogs 要加载的日志数量,如果为0则加载所有
     */    public ChatGptDataDocumentReader(Resource logFile, int numLogs) {
        this.logFile = logFile;
        this.numLogs = numLogs;
    }

    public ChatGptDataDocumentReader(Resource logFile) {
        this(logFile, 0);
    }

    /**
     * 将消息内容格式化为可读字符串
     * @param message 消息对象
     * @param title 对话标题
     * @return 格式化后的消息字符串
     */
    private String concatenateRows(JSONObject message, String title) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        // 获取发送者角色
        JSONObject author = message.getJSONObject("author");
        String sender = author != null ? author.getString("role") : "unknown";
        
        // 获取消息内容
        JSONObject content = message.getJSONObject("content");
        JSONArray parts = content.getJSONArray("parts");
        String text = parts.getString(0);
        
        // 获取并格式化时间
        long createTime = message.getLongValue("create_time");
        LocalDateTime dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochSecond(createTime), 
            ZoneId.systemDefault()
        );
        String formattedDate = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        return String.format("%s - %s on %s: %s\n\n", title, sender, formattedDate, text);
    }

    @Override
    public List<Document> get() {
        try {
            // 读取JSON文件内容
            String jsonContent = StreamUtils.copyToString(logFile.getInputStream(), StandardCharsets.UTF_8);
            JSONArray data = JSON.parseArray(jsonContent);
            List<Document> documents = new ArrayList<>();
            
            // 如果指定了日志数量限制,则只取指定数量的数据
            int limit = numLogs > 0 ? Math.min(numLogs, data.size()) : data.size();
            
            // 使用Stream API处理前limit条数据
            documents = IntStream.range(0, limit)
                .mapToObj(i -> {
                    // 获取会话数据
                    JSONObject conversation = data.getJSONObject(i);
                    String title = conversation.getString("title");
                    JSONObject messages = conversation.getJSONObject("mapping");
                    
                    // 使用Stream处理消息
                    String text = messages.keySet().stream()
                        .map(key -> {
                            JSONObject messageWrapper = messages.getJSONObject(key);
                            JSONObject message = messageWrapper.getJSONObject("message");
                            
                            // 跳过第一条system角色的消息
                            if ("0".equals(key) && 
                                "system".equals(message.getJSONObject("author").getString("role"))) {
                                return "";
                            }
                            return concatenateRows(message, title);
                        })
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.joining());
                    
                    // 创建文档元数据
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("source", logFile.getFilename());
                    
                    // 返回新的Document对象
                    return new Document(text, metadata);
                })
                .collect(Collectors.toList());
            
            return documents;
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to load ChatGPT data from file: " + logFile.getFilename(), e);
        }
    }
} 