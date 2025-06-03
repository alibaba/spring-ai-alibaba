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
package com.alibaba.cloud.ai.example.manus.planning.service;

import com.alibaba.cloud.ai.example.manus.planning.model.vo.UserInputWaitState;
import com.alibaba.cloud.ai.example.manus.tool.FormInputTool;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class UserInputService {

	private final ConcurrentHashMap<String, FormInputTool> formInputToolMap = new ConcurrentHashMap<>();

	public void storeFormInputTool(String planId, FormInputTool tool) {
		formInputToolMap.put(planId, tool);
	}

	public FormInputTool getFormInputTool(String planId) {
		return formInputToolMap.get(planId);
	}

	public void removeFormInputTool(String planId) {
		formInputToolMap.remove(planId);
	}

	public UserInputWaitState createUserInputWaitState(String planId, String message, FormInputTool formInputTool) {
		UserInputWaitState waitState = new UserInputWaitState(planId, message, true);
		if (formInputTool != null) {
			// 假设 FormInputTool 有方法 getFormDescription() 和 getFormInputs() 来获取表单信息
			// 这需要 FormInputTool 类支持这些方法，或者有其他方式获取这些信息
			// 此处为示意性代码，具体实现取决于 FormInputTool 的实际结构
			FormInputTool.UserFormInput latestFormInput = formInputTool.getLatestUserFormInput();
			if (latestFormInput != null) {
				waitState.setFormDescription(latestFormInput.getDescription());
				if (latestFormInput.getInputs() != null) {
					List<Map<String, String>> formInputsForState = latestFormInput.getInputs()
						.stream()
						.map(inputItem -> Map.of("label", inputItem.getLabel(), "value",
								inputItem.getValue() != null ? inputItem.getValue() : ""))
						.collect(Collectors.toList());
					waitState.setFormInputs(formInputsForState);
				}
			}
		}
		return waitState;
	}

	public UserInputWaitState getWaitState(String planId) {
		FormInputTool tool = getFormInputTool(planId);
		if (tool != null && tool.getInputState() == FormInputTool.InputState.AWAITING_USER_INPUT) { // Corrected
																									// to
																									// use
																									// getInputState
																									// and
																									// InputState
			// Assuming a default message or retrieve from tool if available
			return createUserInputWaitState(planId, "Awaiting user input.", tool);
		}
		return null; // Or a UserInputWaitState with waiting=false
	}

	public boolean submitUserInputs(String planId, Map<String, String> inputs) { // Changed
																					// to
																					// return
																					// boolean
		FormInputTool formInputTool = getFormInputTool(planId);
		if (formInputTool != null && formInputTool.getInputState() == FormInputTool.InputState.AWAITING_USER_INPUT) { // Corrected
																														// to
																														// use
																														// getInputState
																														// and
																														// InputState
			List<FormInputTool.InputItem> inputItems = inputs.entrySet().stream().map(entry -> {
				return new FormInputTool.InputItem(entry.getKey(), entry.getValue());
			}).collect(Collectors.toList());

			formInputTool.setUserFormInputValues(inputItems);
			formInputTool.markUserInputReceived();
			return true;
		}
		else {
			if (formInputTool == null) {
				throw new IllegalArgumentException("FormInputTool not found for planId: " + planId);
			}
			// If tool exists but not awaiting input
			return false;
		}
	}

}
