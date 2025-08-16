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
package com.alibaba.cloud.ai.studio.core.workflow;

import com.google.common.collect.Sets;
import com.alibaba.cloud.ai.studio.runtime.enums.ParameterTypeEnum;
import lombok.Getter;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.studio.core.workflow.constants.WorkflowConstants.ARRAY_CODE;

/**
 * Enum representing different types of comparison operators for parameter validation
 */
@Getter
public enum JudgeOperator {

	/**
	 * Basic comparison operators
	 */
	EQUALS("equals", "equals",
			Sets.newHashSet(ParameterTypeEnum.STRING.getCode(), ParameterTypeEnum.NUMBER.getCode(),
					ParameterTypeEnum.BOOLEAN.getCode())),
	NOT_EQUALS("notEquals", "not equals",
			Sets.newHashSet(ParameterTypeEnum.STRING.getCode(), ParameterTypeEnum.NUMBER.getCode(),
					ParameterTypeEnum.BOOLEAN.getCode())),
	IS_NULL("isNull", "is null", ParameterTypeEnum.getAllCodes()),
	IS_NOT_NULL("isNotNull", "is not null", ParameterTypeEnum.getAllCodes()),
	GREATER("greater", "greater than", Sets.newHashSet(ParameterTypeEnum.NUMBER.getCode())),
	GREATER_AND_EQUAL("greaterAndEqual", "greater than or equal to",
			Sets.newHashSet(ParameterTypeEnum.NUMBER.getCode())),
	LESS("less", "less than", Sets.newHashSet(ParameterTypeEnum.NUMBER.getCode())),
	LESS_AND_EQUAL("lessAndEqual", "less than or equal to", Sets.newHashSet(ParameterTypeEnum.NUMBER.getCode())),
	IS_TRUE("isTrue", "is true", Sets.newHashSet(ParameterTypeEnum.BOOLEAN.getCode())),
	IS_FALSE("isFalse", "is false", Sets.newHashSet(ParameterTypeEnum.BOOLEAN.getCode())),

	/**
	 * Length comparison operators for arrays and strings
	 */
	LENGTH_EQUALS("lengthEquals", "length equals",
			Arrays.stream(ParameterTypeEnum.values())
				.map(ParameterTypeEnum::getCode)
				.filter(code -> code.startsWith(ARRAY_CODE) || code.equals(ParameterTypeEnum.STRING.getCode()))
				.collect(Collectors.toSet())),
	LENGTH_GREATER("lengthGreater", "length greater than",
			Arrays.stream(ParameterTypeEnum.values())
				.map(ParameterTypeEnum::getCode)
				.filter(code -> code.startsWith(ARRAY_CODE) || code.equals(ParameterTypeEnum.STRING.getCode()))
				.collect(Collectors.toSet())),
	LENGTH_GREATER_AND_EQUAL("lengthGreaterAndEqual", "length greater than or equal to",
			Arrays.stream(ParameterTypeEnum.values())
				.map(ParameterTypeEnum::getCode)
				.filter(code -> code.startsWith(ARRAY_CODE) || code.equals(ParameterTypeEnum.STRING.getCode()))
				.collect(Collectors.toSet())),
	LENGTH_LESS("lengthLess", "length less than",
			Arrays.stream(ParameterTypeEnum.values())
				.map(ParameterTypeEnum::getCode)
				.filter(code -> code.startsWith(ARRAY_CODE) || code.equals(ParameterTypeEnum.STRING.getCode()))
				.collect(Collectors.toSet())),
	LENGTH_LESS_AND_EQUAL("lengthLessAndEqual", "length less than or equal to",
			Arrays.stream(ParameterTypeEnum.values())
				.map(ParameterTypeEnum::getCode)
				.filter(code -> code.startsWith(ARRAY_CODE) || code.equals(ParameterTypeEnum.STRING.getCode()))
				.collect(Collectors.toSet())),

	/**
	 * Collection and object operators
	 */
	CONTAINS("contains", "contains",
			Arrays.stream(ParameterTypeEnum.values())
				.map(ParameterTypeEnum::getCode)
				.filter(code -> code.startsWith(ARRAY_CODE) || code.equals(ParameterTypeEnum.STRING.getCode())
						|| code.equals(ParameterTypeEnum.OBJECT.getCode()))
				.collect(Collectors.toSet())),
	NOT_CONTAINS("notContains", "does not contain",
			Arrays.stream(ParameterTypeEnum.values())
				.map(ParameterTypeEnum::getCode)
				.filter(code -> code.startsWith(ARRAY_CODE) || code.equals(ParameterTypeEnum.STRING.getCode())
						|| code.equals(ParameterTypeEnum.OBJECT.getCode()))
				.collect(Collectors.toSet())),;

	/**
	 * @return Set of all operator codes
	 */
	public static Set<String> getAllCodes() {
		return Arrays.stream(JudgeOperator.values()).map(JudgeOperator::getCode).collect(Collectors.toSet());
	}

	/**
	 * @param operator operator code
	 * @return JudgeOperator instance for the given code, or null if not found
	 */
	public static JudgeOperator getOperator(String operator) {
		return Arrays.stream(JudgeOperator.values())
			.filter(op -> op.getCode().equals(operator))
			.findFirst()
			.orElse(null);
	}

	/** Operator code */
	private final String code;

	/** Operator description */
	private final String desc;

	/** Set of parameter types this operator can be applied to */
	private final Set<String> scopeSet;

	JudgeOperator(String code, String desc, Set<String> scopeSet) {
		this.code = code;
		this.desc = desc;
		this.scopeSet = scopeSet;
	}

}
