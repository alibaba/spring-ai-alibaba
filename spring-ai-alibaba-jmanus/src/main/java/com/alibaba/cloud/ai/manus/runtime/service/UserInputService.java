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
package com.alibaba.cloud.ai.manus.runtime.service;

import com.alibaba.cloud.ai.manus.runtime.entity.vo.UserInputWaitState;
import com.alibaba.cloud.ai.manus.tool.FormInputTool;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class UserInputService implements IUserInputService {

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

	public UserInputWaitState createUserInputWaitState(String planId, String title, FormInputTool formInputTool) {
		UserInputWaitState waitState = new UserInputWaitState(planId, title, true);
		if (formInputTool != null) {
			// Assume FormInputTool has methods getFormDescription() and getFormInputs()
			// to get form information
			// This requires FormInputTool class to support these methods, or other ways
			// to get this information
			// This is indicative code, specific implementation depends on the actual
			// structure of FormInputTool
			FormInputTool.UserFormInput latestFormInput = formInputTool.getLatestUserFormInput();
			if (latestFormInput != null) {
				// Use title from form input if available, otherwise use the provided
				// title
				if (latestFormInput.getTitle() != null && !latestFormInput.getTitle().isEmpty()) {
					waitState.setTitle(latestFormInput.getTitle());
				}
				waitState.setFormDescription(latestFormInput.getDescription());
				if (latestFormInput.getInputs() != null) {
					List<Map<String, String>> formInputsForState = latestFormInput.getInputs()
						.stream()
						.map(inputItem -> {
							Map<String, String> inputMap = new HashMap<>();
							inputMap.put("label", inputItem.getLabel());
							inputMap.put("value", inputItem.getValue() != null ? inputItem.getValue() : "");
							if (inputItem.getName() != null) {
								inputMap.put("name", inputItem.getName());
							}
							if (inputItem.getType() != null) {
								inputMap.put("type", inputItem.getType().getValue());
							}
							if (inputItem.getPlaceholder() != null) {
								inputMap.put("placeholder", inputItem.getPlaceholder());
							}
							if (inputItem.getRequired() != null) {
								inputMap.put("required", inputItem.getRequired().toString());
							}
							if (inputItem.getOptions() != null && !inputItem.getOptions().isEmpty()) {
								inputMap.put("options", String.join(",", inputItem.getOptions()));
							}

							return inputMap;
						})
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
			// Assuming a default title or retrieve from tool if available
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
