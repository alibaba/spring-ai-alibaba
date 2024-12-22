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

class TranslationResult {

	private String src;

	private String dst;

	public String getSrc() {
		return src;
	}

	public void setSrc(String src) {
		this.src = src;
	}

	public String getDst() {
		return dst;
	}

	public void setDst(String dst) {
		this.dst = dst;
	}

}