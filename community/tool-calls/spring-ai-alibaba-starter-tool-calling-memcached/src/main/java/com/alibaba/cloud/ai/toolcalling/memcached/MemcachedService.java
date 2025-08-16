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
package com.alibaba.cloud.ai.toolcalling.memcached;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import net.spy.memcached.MemcachedClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

/**
 * auth: dahua
 */
public class MemcachedService {

	private static final Logger logger = LoggerFactory.getLogger(MemcachedService.class);

	private final MemcachedClient memcachedClient;

	private final MemcachedServiceSetter setter;

	private final MemcachedServiceGetter getter;

	private final MemcachedServiceDeleter deleter;

	private final MemcachedServiceReplacer replacer;

	private final MemcachedServiceAppender appender;

	public MemcachedService(MemcachedClient memcachedClient) {
		this.memcachedClient = memcachedClient;
		setter = new MemcachedServiceSetter();
		getter = new MemcachedServiceGetter();
		deleter = new MemcachedServiceDeleter();
		replacer = new MemcachedServiceReplacer();
		appender = new MemcachedServiceAppender();
	}

	public class MemcachedServiceSetter implements Function<MemcachedServiceSetter.Request, Boolean> {

		@Override
		public Boolean apply(MemcachedServiceSetter.Request request) {
			try {
				return memcachedClient.set(request.key(), request.ttl(), request.value()).get();
			}
			catch (Exception e) {
				logger.error("Set data to memcached failed. key {} value {} exception {}", request.key(),
						request.value(), e.getMessage(), e);
			}
			return false;
		}

		@JsonClassDescription("set data to memcached api")
		public record Request(@JsonPropertyDescription("key to memcached") String key,
				@JsonPropertyDescription("value to memcached") Object value,
				@JsonPropertyDescription("key ttl") int ttl) {
		}

	}

	public class MemcachedServiceGetter implements Function<MemcachedServiceGetter.Request, Object> {

		@Override
		public Object apply(MemcachedServiceGetter.Request request) {
			try {
				return memcachedClient.get(request.key());
			}
			catch (Exception e) {
				logger.error("Get data from memcached failed. key {} exception {}", request.key(), e.getMessage(), e);
			}
			return null;
		}

		@JsonClassDescription("get data from memcached api")
		public record Request(@JsonPropertyDescription("key to memcached") String key) {
		}

	}

	public class MemcachedServiceDeleter implements Function<MemcachedServiceDeleter.Request, Boolean> {

		@Override
		public Boolean apply(MemcachedServiceDeleter.Request request) {
			try {
				return memcachedClient.delete(request.key()).get();
			}
			catch (Exception e) {
				logger.error("Delete data from memcached failed. key {} exception {}", request.key(), e.getMessage(),
						e);
			}
			return false;
		}

		@JsonClassDescription("delete data from memcached api")
		public record Request(@JsonPropertyDescription("key to memcached") String key) {
		}

	}

	public class MemcachedServiceReplacer implements Function<MemcachedServiceReplacer.Request, Boolean> {

		@Override
		public Boolean apply(MemcachedServiceReplacer.Request request) {
			try {
				return memcachedClient.replace(request.key(), request.ttl(), request.value()).get();
			}
			catch (Exception e) {
				logger.error("Replace data to memcached failed. key {} value {} exception {}", request.key(),
						request.value(), e.getMessage(), e);
			}
			return false;
		}

		@JsonClassDescription("replace data to memcached api")
		public record Request(@JsonPropertyDescription("key to memcached") String key,
				@JsonPropertyDescription("value to memcached") Object value,
				@JsonPropertyDescription("key ttl") int ttl) {
		}

	}

	public class MemcachedServiceAppender implements Function<MemcachedServiceAppender.Request, Boolean> {

		@Override
		public Boolean apply(MemcachedServiceAppender.Request request) {
			try {
				return memcachedClient.append(request.key(), request.value()).get();
			}
			catch (Exception e) {
				logger.error("Append data to memcached failed. key {} value {} exception {}", request.key(),
						request.value(), e.getMessage(), e);
			}
			return false;
		}

		@JsonClassDescription("append data to memcached api")
		public record Request(@JsonPropertyDescription("key to memcached") String key,
				@JsonPropertyDescription("value to memcached") Object value) {
		}

	}

	public MemcachedServiceSetter setter() {
		return setter;
	}

	public MemcachedServiceGetter getter() {
		return getter;
	}

	public MemcachedServiceDeleter deleter() {
		return deleter;
	}

	public MemcachedServiceReplacer replacer() {
		return replacer;
	}

	public MemcachedServiceAppender appender() {
		return appender;
	}

	public void close() {
		if (this.memcachedClient != null) {
			this.memcachedClient.shutdown();
		}
	}

}
