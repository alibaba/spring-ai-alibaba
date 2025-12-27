/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.agent.renderer;

import org.springframework.ai.template.TemplateRenderer;
import org.springframework.ai.template.ValidationMode;

import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.antlr.runtime.Token;
import org.antlr.runtime.TokenStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.compiler.Compiler;
import org.stringtemplate.v4.compiler.STLexer;

public class SaaStTemplateRenderer implements TemplateRenderer {

	private static final Logger logger = LoggerFactory.getLogger(SaaStTemplateRenderer.class);

	private static final String VALIDATION_MESSAGE = "Not all variables were replaced in the template. Missing variable names are: %s.";

	private static final char DEFAULT_START_DELIMITER_TOKEN = '{';

	private static final char DEFAULT_END_DELIMITER_TOKEN = '}';

	private static final ValidationMode DEFAULT_VALIDATION_MODE = ValidationMode.THROW;

	private static final boolean DEFAULT_VALIDATE_ST_FUNCTIONS = false;

	private final char startDelimiterToken;

	private final char endDelimiterToken;

	private final ValidationMode validationMode;

	private final boolean validateStFunctions;

	/**
	 * Constructs a new {@code SaaStTemplateRenderer} with the specified delimiter tokens,
	 * validation mode, and function validation flag.
	 * @param startDelimiterToken the character used to denote the start of a template
	 * variable (e.g., '{')
	 * @param endDelimiterToken the character used to denote the end of a template
	 * variable (e.g., '}')
	 * @param validationMode the mode to use for template variable validation; must not be
	 * null
	 * @param validateStFunctions whether to validate StringTemplate functions in the
	 * template
	 */
	public SaaStTemplateRenderer(char startDelimiterToken, char endDelimiterToken, ValidationMode validationMode,
			boolean validateStFunctions) {
		Assert.notNull(validationMode, "validationMode cannot be null");
		this.startDelimiterToken = startDelimiterToken;
		this.endDelimiterToken = endDelimiterToken;
		this.validationMode = validationMode;
		this.validateStFunctions = validateStFunctions;
	}

	@Override
	public String apply(String template, Map<String, Object> variables) {
		Assert.hasText(template, "template cannot be null or empty");
		Assert.notNull(variables, "variables cannot be null");
		Assert.noNullElements(variables.keySet(), "variables keys cannot be null");

		ST st = createST(template);
		for (Map.Entry<String, Object> entry : variables.entrySet()) {
			st.add(entry.getKey(), entry.getValue());
		}
		if (this.validationMode != ValidationMode.NONE) {
			validate(st, variables);
		}
		return st.render();
	}

	private ST createST(String template) {
		try {
			STGroup group = new STGroup(this.startDelimiterToken, this.endDelimiterToken);
			group.setListener(new Slf4jStErrorListener(logger));
			return new ST(group, template);
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("The template string is not valid.", ex);
		}
	}

	/**
	 * Validates that all required template variables are provided in the model. Returns
	 * the set of missing variables for further handling or logging.
	 * @param st the StringTemplate instance
	 * @param templateVariables the provided variables
	 * @return set of missing variable names, or empty set if none are missing
	 */
	private Set<String> validate(ST st, Map<String, Object> templateVariables) {
		Set<String> templateTokens = getInputVariables(st);
		Set<String> modelKeys = templateVariables.keySet();
		Set<String> missingVariables = new HashSet<>(templateTokens);
		missingVariables.removeAll(modelKeys);

		if (!missingVariables.isEmpty()) {
			if (this.validationMode == ValidationMode.WARN) {
				logger.warn(VALIDATION_MESSAGE.formatted(missingVariables));
			}
			else if (this.validationMode == ValidationMode.THROW) {
				throw new IllegalStateException(VALIDATION_MESSAGE.formatted(missingVariables));
			}
		}
		return missingVariables;
	}

