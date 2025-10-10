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
package com.alibaba.cloud.ai.graph.streaming;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ParallelGraphFlux用于管理并行节点的多个GraphFlux实例。
 * 
 * 核心特性：
 * - 管理多个并行节点产生的GraphFlux流
 * - 保持各个流的节点标识信息
 * - 支持流的合并和分发处理
 * - 提供统一的元数据管理
 *
 * @author disaster
 * @since 1.0.4
 */
public class ParallelGraphFlux {

    /**
     * 并行节点的GraphFlux列表
     */
    private final List<GraphFlux<?>> graphFluxes;

    /**
     * 全局元数据，用于存储并行处理的相关信息
     */
    private final Map<String, Object> metadata;

    /**
     * 私有构造函数
     */
    private ParallelGraphFlux(List<GraphFlux<?>> graphFluxes, Map<String, Object> metadata) {
        this.graphFluxes = graphFluxes != null ? new ArrayList<>(graphFluxes) : new ArrayList<>();
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    /**
     * 创建ParallelGraphFlux实例的静态工厂方法
     *
     * @param graphFluxes GraphFlux列表
     * @return ParallelGraphFlux实例
     */
    public static ParallelGraphFlux of(List<GraphFlux<?>> graphFluxes) {
        return new ParallelGraphFlux(graphFluxes, null);
    }

    /**
     * 创建ParallelGraphFlux实例的静态工厂方法，带有元数据
     *
     * @param graphFluxes GraphFlux列表
     * @param metadata 元数据
     * @return ParallelGraphFlux实例
     */
    public static ParallelGraphFlux of(List<GraphFlux<?>> graphFluxes, Map<String, Object> metadata) {
        return new ParallelGraphFlux(graphFluxes, metadata);
    }

    /**
     * 创建空的ParallelGraphFlux实例
     *
     * @return 空的ParallelGraphFlux实例
     */
    public static ParallelGraphFlux empty() {
        return new ParallelGraphFlux(null, null);
    }

    /**
     * 添加GraphFlux到并行列表中
     *
     * @param graphFlux 要添加的GraphFlux
     * @return 当前ParallelGraphFlux实例，支持链式调用
     */
    public ParallelGraphFlux addGraphFlux(GraphFlux<?> graphFlux) {
        if (graphFlux != null) {
            this.graphFluxes.add(graphFlux);
        }
        return this;
    }

    /**
     * 批量添加GraphFlux到并行列表中
     *
     * @param graphFluxes 要添加的GraphFlux列表
     * @return 当前ParallelGraphFlux实例，支持链式调用
     */
    public ParallelGraphFlux addGraphFluxes(List<GraphFlux<?>> graphFluxes) {
        if (graphFluxes != null) {
            this.graphFluxes.addAll(graphFluxes);
        }
        return this;
    }

    /**
     * 获取所有GraphFlux列表
     *
     * @return 只读的GraphFlux列表
     */
    public List<GraphFlux<?>> getGraphFluxes() {
        return Collections.unmodifiableList(graphFluxes);
    }

    /**
     * 获取元数据
     *
     * @return 元数据Map
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * 检查是否为空
     *
     * @return 如果没有GraphFlux返回true，否则返回false
     */
    public boolean isEmpty() {
        return graphFluxes.isEmpty();
    }

    /**
     * 获取GraphFlux的数量
     *
     * @return GraphFlux的数量
     */
    public int size() {
        return graphFluxes.size();
    }

    /**
     * 根据节点ID获取对应的GraphFlux
     *
     * @param nodeId 节点ID
     * @return 对应的GraphFlux，如果没找到返回null
     */
    public GraphFlux<?> getGraphFluxByNodeId(String nodeId) {
        return graphFluxes.stream()
                .filter(gf -> nodeId.equals(gf.getNodeId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取所有节点ID
     *
     * @return 节点ID列表
     */
    public List<String> getNodeIds() {
        return graphFluxes.stream()
                .map(GraphFlux::getNodeId)
                .collect(Collectors.toList());
    }

    /**
     * 检查是否包含指定节点ID的GraphFlux
     *
     * @param nodeId 节点ID
     * @return 如果包含返回true，否则返回false
     */
    public boolean containsNodeId(String nodeId) {
        return graphFluxes.stream()
                .anyMatch(gf -> nodeId.equals(gf.getNodeId()));
    }

    /**
     * 检查是否所有GraphFlux都有结果映射函数
     *
     * @return 如果所有GraphFlux都有结果映射函数返回true，空集合返回false
     */
    public boolean allHaveMapResult() {
        return !graphFluxes.isEmpty() && graphFluxes.stream()
                .allMatch(GraphFlux::hasMapResult);
    }

    /**
     * 检查是否有任一GraphFlux有结果映射函数
     *
     * @return 如果有任一GraphFlux有结果映射函数返回true，否则返回false
     */
    public boolean anyHaveMapResult() {
        return graphFluxes.stream()
                .anyMatch(GraphFlux::hasMapResult);
    }

    /**
     * 添加全局元数据
     *
     * @param key 键
     * @param value 值
     * @return 当前ParallelGraphFlux实例，支持链式调用
     */
    public ParallelGraphFlux withMetadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }

    /**
     * 添加多个全局元数据
     *
     * @param metadata 要添加的元数据Map
     * @return 当前ParallelGraphFlux实例，支持链式调用
     */
    public ParallelGraphFlux withMetadata(Map<String, Object> metadata) {
        if (metadata != null) {
            this.metadata.putAll(metadata);
        }
        return this;
    }


    /**
     * 创建一个新的ParallelGraphFlux，只包含满足条件的GraphFlux
     *
     * @param predicate 过滤条件
     * @return 新的ParallelGraphFlux实例
     */
    public ParallelGraphFlux filter(java.util.function.Predicate<GraphFlux<?>> predicate) {
        List<GraphFlux<?>> filtered = graphFluxes.stream()
                .filter(predicate)
                .collect(Collectors.toList());
        return new ParallelGraphFlux(filtered, this.metadata);
    }

    @Override
    public String toString() {
        return String.format("ParallelGraphFlux{size=%d, nodeIds=%s, metadata=%s}", 
                size(), getNodeIds(), metadata);
    }
}
