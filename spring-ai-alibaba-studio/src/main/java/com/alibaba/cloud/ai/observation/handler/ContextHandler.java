package com.alibaba.cloud.ai.observation.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamWriteConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.observation.Observation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @Description:
 * @Author: XiaoYunTao
 * @Date: 2024/11/26
 */
public interface ContextHandler<T extends Observation.Context> {

	Logger LOGGER = LoggerFactory.getLogger(ContextHandler.class);

	ObjectMapper OBJECT_MAPPER = createObjectMapper();

	private static ObjectMapper createObjectMapper() {
		ObjectMapper mapper = new ObjectMapper();

		// 注册 JavaTimeModule
		mapper.registerModule(new JavaTimeModule());

		// 设置嵌套深度限制为 200
		mapper.getFactory().setStreamWriteConstraints(StreamWriteConstraints.builder().maxNestingDepth(20).build());

		return mapper;
	}

	/**
	 * 处理 Context 数据存储
	 */
	void handle(T context, long duration);

	/**
	 * 使用 Jackson 将对象转换为 JSON 字符串
	 */
	default String toJsonString(Object object) {
		try {
			return OBJECT_MAPPER.writeValueAsString(object);
		}
		catch (JsonProcessingException e) {
			LOGGER.error("JSON 序列化失败", e);
			return null;
		}
	}

	/**
	 * 安全地设置字段值，仅在值非空时设置。
	 * @param supplier 值的提供者
	 * @param setter 值的设置函数
	 * @param <T> 值的类型
	 */
	default <T> void setIfNotNull(Supplier<T> supplier, Consumer<T> setter) {
		try {
			T value = supplier.get();
			if (value != null) {
				setter.accept(value);
			}
		}
		catch (Exception e) {
			LOGGER.error("获取值失败", e);
		}
	}

}
