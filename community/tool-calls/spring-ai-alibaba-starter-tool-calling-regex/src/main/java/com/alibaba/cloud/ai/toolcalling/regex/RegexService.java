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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 北极星
 */
public class RegexService implements Function<RegexService.RegexRequest, Object> {

	/**
	 * 取得内容中匹配的所有结果，使用{@link Consumer}完成匹配结果处理
	 * @param pattern 编译后的正则模式
	 * @param content 被查找的内容
	 * @param consumer 匹配结果处理函数
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
	 * 取得内容中匹配的所有结果
	 * @param <T> 集合类型
	 * @param pattern 编译后的正则模式
	 * @param content 被查找的内容
	 * @param group 正则的分组
	 * @param collection 返回的集合类型
	 * @return 结果集
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

	/**
	 * Applies this function to the given argument.
	 * @param regexRequest the function argument
	 * @return the function result
	 */
	@Override
	public java.lang.Object apply(RegexRequest regexRequest) {
		String content = regexRequest.content;
		Pattern expression = regexRequest.expression;
		int group = regexRequest.group;
		return findAll(expression, content, group, new ArrayList<>());
	}

	record RegexRequest(@JsonProperty("content") String content, @JsonProperty("expression") Pattern expression,
			@JsonProperty("group") int group) {
	}

}
