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

package com.alibaba.cloud.ai.example;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.util.StreamingChatGeneratorUtil;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * StreamingChatGeneratorUtil 通用工具类使用示例
 * 
 * @author zhangshenghang
 */
public class StreamingProcessorExample {

    /**
     * 示例1：简单的流式处理 - 使用快速方法
     */
    public static class SimpleStreamingNode implements NodeAction {
        
        @Override
        public Map<String, Object> apply(OverAllState state) throws Exception {
            // 模拟一个数据流
            Flux<ChatResponse> dataStream = getDataStream();
            
            // 使用快速方法创建带状态消息的流式生成器
            var generator = StreamingChatGeneratorUtil.createStreamingGeneratorWithMessages(
                this.getClass(),
                state,
                "开始处理数据...",
                "数据处理完成！",
                result -> Map.of("output", result, "processed", true),
                dataStream
            );
            
            return Map.of("result", generator);
        }
        
        private Flux<ChatResponse> getDataStream() {
            // 模拟数据流
            return Flux.empty();
        }
    }

    /**
     * 示例2：高级流式处理 - 使用建造者模式
     */
    public static class AdvancedStreamingNode implements NodeAction {
        
        @Override
        public Map<String, Object> apply(OverAllState state) throws Exception {
            Flux<ChatResponse> dataStream = getDataStream();
            
            // 使用建造者模式进行高级配置
            var generator = StreamingChatGeneratorUtil.createStreamingProcessor()
                .nodeClass(this.getClass())
                .state(state)
                .startMessage("🚀 开始执行复杂任务...")
                .completionMessage("✅ 任务执行成功完成！")
                .contentExtractor(response -> {
                    // 自定义内容提取逻辑
                    String text = response.getResult().getOutput().getText();
                    return text != null ? text.toUpperCase() : "";
                })
                .resultMapper(collectedContent -> {
                    // 自定义结果映射逻辑
                    return Map.of(
                        "processedContent", collectedContent,
                        "wordCount", collectedContent.split("\\s+").length,
                        "timestamp", System.currentTimeMillis()
                    );
                })
                .trimResult(true)
                .build(dataStream);
            
            return Map.of("advanced_result", generator);
        }
        
        private Flux<ChatResponse> getDataStream() {
            // 模拟数据流
            return Flux.empty();
        }
    }

    /**
     * 示例3：SQL生成节点
     */
    public static class SqlGenerationNode implements NodeAction {
        
        @Override
        public Map<String, Object> apply(OverAllState state) throws Exception {
            String query = (String) state.value("user_query").orElse("");
            
            // 模拟SQL生成流
            Flux<ChatResponse> sqlStream = generateSqlStream(query);
            
            var generator = StreamingChatGeneratorUtil.createStreamingGeneratorWithMessages(
                this.getClass(),
                state,
                "🔍 开始分析查询需求...",
                "📝 SQL语句生成完成！",
                sqlResult -> Map.of(
                    "generated_sql", sqlResult,
                    "query", query,
                    "node_type", "sql_generation"
                ),
                sqlStream
            );
            
            return Map.of("sql_generator", generator);
        }
        
        private Flux<ChatResponse> generateSqlStream(String query) {
            // 模拟SQL生成流
            return Flux.empty();
        }
    }

    /**
     * 示例4：数据分析节点
     */
    public static class DataAnalysisNode implements NodeAction {
        
        @Override
        public Map<String, Object> apply(OverAllState state) throws Exception {
            // 使用建造者模式进行定制化配置
            var generator = StreamingChatGeneratorUtil.createStreamingProcessor()
                .nodeClass(this.getClass())
                .state(state)
                .startMessage("📊 开始数据分析...")
                .completionMessage("📈 分析报告生成完成！")
                .contentExtractor(response -> {
                    // 提取分析结果中的关键信息
                    String content = response.getResult().getOutput().getText();
                    if (content != null && content.contains("ANALYSIS:")) {
                        return content.substring(content.indexOf("ANALYSIS:") + 9);
                    }
                    return content;
                })
                .resultMapper(analysis -> {
                    // 构建分析结果
                    return Map.of(
                        "analysis_result", analysis,
                        "confidence_score", calculateConfidence(analysis),
                        "recommendations", extractRecommendations(analysis)
                    );
                })
                .build(getAnalysisStream())
            ;
            
            return Map.of("analysis", generator);
        }
        
        private Flux<ChatResponse> getAnalysisStream() {
            // 模拟分析流
            return Flux.empty();
        }
        
        private double calculateConfidence(String analysis) {
            // 模拟置信度计算
            return 0.85;
        }
        
        private String extractRecommendations(String analysis) {
            // 模拟建议提取
            return "基于分析结果的建议";
        }
    }
}
