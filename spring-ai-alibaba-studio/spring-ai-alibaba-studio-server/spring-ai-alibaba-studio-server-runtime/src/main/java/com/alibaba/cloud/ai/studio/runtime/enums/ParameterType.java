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

package com.alibaba.cloud.ai.studio.runtime.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Enum representing different parameter types for API parameters. Each type defines
 * validation rules for its corresponding data type.
 *
 * @since 1.0.0.3
 */
@Getter
public enum ParameterType {

	/** Represents numeric values (Integer, Long, Float, Double, BigDecimal) */
	NUMBER("Number") {
		@Override
		public boolean isValidType(Object obj) {
			return isNumber(obj);
		}
	},

	/** Represents boolean values */
	BOOLEAN("Boolean") {
		@Override
		public boolean isValidType(Object obj) {
			return obj instanceof Boolean;
		}
	},

	/** Represents string values */
	STRING("String") {
		@Override
		public boolean isValidType(Object obj) {
			return obj instanceof String;
		}
	},

	/** Represents any object type */
	OBJECT("Object") {
		@Override
		public boolean isValidType(Object obj) {
			return true;
		}
	},

	/** Represents file type */
	FILE("File") {
		@Override
		public boolean isValidType(Object obj) {
			return true;
		}
	},

	/** Represents an array of objects */
	ARRAY_OBJECT("Array<Object>") {
		@Override
		public boolean isValidType(Object obj) {
			return obj instanceof List;
		}
	},

	/** Represents an array of strings */
	ARRAY_STRING("Array<String>") {
		@Override
		public boolean isValidType(Object obj) {
			if (!(obj instanceof List)) {
				return false;
			}

			for (Object o : (List) obj) {
				if (!(o instanceof String)) {
					return false;
				}
			}

			return true;
		}
	},

	/** Represents an array of numbers */
	ARRAY_NUMBER("Array<Number>") {
		@Override
		public boolean isValidType(Object obj) {
			if (!(obj instanceof List)) {
				return false;
			}

			for (Object o : (List) obj) {
				if (!isNumber(o)) {
					return false;
				}
			}

			return true;
		}
	},

	/** Represents an array of booleans */
	ARRAY_BOOLEAN("Array<Boolean>") {
		@Override
		public boolean isValidType(Object obj) {
			if (!(obj instanceof List)) {
				return false;
			}

			for (Object o : (List) obj) {
				if (!(o instanceof Boolean)) {
					return false;
				}
			}

			return true;
		}
	},

	;

	/** The string representation of the parameter type */
	private final String type;

	ParameterType(String type) {
		this.type = type;
	}

	/**
	 * Converts a string to its corresponding ParameterType enum value
	 * @param type The string representation of the parameter type
	 * @return The matching ParameterType enum value, or null if not found
	 */
	public static ParameterType of(String type) {
		if (StringUtils.isBlank(type)) {
			return null;
		}

		Optional<ParameterType> any = Arrays.stream(values())
			.filter(parameterType -> type.equalsIgnoreCase(parameterType.getType()))
			.findAny();

		return any.orElse(null);
	}

	/**
	 * Validates if the given object matches this parameter type
	 * @param obj The object to validate
	 * @return true if the object is valid for this parameter type
	 */
	public abstract boolean isValidType(Object obj);

	/**
	 * Checks if an object is a numeric type
	 * @param obj The object to check
	 * @return true if the object is a numeric type
	 */
	private static boolean isNumber(Object obj) {
		return obj instanceof Integer || obj instanceof Long || obj instanceof Float || obj instanceof Double
				|| obj instanceof BigDecimal;
	}

}
