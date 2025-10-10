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

import reactor.core.publisher.Flux;

import java.util.function.Function;

/**
 * GraphFlux是一个泛型包装类，用于统一管理流式输出并关联节点标识。
 * <p>
 * 核心特性：
 * - 统一包装Flux流式数据并附加节点标识
 * - 支持自定义结果映射函数
 * - 保持向后兼容性，无需修改现有接口
 *
 * @param <T> 流式数据的类型
 * @author disaster
 * @since 1.0.4
 */
public class GraphFlux<T> {

    /**
     * 节点标识
     */
    private final String nodeId;

    /**
     * 流式数据
     */
    private final Flux<T> flux;

    /**
     * 存储key
     */
    private final String key;

    /**
     * 结果映射函数，用于将流式数据的最终结果转换为Map格式
     */
    private final Function<T,?> mapResult;



    /**
     * 私有构造函数，通过静态工厂方法创建实例
     */
    private GraphFlux(String nodeId, Flux<T> flux, String key, Function<T,?> mapResult) {
        this.nodeId = nodeId;
        this.flux = flux;
        this.key = key;
        this.mapResult = mapResult;
    }

    /**
     * 创建GraphFlux实例的静态工厂方法
     *
     * @param nodeId 节点标识
     * @param flux   流式数据
     * @param <T>    流式数据类型
     * @return GraphFlux实例
     */
    public static <T> GraphFlux<T> of(String nodeId, Flux<T> flux) {
        return new GraphFlux<>(nodeId, flux,null, null);
    }

    /**
     * 创建GraphFlux实例的静态工厂方法，带有结果映射函数
     *
     * @param nodeId    节点标识
     * @param flux      流式数据
     * @param mapResult 结果映射函数
     * @param <T>       流式数据类型
     * @return GraphFlux实例
     */
    public static <T> GraphFlux<T> of(String nodeId, String key, Flux<T> flux, Function<T, ?> mapResult) {
        return new GraphFlux<>(nodeId, flux, key, mapResult);
    }


    /**
     * 获取节点标识
     *
     * @return 节点标识
     */
    public String getNodeId() {
        return nodeId;
    }

    public String getKey() {
        return key;
    }

    /**
     * 获取流式数据
     *
     * @return Flux流式数据
     */
    public Flux<T> getFlux() {
        return flux;
    }

    /**
     * 获取结果映射函数
     *
     * @return 结果映射函数，可能为null
     */
    public Function getMapResult() {
        return mapResult;
    }


    /**
     * 检查是否有结果映射函数
     *
     * @return 如果有结果映射函数返回true，否则返回false
     */
    public boolean hasMapResult() {
        return mapResult != null;
    }




    @Override
    public String toString() {
        return String.format("GraphFlux{nodeId='%s', hasMapResult=%s}",
                nodeId, hasMapResult());
    }
}
