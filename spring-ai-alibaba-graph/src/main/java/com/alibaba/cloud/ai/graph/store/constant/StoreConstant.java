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
package com.alibaba.cloud.ai.graph.store.constant;

/**
 * Constants for the Store module.
 * 
 * @author Spring AI Alibaba
 * @since 1.0.0.3
 */
public final class StoreConstant {

	/**
	 * Default Redis key prefix for Store items.
	 */
	public static final String REDIS_KEY_PREFIX = "spring:ai:alibaba:store:";

	/**
	 * Default namespace separator.
	 */
	public static final String NAMESPACE_SEPARATOR = "/";

	/**
	 * Store type identifiers
	 */
	public static final String STORE_TYPE_MEMORY = "memory";

	public static final String STORE_TYPE_REDIS = "redis";

	public static final String STORE_TYPE_FILESYSTEM = "filesystem";

	public static final String STORE_TYPE_MONGODB = "mongodb";

	public static final String STORE_TYPE_DATABASE = "database";

	/**
	 * Private constructor to prevent instantiation.
	 */
	private StoreConstant() {
		throw new UnsupportedOperationException("Utility class cannot be instantiated");
	}

}
