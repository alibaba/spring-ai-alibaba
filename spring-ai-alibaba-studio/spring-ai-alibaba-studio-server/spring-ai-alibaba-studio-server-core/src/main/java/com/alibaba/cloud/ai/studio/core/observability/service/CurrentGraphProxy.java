package com.alibaba.cloud.ai.studio.core.observability.service;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.studio.core.observability.workflow.SAAGraphFlow;
import reactor.core.publisher.Flux;

import java.util.Map;

public interface CurrentGraphProxy {
    Boolean run();

    Boolean switchTo(String graphId);

    SAAGraphFlow getCurrentGraph();

    /**
     * 流式调用写作助手 - 快照模式（获取每个节点完成后的状态快照）
     * 示例请求：GET /write/stream_snapshots?text=今天我去了西湖，天气特别好，感觉特别开心
     */
    Flux<Map<String, Object>> writeStreamSnapshots(String inputText);

    /**
     * 流式调用写作助手 - 基础流式输出（获取每个节点的输出）
     * 示例请求：GET /write/stream?text=今天我去了西湖，天气特别好，感觉特别开心
     */
    Flux<NodeOutput> writeStream(String inputText);

}
