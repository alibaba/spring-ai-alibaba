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
package com.alibaba.cloud.ai.transformer.splitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.ai.transformer.splitter.TextSplitter;

/**
 * Title Recursive character text splitter.<br>
 * Description Text splitter implementation that recursively splits text by characters.
 *
 * @author HunterPorter
 */
public class RecursiveCharacterTextSplitter extends TextSplitter {

	/**
	 * Maximum size of each chunk 最大块大小
	 */
	private final int chunkSize;

	/**
	 * Array of separators to use for splitting 分隔符
	 */
	private final String[] separators;

	/**
	 * Create with default separators
	 */
	public RecursiveCharacterTextSplitter() {
		this(1024);
	}

	/**
	 * Create with custom chunk size and default separators
	 * @param chunkSize Maximum size of each chunk
	 */
	public RecursiveCharacterTextSplitter(int chunkSize) {
		this(chunkSize, null);
	}

	/**
	 * Create with custom chunk size, overlap and separators
	 * @param chunkSize Maximum size of each chunk
	 * @param separators Array of separators to use for splitting
	 */
	public RecursiveCharacterTextSplitter(int chunkSize, String[] separators) {
		if (chunkSize <= 0) {
			throw new IllegalArgumentException("Chunk size must be positive");
		}

		this.chunkSize = chunkSize;
		this.separators = Objects.requireNonNullElse(separators,
				new String[] { "\n\n", "\n", "。", "！", "？", "；", "，", " " });
	}

	@Override
	public List<String> splitText(String text) {
		List<String> chunks = new ArrayList<>();
		splitText(text, 0, chunks);
		return chunks;
	}

	private void splitText(String text, int separatorIndex, List<String> chunks) {
		if (text.isEmpty()) {
			return;
		}

		if (text.length() <= chunkSize) {
			chunks.add(text);
			return;
		}

		if (separatorIndex >= separators.length) {
			// Final fallback - split by chunkSize 最终按块大小分割
			for (int i = 0; i < text.length(); i += chunkSize) {
				int end = Math.min(i + chunkSize, text.length());
				chunks.add(text.substring(i, end));
			}
			return;
		}

		String separator = separators[separatorIndex];
		String[] splits;
		if (separator.isEmpty()) {
			// Split by character
			splits = new String[text.length()];
			for (int i = 0; i < text.length(); i++) {
				splits[i] = String.valueOf(text.charAt(i));
			}
		}
		else {
			// Split by separator
			splits = text.split(separator);
		}

		for (String split : splits) {
			if (split.length() > chunkSize) {
				splitText(split, separatorIndex + 1, chunks);
			}
			else {
				chunks.add(split);
			}
		}
	}

}
