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
package com.alibaba.cloud.ai.example.manus.tool;

import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.example.manus.dynamic.prompt.service.PromptService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM form input tool: supports multiple input items with labels and descriptions.
 */
@Component
public class FormInputTool extends AbstractBaseTool<FormInputTool.UserFormInput> {

	private final ObjectMapper objectMapper;

	@Autowired
	private PromptService promptService;

	private static final Logger log = LoggerFactory.getLogger(FormInputTool.class);

	private String getToolParameters() {
		try {
			return promptService.getPromptByName("FORM_INPUT_TOOL_PARAMETERS").getPromptContent();
		}
		catch (Exception e) {
			log.warn("Failed to load prompt-based tool parameters, using legacy configuration", e);
			return LEGACY_PARAMETERS;
		}
	}

	private String getToolDescription() {
		try {
			return promptService.getPromptByName("FORM_INPUT_TOOL_DESCRIPTION").getPromptContent();
		}
		catch (Exception e) {
			log.warn("Failed to load prompt-based tool description, using legacy configuration", e);
			return LEGACY_DESCRIPTION;
		}
	}

	private static final String LEGACY_PARAMETERS = """
			{
			  "type": "object",
			  "properties": {
			    "inputs": {
			      "type": "array",
			      "description": "List of input items, each containing label and value fields",
			      "items": {
			        "type": "object",
			        "properties": {
			          "label": { "type": "string", "description": "Input item label" },
			          "value": { "type": "string", "description": "Input content" }
			        },
			        "required": ["label"]
			      }
			    },
			    "description": {
			      "type": "string",
			      "description": "Instructions on how to fill these input items"
			    }
			  },
			  "required": [ "description"]
			}
			""";

	public static final String name = "form_input";

	private static final String LEGACY_DESCRIPTION = """
			Provides a labeled multi-input form tool.

			LLM can use this tool to let users submit 0 or more input items (each with label and content), along with filling instructions.
			Allows users to submit 0 input items.
			Suitable for scenarios requiring structured input and can also be used when the model needs to wait for user input before continuing.
			""";

	public OpenAiApi.FunctionTool getToolDefinition() {
		try {
			OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(getToolDescription(), name,
					getToolParameters());
			return new OpenAiApi.FunctionTool(function);
		}
		catch (Exception e) {
			log.warn("Failed to load prompt-based tool definition, using legacy configuration", e);
			OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(LEGACY_DESCRIPTION, name,
					LEGACY_PARAMETERS);
			return new OpenAiApi.FunctionTool(function);
		}
	}

	// Data structures:
	/**
	 * Form input item containing label and corresponding value.
	 */
	public static class InputItem {

		private String label;

		private String value;

		public InputItem() {
		}

		public InputItem(String label, String value) {
			this.label = label;
			this.value = value;
		}

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

	}

	/**
	 * User-submitted form data containing list of input items and description.
	 */
	public static class UserFormInput {

		private List<InputItem> inputs;

		private String description;

		public UserFormInput() {
		}

		public UserFormInput(List<InputItem> inputs, String description) {
			this.inputs = inputs;
			this.description = description;
		}

		public List<InputItem> getInputs() {
			return inputs;
		}

		public void setInputs(List<InputItem> inputs) {
			this.inputs = inputs;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

	}

