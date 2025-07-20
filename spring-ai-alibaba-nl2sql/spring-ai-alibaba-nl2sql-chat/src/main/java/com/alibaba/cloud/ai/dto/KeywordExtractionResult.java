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

package com.alibaba.cloud.ai.dto;

import java.util.List;

/**
 * 关键词提取结果数据结构，用于存储每个问题变体的提取结果
 *
 * @author zhangshenghang
 */
public class KeywordExtractionResult {

	/**
	 * 问题变体
	 */
	private final String question;

	/**
	 * 提取的证据列表
	 */
	private final List<String> evidences;

	/**
	 * 提取的关键词列表
	 */
	private final List<String> keywords;

	/**
	 * 提取是否成功
	 */
	private final boolean successful;

	/**
	 * 构造函数 - 成功情况
	 * @param question 问题变体
	 * @param evidences 提取的证据列表
	 * @param keywords 提取的关键词列表
	 */
	public KeywordExtractionResult(String question, List<String> evidences, List<String> keywords) {
		this.question = question;
		this.evidences = evidences;
		this.keywords = keywords;
		this.successful = true;
	}

	/**
	 * 构造函数 - 失败情况
	 * @param question 问题变体
	 * @param successful 提取是否成功
	 */
	public KeywordExtractionResult(String question, boolean successful) {
		this.question = question;
		this.evidences = List.of();
		this.keywords = List.of();
		this.successful = successful;
	}

	/**
	 * 获取问题变体
	 * @return 问题变体
	 */
	public String getQuestion() {
		return question;
	}

	/**
	 * 获取提取的证据列表
	 * @return 证据列表
	 */
	public List<String> getEvidences() {
		return evidences;
	}

	/**
	 * 获取提取的关键词列表
	 * @return 关键词列表
	 */
	public List<String> getKeywords() {
		return keywords;
	}

	/**
	 * 提取是否成功
	 * @return 是否成功
	 */
	public boolean isSuccessful() {
		return successful;
	}

	@Override
	public String toString() {
		return "KeywordExtractionResult{" + "question='" + question + '\'' + ", evidences=" + evidences + ", keywords="
				+ keywords + ", successful=" + successful + '}';
	}

}
