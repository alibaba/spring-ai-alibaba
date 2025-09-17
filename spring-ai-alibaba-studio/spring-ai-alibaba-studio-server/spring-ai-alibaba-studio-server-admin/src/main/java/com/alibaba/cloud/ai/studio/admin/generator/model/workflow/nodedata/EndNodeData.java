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
package com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alibaba.cloud.ai.studio.admin.generator.model.Variable;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableSelector;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeData;
import org.springframework.util.StringUtils;

public class EndNodeData extends NodeData {

	public static Variable getDefaultOutputSchema() {
		return new Variable("output", VariableType.ARRAY_STRING);
	}

	private String outputKey;

	private String outputType;

	private String textTemplate;

	private final static Pattern VAR_PATTERN = Pattern.compile("\\{(\\w+)}");

	// textTemplate出现的变量名称
	private List<String> textTemplateVars;

	public String getOutputKey() {
		return outputKey;
	}

	public EndNodeData setOutputKey(String outputKey) {
		this.outputKey = outputKey;
		return this;
	}

	public String getOutputType() {
		return outputType;
	}

	public void setOutputType(String outputType) {
		this.outputType = outputType;
	}

	public String getTextTemplate() {
		return textTemplate;
	}

	public void setTextTemplate(String textTemplate) {
		this.textTemplate = textTemplate;
		// 更新textTemplateVars，模板的{vars}为要提取的变量
		Matcher matcher = VAR_PATTERN.matcher(textTemplate);
		List<String> vars = new ArrayList<>();
		while (matcher.find()) {
			String res = matcher.group(1);
			if (StringUtils.hasText(res)) {
				vars.add(res);
			}
		}
		this.setTextTemplateVars(Collections.unmodifiableList(vars));

	}

	public List<String> getTextTemplateVars() {
		return textTemplateVars;
	}

	public void setTextTemplateVars(List<String> textTemplateVars) {
		this.textTemplateVars = textTemplateVars;
	}

	public EndNodeData() {
	}

	public EndNodeData(List<VariableSelector> inputs, List<Variable> outputs) {
		super(inputs, outputs);
	}

}
