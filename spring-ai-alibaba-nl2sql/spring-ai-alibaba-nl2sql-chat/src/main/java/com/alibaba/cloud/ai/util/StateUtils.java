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

package com.alibaba.cloud.ai.util;

import com.alibaba.cloud.ai.graph.OverAllState;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.function.Supplier;

/**
 * 状态管理工具类，提供类型安全的状态获取方法
 *
 * @author zhangshenghang
 */
public class StateUtils {

	/**
	 * 安全获取字符串类型状态值
	 */
	public static String getStringValue(OverAllState state, String key) {
		return state.value(key)
			.map(String.class::cast)
			.orElseThrow(() -> new IllegalStateException("State key not found: " + key));
	}

	/**
	 * 安全获取字符串类型状态值，带默认值
	 */
	public static String getStringValue(OverAllState state, String key, String defaultValue) {
		return state.value(key).map(String.class::cast).orElse(defaultValue);
	}

	/**
	 * 安全获取列表类型状态值
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> getListValue(OverAllState state, String key) {
		return state.value(key)
			.map(v -> (List<T>) v)
			.orElseThrow(() -> new IllegalStateException("State key not found: " + key));
	}

	/**
	 * 安全获取对象类型状态值
	 */
	public static <T> T getObjectValue(OverAllState state, String key, Class<T> type) {
		return state.value(key)
			.map(type::cast)
			.orElseThrow(() -> new IllegalStateException("State key not found: " + key));
	}

	/**
	 * 安全获取对象类型状态值，带默认值
	 */
	public static <T> T getObjectValue(OverAllState state, String key, Class<T> type, T defaultValue) {
		return state.value(key).map(type::cast).orElse(defaultValue);
	}

	/**
	 * 安全获取对象类型状态值，带默认值提供器
	 */
	public static <T> T getObjectValue(OverAllState state, String key, Class<T> type, Supplier<T> defaultSupplier) {
		return state.value(key).map(type::cast).orElseGet(defaultSupplier);
	}

	/**
	 * 检查状态值是否存在
	 */
	public static boolean hasValue(OverAllState state, String key) {
		return state.value(key).isPresent() && !((String) state.value(key).get()).equals("");
	}

	/**
	 * 获取Document列表
	 */
	public static List<Document> getDocumentList(OverAllState state, String key) {
		return getListValue(state, key);
	}

	/**
	 * 获取Document列表的列表
	 */
	public static List<List<Document>> getDocumentListList(OverAllState state, String key) {
		return getListValue(state, key);
	}

}
