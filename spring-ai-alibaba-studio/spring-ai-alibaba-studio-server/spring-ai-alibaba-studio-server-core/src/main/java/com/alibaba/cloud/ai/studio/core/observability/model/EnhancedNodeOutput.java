package com.alibaba.cloud.ai.studio.core.observability.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 增强的节点输出模型，包含节点元信息和业务数据
 * 
 * 提供完整的节点执行信息，包括：
 * - 节点基本信息（ID、名称、类型）
 * - 执行状态和时间信息
 * - 业务数据输出
 * - 执行上下文信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnhancedNodeOutput {
    
    /**
     * 节点ID
     */
    @JsonProperty("node_id")
    private String nodeId;

    /**
     * 执行状态
     * EXECUTING - 执行中
     * SUCCESS - 执行成功
     * FAILED - 执行失败
     * SKIPPED - 跳过执行
     */
    @JsonProperty("execution_status")
    private String executionStatus;
    
    /**
     * 开始执行时间
     */
    @JsonProperty("start_time")
    private LocalDateTime startTime;
    
    /**
     * 完成时间
     */
    @JsonProperty("end_time")
    private LocalDateTime endTime;
    
    /**
     * 执行耗时（毫秒）
     */
    @JsonProperty("duration_ms")
    private Long durationMs;
    
    /**
     * 节点业务数据输出
     */
    @JsonProperty("data")
    private Map<String, Object> data;
    
    /**
     * 错误信息（如果执行失败）
     */
    @JsonProperty("error_message")
    private String errorMessage;
    
    /**
     * 输入数据（可选，用于调试）
     */
    @JsonProperty("input_data")
    private Map<String, Object> inputData;
    
    /**
     * 执行序号（在整个流程中的执行顺序）
     */
    @JsonProperty("execution_order")
    private Integer executionOrder;
    
    /**
     * 是否为最终节点
     */
    @JsonProperty("is_final")
    private Boolean isFinal;
    
    /**
     * 父节点ID列表（依赖的上游节点）
     */
    @JsonProperty("parent_nodes")
    private java.util.List<String> parentNodes;
    
    /**
     * 创建一个表示节点开始执行的输出
     */
    public static EnhancedNodeOutput createStarting(String nodeId, String nodeName, String nodeType) {
        return EnhancedNodeOutput.builder()
                .nodeId(nodeId)
                .executionStatus("EXECUTING")
                .startTime(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建一个表示节点执行完成的输出
     */
    public static EnhancedNodeOutput createCompleted(String nodeId, String nodeName, String nodeType, 
                                                   Map<String, Object> data, LocalDateTime startTime) {
        LocalDateTime endTime = LocalDateTime.now();
        long duration = java.time.Duration.between(startTime, endTime).toMillis();
        
        return EnhancedNodeOutput.builder()
                .nodeId(nodeId)
                .executionStatus("SUCCESS")
                .startTime(startTime)
                .endTime(endTime)
                .durationMs(duration)
                .data(data)
                .build();
    }
    
    /**
     * 创建一个表示节点执行失败的输出
     */
    public static EnhancedNodeOutput createFailed(String nodeId, String nodeName, String nodeType, 
                                                String errorMessage, LocalDateTime startTime) {
        LocalDateTime endTime = LocalDateTime.now();
        long duration = startTime != null ? java.time.Duration.between(startTime, endTime).toMillis() : 0;
        
        return EnhancedNodeOutput.builder()
                .nodeId(nodeId)
                .executionStatus("FAILED")
                .startTime(startTime)
                .endTime(endTime)
                .durationMs(duration)
                .errorMessage(errorMessage)
                .build();
    }
}
