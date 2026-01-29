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

	private static final String DEFAULT_START_DELIMITER_STRING = "{";

	private static final String DEFAULT_END_DELIMITER_STRING = "}";

	private static final ValidationMode DEFAULT_VALIDATION_MODE = ValidationMode.THROW;

	private static final boolean DEFAULT_VALIDATE_ST_FUNCTIONS = false;

	// Use uncommon characters as temporary delimiters for string-based delimiter conversion
	private static final char TEMP_START_DELIMITER = '\u0001';

	private static final char TEMP_END_DELIMITER = '\u0002';

	private final char startDelimiterToken;

	private final char endDelimiterToken;

	private final String startDelimiterString;

	private final String endDelimiterString;

	private final boolean useStringDelimiters;

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
		this.startDelimiterString = String.valueOf(startDelimiterToken);
		this.endDelimiterString = String.valueOf(endDelimiterToken);
		this.useStringDelimiters = false;
		this.validationMode = validationMode;
		this.validateStFunctions = validateStFunctions;
	}

	/**
	 * Constructs a new {@code SaaStTemplateRenderer} with the specified string delimiter tokens,
	 * validation mode, and function validation flag.
	 * <p>
	 * This constructor supports multi-character delimiters (e.g., "{{" and "}}"), which is
	 * useful for avoiding conflicts with JSON content or other text that uses single-character
	 * delimiters.
	 * @param startDelimiterString the string used to denote the start of a template
	 * variable (e.g., "{{")
	 * @param endDelimiterString the string used to denote the end of a template
	 * variable (e.g., "}}")
	 * @param validationMode the mode to use for template variable validation; must not be
	 * null
	 * @param validateStFunctions whether to validate StringTemplate functions in the
	 * template
	 */
	public SaaStTemplateRenderer(String startDelimiterString, String endDelimiterString, ValidationMode validationMode,
			boolean validateStFunctions) {
		Assert.notNull(validationMode, "validationMode cannot be null");
		Assert.hasText(startDelimiterString, "startDelimiterString cannot be null or empty");
		Assert.hasText(endDelimiterString, "endDelimiterString cannot be null or empty");
		this.startDelimiterString = startDelimiterString;
		this.endDelimiterString = endDelimiterString;
		this.useStringDelimiters = true;
		// For string delimiters, we use temporary single-char delimiters internally
		this.startDelimiterToken = TEMP_START_DELIMITER;
		this.endDelimiterToken = TEMP_END_DELIMITER;
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
		String result = st.render();
		// Restore protected JSON content in the result
		if (!this.useStringDelimiters) {
			result = restoreJsonContentInResult(result);
		}
		return result;
	}

	private ST createST(String template) {
		try {
			String processedTemplate = template;
			// If using string delimiters, convert them to single-char delimiters for ST
			if (this.useStringDelimiters) {
				processedTemplate = convertStringDelimitersToChar(template);
			}
			else {
				// For single-char delimiters, protect JSON content to avoid conflicts
				processedTemplate = protectJsonContent(template);
			}
			STGroup group = new STGroup(this.startDelimiterToken, this.endDelimiterToken);
			group.setListener(new Slf4jStErrorListener(logger));
			return new ST(group, processedTemplate);
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("The template string is not valid.", ex);
		}
	}

	// Temporary placeholders for JSON braces
	private static final String JSON_OPEN_PLACEHOLDER = "\uE000";
	private static final String JSON_CLOSE_PLACEHOLDER = "\uE001";

	/**
	 * Protects JSON content in templates when using single-character delimiters.
	 * This method identifies JSON objects/arrays and replaces their braces with placeholders
	 * to prevent StringTemplate from treating them as template variables.
	 * @param template the original template
	 * @return template with JSON braces protected
	 */
	private String protectJsonContent(String template) {
		StringBuilder result = new StringBuilder();
		int i = 0;
		int len = template.length();

		while (i < len) {
			// Look for potential JSON start: { followed by quote or whitespace+quote
			int braceIdx = template.indexOf(this.startDelimiterToken, i);
			if (braceIdx == -1) {
				result.append(template.substring(i));
				break;
			}

			// Append text before the brace
			result.append(template.substring(i, braceIdx));

			// Check if this is a template variable or JSON content
			int afterBrace = braceIdx + 1;
			if (afterBrace < len) {
				char nextChar = template.charAt(afterBrace);
				// If followed by a valid identifier start (letter, underscore, $), it's a template variable
				boolean isTemplateVar = (Character.isLetter(nextChar) || nextChar == '_' || nextChar == '$');

				if (!isTemplateVar) {
					// Check if it looks like JSON: { followed by quote, whitespace+quote, or other JSON chars
					boolean looksLikeJson = false;
					if (nextChar == '"' || nextChar == '\'' || nextChar == '[' || nextChar == '{') {
						looksLikeJson = true;
					}
					else if (Character.isWhitespace(nextChar)) {
						// Check if whitespace is followed by a quote (JSON-like)
						int nextNonWhitespace = afterBrace + 1;
						while (nextNonWhitespace < len && Character.isWhitespace(template.charAt(nextNonWhitespace))) {
							nextNonWhitespace++;
						}
						if (nextNonWhitespace < len) {
							char afterWhitespace = template.charAt(nextNonWhitespace);
							if (afterWhitespace == '"' || afterWhitespace == '\'' || afterWhitespace == '[') {
								looksLikeJson = true;
							}
						}
					}

					if (looksLikeJson) {
						// This looks like JSON, find the matching closing brace
						int closeBraceIdx = findMatchingJsonBrace(template, afterBrace);
						if (closeBraceIdx != -1) {
							// Replace JSON braces with placeholders
							result.append(JSON_OPEN_PLACEHOLDER);
							result.append(template.substring(afterBrace, closeBraceIdx));
							result.append(JSON_CLOSE_PLACEHOLDER);
							i = closeBraceIdx + 1;
							continue;
						}
					}
				}
			}

			// Not JSON, keep as-is (it's a template variable)
			result.append(this.startDelimiterToken);
			i = braceIdx + 1;
		}

		return result.toString();
	}

	/**
	 * Finds the matching closing brace for a JSON object/array, handling nested structures.
	 * @param template the template string
	 * @param startPos the position after the opening brace
	 * @return the index of the matching closing brace, or -1 if not found
	 */
	private int findMatchingJsonBrace(String template, int startPos) {
		int depth = 1;
		int i = startPos;
		int len = template.length();
		boolean inString = false;
		char stringChar = 0;

		while (i < len && depth > 0) {
			char ch = template.charAt(i);

			// Handle string literals
			if (!inString && (ch == '"' || ch == '\'')) {
				inString = true;
				stringChar = ch;
			}
			else if (inString && ch == stringChar) {
				// Check if it's escaped
				if (i == 0 || template.charAt(i - 1) != '\\') {
					inString = false;
					stringChar = 0;
				}
			}

			if (!inString) {
				if (ch == this.startDelimiterToken) {
					depth++;
				}
				else if (ch == this.endDelimiterToken) {
					depth--;
					if (depth == 0) {
						return i;
					}
				}
			}

			i++;
		}

		return -1;
	}

	/**
	 * Restores protected JSON content in the rendered result by replacing placeholders back to braces.
	 * @param result the rendered result string
	 * @return result with JSON braces restored
	 */
	private String restoreJsonContentInResult(String result) {
		return result.replace(JSON_OPEN_PLACEHOLDER, String.valueOf(this.startDelimiterToken))
				.replace(JSON_CLOSE_PLACEHOLDER, String.valueOf(this.endDelimiterToken));
	}

	/**
	 * Converts string-based delimiters (e.g., "{{" and "}}") to single-character delimiters
	 * for StringTemplate processing. This allows support for multi-character delimiters while
	 * working with StringTemplate V4's char-based API.
	 * <p>
	 * The conversion uses a pattern matching approach to identify template variables while
	 * avoiding conflicts with literal text that might contain the delimiter strings.
	 * This method handles StringTemplate expressions including:
	 * <ul>
	 *   <li>Simple variables: {{name}}</li>
	 *   <li>Property access: {{user.name}}</li>
	 *   <li>Function calls: {{first(items)}}</li>
	 *   <li>Conditional expressions: {{name ?: "default"}}</li>
	 *   <li>List operations: {{items; separator=", "}}</li>
	 * </ul>
	 * @param template the original template with string delimiters
	 * @return the template with converted single-character delimiters
	 */
	private String convertStringDelimitersToChar(String template) {
		StringBuilder result = new StringBuilder();
		int i = 0;
		int len = template.length();
		int startLen = this.startDelimiterString.length();
		int endLen = this.endDelimiterString.length();

		while (i < len) {
			// Look for start delimiter
			int startIdx = template.indexOf(this.startDelimiterString, i);
			if (startIdx == -1) {
				// No more start delimiters, append the rest
				result.append(template.substring(i));
				break;
			}

			// Append text before the start delimiter
			result.append(template.substring(i, startIdx));

			// Check if this is a valid template variable start
			// A valid start should be followed by an identifier or StringTemplate expression
			int afterStart = startIdx + startLen;
			if (afterStart < len) {
				char nextChar = template.charAt(afterStart);
				// Check if next character is a valid identifier start (letter, underscore, $, or whitespace for expressions)
				// Also allow whitespace after delimiter for expressions like "{{ name }}"
				if (Character.isLetter(nextChar) || nextChar == '_' || nextChar == '$'
						|| Character.isWhitespace(nextChar)) {
					// This looks like a template variable, find the matching end delimiter
					// We need to handle nested delimiters and balanced matching
					int endIdx = findMatchingEndDelimiter(template, afterStart, startLen, endLen);
					if (endIdx != -1) {
						// Found matching end delimiter
						// Extract the variable content
						String variableContent = template.substring(afterStart, endIdx);
						// Trim whitespace if needed, but preserve internal structure
						variableContent = variableContent.trim();
						// Convert to single-char delimiter format
						result.append(this.startDelimiterToken);
						result.append(variableContent);
						result.append(this.endDelimiterToken);
						i = endIdx + endLen;
						continue;
					}
				}
			}

			// Not a valid template variable, keep the original delimiter as-is
			result.append(this.startDelimiterString);
			i = startIdx + startLen;
		}

		return result.toString();
	}

	/**
	 * Finds the matching end delimiter for a template expression, handling nested delimiters
	 * and balanced matching.
	 * @param template the template string
	 * @param startPos the position after the start delimiter
	 * @param startLen the length of the start delimiter string
	 * @param endLen the length of the end delimiter string
	 * @return the index of the matching end delimiter, or -1 if not found
	 */
	private int findMatchingEndDelimiter(String template, int startPos, int startLen, int endLen) {
		int depth = 1; // We've already found one start delimiter
		int i = startPos;
		int len = template.length();
		boolean inString = false;
		char stringChar = 0;

		while (i < len) {
			// Check for string delimiters (single or double quotes)
			if (!inString) {
				char ch = template.charAt(i);
				if (ch == '"' || ch == '\'') {
					inString = true;
					stringChar = ch;
					i++;
					continue;
				}
			}
			else {
				char ch = template.charAt(i);
				if (ch == stringChar && (i == 0 || template.charAt(i - 1) != '\\')) {
					inString = false;
					stringChar = 0;
					i++;
					continue;
				}
				i++;
				continue;
			}

			// Check for nested start delimiter
			if (i + startLen <= len) {
				String potentialStart = template.substring(i, i + startLen);
				if (potentialStart.equals(this.startDelimiterString)) {
					depth++;
					i += startLen;
					continue;
				}
			}

			// Check for end delimiter
			if (i + endLen <= len) {
				String potentialEnd = template.substring(i, i + endLen);
				if (potentialEnd.equals(this.endDelimiterString)) {
					depth--;
					if (depth == 0) {
						return i;
					}
					i += endLen;
					continue;
				}
			}

			i++;
		}

		return -1; // No matching end delimiter found
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

		private String startDelimiterString = DEFAULT_START_DELIMITER_STRING;

		private String endDelimiterString = DEFAULT_END_DELIMITER_STRING;

		private boolean useStringDelimiters = false;

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
			this.startDelimiterString = String.valueOf(startDelimiterToken);
			this.useStringDelimiters = false;
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
			this.endDelimiterString = String.valueOf(endDelimiterToken);
			this.useStringDelimiters = false;
			return this;
		}

		/**
		 * Sets the string used as the start delimiter for template expressions.
		 * This allows using multi-character delimiters (e.g., "{{") which is useful
		 * for avoiding conflicts with JSON content or other text that uses single-character
		 * delimiters.
		 * <p>
		 * When string delimiters are set, they take precedence over character delimiters.
		 * @param startDelimiterString The start delimiter string (e.g., "{{").
		 * @return This builder instance for chaining.
		 */
		public SaaStTemplateRenderer.Builder startDelimiter(String startDelimiterString) {
			Assert.hasText(startDelimiterString, "startDelimiterString cannot be null or empty");
			this.startDelimiterString = startDelimiterString;
			this.useStringDelimiters = true;
			return this;
		}

		/**
		 * Sets the string used as the end delimiter for template expressions.
		 * This allows using multi-character delimiters (e.g., "}}") which is useful
		 * for avoiding conflicts with JSON content or other text that uses single-character
		 * delimiters.
		 * <p>
		 * When string delimiters are set, they take precedence over character delimiters.
		 * @param endDelimiterString The end delimiter string (e.g., "}}").
		 * @return This builder instance for chaining.
		 */
		public SaaStTemplateRenderer.Builder endDelimiter(String endDelimiterString) {
			Assert.hasText(endDelimiterString, "endDelimiterString cannot be null or empty");
			this.endDelimiterString = endDelimiterString;
			this.useStringDelimiters = true;
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
			if (this.useStringDelimiters) {
				return new SaaStTemplateRenderer(this.startDelimiterString, this.endDelimiterString, this.validationMode,
						this.validateStFunctions);
			}
			else {
				return new SaaStTemplateRenderer(this.startDelimiterToken, this.endDelimiterToken, this.validationMode,
						this.validateStFunctions);
			}
		}

	}

}