	public FormInputTool(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public enum InputState {

		AWAITING_USER_INPUT, INPUT_RECEIVED, INPUT_TIMEOUT

	}

	private InputState inputState = InputState.INPUT_RECEIVED; // Default state

	private UserFormInput currentFormDefinition; // Stores the form structure defined by
													// LLM and its current values

	public InputState getInputState() {
		return inputState;
	}

	public void setInputState(InputState inputState) {
		this.inputState = inputState;
	}

	@Override
	public ToolExecuteResult run(UserFormInput formInput) {
		log.info("FormInputTool input: {}", formInput);

		this.currentFormDefinition = formInput;
		// Initialize values to empty string if null, to ensure they are present for
		// form binding
		if (this.currentFormDefinition != null && this.currentFormDefinition.getInputs() != null) {
			for (InputItem item : this.currentFormDefinition.getInputs()) {
				if (item.getValue() == null) {
					item.setValue(""); // Initialize with empty string
				}
			}
		}
		setInputState(InputState.AWAITING_USER_INPUT);

		// Return form definition as a structured result
		try {
			String formJson = objectMapper.writeValueAsString(formInput);
			return new ToolExecuteResult(formJson);
		}
		catch (Exception e) {
			log.error("Error serializing form input", e);
			return new ToolExecuteResult("{\"error\": \"Failed to process form input: " + e.getMessage() + "\"}");
		}
	}

	/**
	 * Get the latest form structure defined by LLM (including description and input item
	 * labels and current values). This form structure will be used to present to users in
	 * the frontend.
	 * @return latest UserFormInput object, or null if not yet defined.
	 */
	public UserFormInput getLatestUserFormInput() {
		return this.currentFormDefinition;
	}

	/**
	 * Set user-submitted form input values. These values will update the value of
	 * corresponding input items in currentFormDefinition.
	 * @param submittedItems list of input items submitted by user (label-value pairs).
	 */
	public void setUserFormInputValues(List<InputItem> submittedItems) {
		if (this.currentFormDefinition == null || this.currentFormDefinition.getInputs() == null) {
			log.warn("Cannot set user form input values: form definition is missing or has no inputs.");
			return;
		}
		if (submittedItems == null) {
			log.warn("Submitted items are null. No values to update.");
			return;
		}

		Map<String, String> submittedValuesMap = new HashMap<>();
		for (InputItem submittedItem : submittedItems) {
			if (submittedItem.getLabel() != null) {
				submittedValuesMap.put(submittedItem.getLabel(), submittedItem.getValue());
			}
		}

		for (InputItem definitionItem : this.currentFormDefinition.getInputs()) {
			if (definitionItem.getLabel() != null && submittedValuesMap.containsKey(definitionItem.getLabel())) {
				definitionItem.setValue(submittedValuesMap.get(definitionItem.getLabel()));
			}
		}
		// The caller (UserInputService) is responsible for calling
		// markUserInputReceived()
	}

	public void markUserInputReceived() {
		setInputState(InputState.INPUT_RECEIVED);
	}

	public void handleInputTimeout() {
		log.warn("Input timeout occurred. No input received from the user for form: {}",
				this.currentFormDefinition != null ? this.currentFormDefinition.getDescription() : "N/A");
		setInputState(InputState.INPUT_TIMEOUT);
		this.currentFormDefinition = null; // Clear form definition on timeout
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return getToolDescription();
	}

	@Override
	public String getParameters() {
		return getToolParameters();
	}

	@Override
	public Class<UserFormInput> getInputType() {
		return UserFormInput.class;
	}

	@Override
	public boolean isReturnDirect() {
		return true;
	}

	@Override
	public void cleanup(String planId) {
		// Optional implementation
	}

	@Override
	public String getServiceGroup() {
		return "default-service-group";
	}

	/**
	 * Get current tool state, including form description and input items (including
	 * user-entered values if any)
	 */
	@Override
	public String getCurrentToolStateString() {
		if (currentFormDefinition == null) {
			return String.format("FormInputTool Status: No form defined. Current input state: %s",
					inputState.toString());
		}
		try {
			StringBuilder stateBuilder = new StringBuilder("FormInputTool Status:\n");
			stateBuilder
				.append(String.format("Description: %s\nInput Items: %s\n", currentFormDefinition.getDescription(),
						objectMapper.writeValueAsString(currentFormDefinition.getInputs())));
			stateBuilder.append(String.format("Current input state: %s\n", inputState.toString()));
			return stateBuilder.toString();
		}
		catch (JsonProcessingException e) {
			log.error("Error serializing currentFormDefinition for state string", e);
			return String.format("FormInputTool Status: Error serializing input items. Current input state: %s",
					inputState.toString());
		}
	}

}
