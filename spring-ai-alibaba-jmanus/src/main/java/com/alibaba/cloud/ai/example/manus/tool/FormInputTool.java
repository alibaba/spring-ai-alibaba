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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.openai.api.OpenAiApi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM表单输入工具：支持带标签的多输入项和描述说明。
 */
public class FormInputTool implements ToolCallBiFunctionDef<FormInputTool.UserFormInput> {

	private static final Logger log = LoggerFactory.getLogger(FormInputTool.class);

	private static final String PARAMETERS = """
			{
			  "type": "object",
			  "properties": {
			    "inputs": {
			      "type": "array",
			      "description": "输入项列表，每项包含 label 和 value 字段",
			      "items": {
			        "type": "object",
			        "properties": {
			          "label": { "type": "string", "description": "输入项标签" },
			          "value": { "type": "string", "description": "输入内容" }
			        },
			        "required": ["label"]
			      }
			    },
			    "description": {
			      "type": "string",
			      "description": "如何填写这些输入项的说明"
			    }
			  },
			  "required": [ "description"]
			}
			""";

	public static final String name = "form_input";

	private static final String description = """
			提供一个带标签的多输入项表单工具。

			LLM可通过本工具 让用户 提交0个或多个输入项（每项有label和内容），并附带填写说明。
			允许用户提交0个输入项。
			适用于需要结构化输入的场景也可以用于模型需要等待用户输入然后再继续的场景.
			""";

	public static OpenAiApi.FunctionTool getToolDefinition() {
		OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(description, name, PARAMETERS);
		return new OpenAiApi.FunctionTool(function);
	}

	// Data structures:
	/**
	 * 表单输入项，包含标签和对应的值。
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
	 * 用户提交的表单数据，包含输入项列表和说明。
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

	private static final ObjectMapper objectMapper = new ObjectMapper();

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
	public ToolExecuteResult apply(UserFormInput formInput, ToolContext toolContext) {
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
	 * 获取由LLM定义的最新表单结构（包括描述和输入项标签及当前值）。 这个表单结构将用于在前端呈现给用户。
	 * @return 最新的 UserFormInput 对象，如果尚未定义则为 null。
	 */
	public UserFormInput getLatestUserFormInput() {
		return this.currentFormDefinition;
	}

	/**
	 * 设置用户提交的表单输入值。 这些值将更新 currentFormDefinition 中对应输入项的 value。
	 * @param submittedItems 用户提交的输入项列表 (label-value pairs).
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
		return description;
	}

	@Override
	public String getParameters() {
		return PARAMETERS;
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
	public void setPlanId(String planId) {
		// 可选实现
	}

	@Override
	public void cleanup(String planId) {
		// 可选实现
	}

	@Override
	public String getServiceGroup() {
		return "default-service-group";
	}

	/**
	 * 获取当前工具状态，包括表单说明和输入项 (包括用户已输入的值 if any)
	 */
	@Override
	public String getCurrentToolStateString() {
		if (currentFormDefinition == null) {
			return String.format("FormInputTool 状态：未定义表单。当前输入状态: %s", inputState.toString());
		}
		try {
			StringBuilder stateBuilder = new StringBuilder("FormInputTool 状态：\n");
			stateBuilder.append(String.format("说明：%s\n输入项：%s\n", currentFormDefinition.getDescription(),
					objectMapper.writeValueAsString(currentFormDefinition.getInputs())));
			stateBuilder.append(String.format("当前输入状态: %s\n", inputState.toString()));
			return stateBuilder.toString();
		}
		catch (JsonProcessingException e) {
			log.error("Error serializing currentFormDefinition for state string", e);
			return String.format("FormInputTool 状态：序列化输入项时出错。当前输入状态: %s", inputState.toString());
		}
	}

}