	private Set<String> getInputVariables(ST st) {
		TokenStream tokens = st.impl.tokens;
		Set<String> inputVariables = new HashSet<>();
		boolean isInsideList = false;

		for (int i = 0; i < tokens.size(); i++) {
			Token token = tokens.get(i);

			// Handle list variables with option (e.g., {items; separator=", "})
			if (token.getType() == STLexer.LDELIM && i + 1 < tokens.size()
					&& tokens.get(i + 1).getType() == STLexer.ID) {
				if (i + 2 < tokens.size() && tokens.get(i + 2).getType() == STLexer.COLON) {
					String text = tokens.get(i + 1).getText();
					if (!org.stringtemplate.v4.compiler.Compiler.funcs.containsKey(text) || this.validateStFunctions) {
						inputVariables.add(text);
						isInsideList = true;
					}
				}
			}
			else if (token.getType() == STLexer.RDELIM) {
				isInsideList = false;
			}
			// Handle regular variables - only add IDs that are at the start of an
			// expression
			else if (!isInsideList && token.getType() == STLexer.ID) {
				// Check if this ID is a function call
				boolean isFunctionCall = (i + 1 < tokens.size() && tokens.get(i + 1).getType() == STLexer.LPAREN);

				// Check if this ID is at the beginning of an expression (not a property
				// access)
				boolean isAfterDot = (i > 0 && tokens.get(i - 1).getType() == STLexer.DOT);

				// Only add IDs that are:
				// 1. Not function calls
				// 2. Not property values (not preceded by a dot)
				// 3. Either not built-in functions or we're validating functions
				if (!isFunctionCall && !isAfterDot) {
					String varName = token.getText();
					if (!Compiler.funcs.containsKey(varName) || this.validateStFunctions) {
						inputVariables.add(varName);
					}
				}
			}
		}
		return inputVariables;
	}

	public static SaaStTemplateRenderer.Builder builder() {
		return new SaaStTemplateRenderer.Builder();
	}

	/**
	 * Builder for configuring and creating {@link SaaStTemplateRenderer} instances.
	 */
	public static final class Builder {

		private char startDelimiterToken = DEFAULT_START_DELIMITER_TOKEN;

		private char endDelimiterToken = DEFAULT_END_DELIMITER_TOKEN;

		private ValidationMode validationMode = DEFAULT_VALIDATION_MODE;

		private boolean validateStFunctions = DEFAULT_VALIDATE_ST_FUNCTIONS;

		private Builder() {
		}

		/**
		 * Sets the character used as the start delimiter for template expressions.
		 * Default is '{'.
		 * @param startDelimiterToken The start delimiter character.
		 * @return This builder instance for chaining.
		 */
		public SaaStTemplateRenderer.Builder startDelimiterToken(char startDelimiterToken) {
			this.startDelimiterToken = startDelimiterToken;
			return this;
		}

		/**
		 * Sets the character used as the end delimiter for template expressions. Default
		 * is '}'.
		 * @param endDelimiterToken The end delimiter character.
		 * @return This builder instance for chaining.
		 */
		public SaaStTemplateRenderer.Builder endDelimiterToken(char endDelimiterToken) {
			this.endDelimiterToken = endDelimiterToken;
			return this;
		}

		/**
		 * Sets the validation mode to control behavior when the provided variables do not
		 * match the variables required by the template. Default is
		 * {@link ValidationMode#THROW}.
		 * @param validationMode The desired validation mode.
		 * @return This builder instance for chaining.
		 */
		public SaaStTemplateRenderer.Builder validationMode(ValidationMode validationMode) {
			this.validationMode = validationMode;
			return this;
		}

		/**
		 * Configures the renderer to support StringTemplate's built-in functions during
		 * validation.
		 * <p>
		 * When enabled (set to true), identifiers in the template that match known ST
		 * function names (e.g., "first", "rest", "length") will not be treated as
		 * required input variables during validation.
		 * <p>
		 * When disabled (default, false), these identifiers are treated like regular
		 * variables and must be provided in the input map if validation is enabled
		 * ({@link ValidationMode#WARN} or {@link ValidationMode#THROW}).
		 * @return This builder instance for chaining.
		 */
		public SaaStTemplateRenderer.Builder validateStFunctions() {
			this.validateStFunctions = true;
			return this;
		}

		/**
		 * Builds and returns a new {@link SaaStTemplateRenderer} instance with the
		 * configured settings.
		 * @return A configured {@link SaaStTemplateRenderer}.
		 */
		public SaaStTemplateRenderer build() {
			return new SaaStTemplateRenderer(this.startDelimiterToken, this.endDelimiterToken, this.validationMode,
					this.validateStFunctions);
		}

	}

}
