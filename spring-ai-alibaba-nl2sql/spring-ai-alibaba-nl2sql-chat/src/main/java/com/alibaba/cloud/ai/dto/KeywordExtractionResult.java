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
 * Keyword extraction result data structure, used to store extraction results for each
 * question variant
 *
 * @author zhangshenghang
 */
public class KeywordExtractionResult {

	/**
	 * Question variant
	 */
	private final String question;

	/**
	 * Extracted evidence list
	 */
	private final List<String> evidences;

	/**
	 * Extracted keyword list
	 */
	private final List<String> keywords;

	/**
	 * Whether extraction was successful
	 */
	private final boolean successful;

	/**
	 * Constructor - success case
	 * @param question question variant
	 * @param evidences extracted evidence list
	 * @param keywords extracted keyword list
	 */
	public KeywordExtractionResult(String question, List<String> evidences, List<String> keywords) {
		this.question = question;
		this.evidences = evidences;
		this.keywords = keywords;
		this.successful = true;
	}

	/**
	 * Constructor - failure case
	 * @param question question variant
	 * @param successful whether extraction was successful
	 */
	public KeywordExtractionResult(String question, boolean successful) {
		this.question = question;
		this.evidences = List.of();
		this.keywords = List.of();
		this.successful = successful;
	}

	/**
	 * Get question variant
	 * @return question variant
	 */
	public String getQuestion() {
		return question;
	}

	/**
	 * Get extracted evidence list
	 * @return evidence list
	 */
	public List<String> getEvidences() {
		return evidences;
	}

	/**
	 * Get extracted keyword list
	 * @return keyword list
	 */
	public List<String> getKeywords() {
		return keywords;
	}

	/**
	 * Whether extraction was successful
	 * @return whether successful
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
