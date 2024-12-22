/*
 * Copyright 2024 - 2024 the original author or authors.
 *
 * 										Licensed under the Apache License, Version 2.0 (the "License");
 * 										you may not use this file except in compliance with the License.
 * 										You may obtain a copy of the License at
 *
 * 										https://www.apache.org/licenses/LICENSE-2.0
 *
 * 										Unless required by applicable law or agreed to in writing, software
 * 										distributed under the License is distributed on an "AS IS" BASIS,
 * 										WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 										See the License for the specific language governing permissions and
 * 										limitations under the License.
 */
package com.alibaba.cloud.ai.functioncalling.baidutranslate;

import java.util.List;

class TranslationResponse {

	private String from;

	private String to;

	private List<TranslationResult> trans_result;

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public List<TranslationResult> getTransResult() {
		return trans_result;
	}

	public void setTransResult(List<TranslationResult> trans_result) {
		this.trans_result = trans_result;
	}

}