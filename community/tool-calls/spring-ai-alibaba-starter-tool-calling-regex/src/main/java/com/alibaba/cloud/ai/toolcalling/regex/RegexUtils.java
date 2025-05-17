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
package com.alibaba.cloud.ai.toolcalling.regex;

import org.springframework.util.Assert;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for regular expression operations
 *
 * @author Inlines10
 */
public final class RegexUtils {

	private RegexUtils() {
	}

	/**
	 * Find all matches in the content and process them using the provided consumer
	 * @param pattern compiled regex pattern
	 * @param content content to search in
	 * @param consumer consumer to process each match
	 */
	public static void findAll(Pattern pattern, CharSequence content, Consumer<Matcher> consumer) {
		if (null == pattern || null == content) {
			return;
		}

		final Matcher matcher = pattern.matcher(content);
		while (matcher.find()) {
			consumer.accept(matcher);
		}
	}

	/**
	 * Find all matches in the content and collect them into the provided collection
	 * @param <T> collection type
	 * @param pattern compiled regex pattern
	 * @param content content to search in
	 * @param group regex group to extract
	 * @param collection collection to store results
	 * @return collection with results
	 */
	public static <T extends Collection<String>> T findAll(Pattern pattern, CharSequence content, int group,
			T collection) {
		if (null == pattern || null == content) {
			return null;
		}
		Assert.notNull(collection, "Collection must be not null .");

		findAll(pattern, content, (matcher) -> collection.add(matcher.group(group)));
		return collection;
	}

}
