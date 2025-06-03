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
package com.alibaba.cloud.ai.example.manus.planning.model.vo;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class UserInputWaitState implements Serializable {

	private String planId;

	private String message;

	private boolean waiting;

	private String formDescription; // 新增字段：表单描述

	private List<Map<String, String>> formInputs; // 新增字段：表单输入项

	public UserInputWaitState() {
	}

	public UserInputWaitState(String planId, String message, boolean waiting) {
		this.planId = planId;
		this.message = message;
		this.waiting = waiting;
	}

	public String getPlanId() {
		return planId;
	}

	public void setPlanId(String planId) {
		this.planId = planId;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public boolean isWaiting() {
		return waiting;
	}

	public void setWaiting(boolean waiting) {
		this.waiting = waiting;
	}

	public String getFormDescription() {
		return formDescription;
	}

	public void setFormDescription(String formDescription) {
		this.formDescription = formDescription;
	}

	public List<Map<String, String>> getFormInputs() {
		return formInputs;
	}

	public void setFormInputs(List<Map<String, String>> formInputs) {
		this.formInputs = formInputs;
	}

}
