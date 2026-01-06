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

package com.alibaba.cloud.ai.studio.core.rag.splitter;

import org.springframework.ai.transformer.splitter.TextSplitter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A text splitter that splits text based on a regular expression pattern. It supports
 * overlapping segments to maintain context between splits.
 *
 * @since 1.0.0.3
 */
public class RegexTextSplitter extends TextSplitter {

	/** The compiled regular expression pattern used for splitting */
	private final Pattern pattern;

	/** The number of characters to overlap between consecutive segments */
	private final int overlapSize;

	/**
	 * Creates a new RegexTextSplitter with the specified regex pattern and overlap size.
	 * @param regex The regular expression pattern to split the text
	 * @param overlapSize The number of characters to overlap between segments
	 */
	public RegexTextSplitter(String regex, int overlapSize) {
		this.pattern = Pattern.compile(regex);
		this.overlapSize = overlapSize;
	}

	/**
	 * Splits the input text into segments based on the regex pattern. Each segment
	 * includes an overlap with the previous segment to maintain context.
	 * @param text The text to split
	 * @return List of text segments
	 */
	@Override
	protected List<String> splitText(String text) {
		List<String> segments = new ArrayList<>();
		Matcher matcher = pattern.matcher(text);

		int lastEnd = 0;
		while (matcher.find()) {
			int start = matcher.start();
			int end = matcher.end();

			int overlapStart = Math.max(0, start - overlapSize);
			if (lastEnd == 0) {
				segments.add(text.substring(0, end));
			}
			else {
				segments.add(text.substring(overlapStart, end));
			}

			lastEnd = end;
		}

		if (lastEnd < text.length()) {
			int overlapStart = Math.max(0, lastEnd - overlapSize);
			segments.add(text.substring(overlapStart));
		}

		return segments;
	}

}
