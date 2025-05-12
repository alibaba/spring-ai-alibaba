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
package com.alibaba.cloud.ai.graph.utils;

import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryFileStorage {

	private static final Map<String, FileRecord> CACHE = new ConcurrentHashMap<>();

	public static class FileRecord {

		private final String id;

		private final String fileKey;

		private final String name;

		private final String mimetype;

		private final long size;

		private final byte[] content;

		public FileRecord(String id, String fileKey, String name, String mimetype, long size, byte[] content) {
			this.id = id;
			this.fileKey = fileKey;
			this.name = name;
			this.mimetype = mimetype;
			this.size = size;
			this.content = content;
		}

		public String getId() {
			return id;
		}

		public String getFileKey() {
			return fileKey;
		}

		public String getName() {
			return name;
		}

		public String getMimetype() {
			return mimetype;
		}

		public long getSize() {
			return size;
		}

		public byte[] getContent() {
			return content;
		}

	}

	public static FileRecord save(byte[] content, String mimetype, String originalFilename) {
		String id = UUID.randomUUID().toString();
		String extension = Optional.of(org.springframework.http.MediaType.parseMediaType(mimetype).getSubtype())
			.orElse("bin");
		String filename = StringUtils.hasText(originalFilename) ? originalFilename : id + "." + extension;
		String key = String.format("inmem://%s", id);
		FileRecord record = new FileRecord(id, key, filename, mimetype, content.length, content);
		CACHE.put(id, record);
		return record;
	}

	public static FileRecord get(String id) {
		return CACHE.get(id);
	}

	public static void remove(String id) {
		CACHE.remove(id);
	}

	public static void clear() {
		CACHE.clear();
	}

}
