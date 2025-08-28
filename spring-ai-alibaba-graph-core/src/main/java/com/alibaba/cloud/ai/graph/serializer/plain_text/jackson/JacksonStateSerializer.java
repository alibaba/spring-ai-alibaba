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
package com.alibaba.cloud.ai.graph.serializer.plain_text.jackson;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.serializer.plain_text.PlainTextStateSerializer;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.charset.StandardCharsets;

/**
 * Base Implementation of {@link PlainTextStateSerializer} using Jackson library. Need to
 * be extended from specific state implementation
 *
 */
public abstract class JacksonStateSerializer extends PlainTextStateSerializer {

	protected final ObjectMapper objectMapper;

	protected JacksonStateSerializer(AgentStateFactory<OverAllState> stateFactory) {
		this(stateFactory, new ObjectMapper());
	}

	protected JacksonStateSerializer(AgentStateFactory<OverAllState> stateFactory, ObjectMapper objectMapper) {
		super(stateFactory);
		this.objectMapper = objectMapper;
		configureObjectMapper(this.objectMapper);
	}

	/**
	 * Configure ObjectMapper with secure type handling to preserve type information while
	 * preventing deserialization attacks using blacklist approach
	 */
	private void configureObjectMapper(ObjectMapper mapper) {
		mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

		// Create a secure polymorphic type validator with blacklist for dangerous classes
		PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
			// Allow most types by default, but deny dangerous ones
			.allowIfBaseType(Object.class)
			// Blacklist dangerous classes that could be used for attacks
			.denyForExactBaseType(java.lang.ProcessBuilder.class)
			.denyForExactBaseType(java.lang.Runtime.class)
			.denyForExactBaseType(java.lang.Process.class)
			.denyForExactBaseType(java.io.FileInputStream.class)
			.denyForExactBaseType(java.io.FileOutputStream.class)
			.denyForExactBaseType(java.io.FileReader.class)
			.denyForExactBaseType(java.io.FileWriter.class)
			.denyForExactBaseType(java.net.Socket.class)
			.denyForExactBaseType(java.net.ServerSocket.class)
			.denyForExactBaseType(java.net.URL.class)
			.denyForExactBaseType(java.net.URLConnection.class)
			.denyForExactBaseType(java.net.HttpURLConnection.class)
			.denyForExactBaseType(java.lang.reflect.Method.class)
			.denyForExactBaseType(java.lang.reflect.Constructor.class)
			.denyForExactBaseType(java.lang.reflect.Field.class)
			.denyForExactBaseType(java.lang.Class.class)
			.denyForExactBaseType(java.lang.ClassLoader.class)
			.denyForExactBaseType(java.beans.Expression.class)
			.denyForExactBaseType(java.beans.Statement.class)
			.build();

		// Enable polymorphic type handling with security validation
		mapper.activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

		// Configure to handle missing properties gracefully
		mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES,
				false);
		mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, false);

		// Enable support for creating objects without default constructors
		mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);

		// Additional configuration for handling complex objects
		mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
		mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
				true);

		// Configure collection handling to avoid SubList issues
		mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY, false);

		// Set custom deserialization problem handler for automatic fallback to HashMap
		mapper.addHandler(new FallbackDeserializationProblemHandler());

		// Register custom modules
		registerSpringAIMessageModule(mapper);
		registerCollectionModule(mapper);
		registerFallbackModule(mapper);
	}

	/**
	 * Register custom serialization/deserialization support for Spring AI Message types
	 */
	private void registerSpringAIMessageModule(ObjectMapper mapper) {
		SimpleModule springAIModule = new SimpleModule("SpringAIMessageModule");

		// Try to register Spring AI message deserializers if classes are available
		try {
			Class<?> userMessageClass = Class.forName("org.springframework.ai.chat.messages.UserMessage");
			Class<?> assistantMessageClass = Class.forName("org.springframework.ai.chat.messages.AssistantMessage");
			Class<?> systemMessageClass = Class.forName("org.springframework.ai.chat.messages.SystemMessage");

			// Create unified deserializer for all Spring AI Message classes
			SpringAIMessageDeserializer messageDeserializer = new SpringAIMessageDeserializer();

			// Register custom deserializers for each message type
			@SuppressWarnings("unchecked")
			Class<Object> userClass = (Class<Object>) userMessageClass;
			@SuppressWarnings("unchecked")
			Class<Object> assistantClass = (Class<Object>) assistantMessageClass;
			@SuppressWarnings("unchecked")
			Class<Object> systemClass = (Class<Object>) systemMessageClass;

			springAIModule.addDeserializer(userClass, messageDeserializer);
			springAIModule.addDeserializer(assistantClass, messageDeserializer);
			springAIModule.addDeserializer(systemClass, messageDeserializer);

		}
		catch (ClassNotFoundException e) {
			// Spring AI classes not available, skip registration
		}

		mapper.registerModule(springAIModule);
	}

	/**
	 * Register custom serialization/deserialization support for problematic collection
	 * types
	 */
	private void registerCollectionModule(ObjectMapper mapper) {
		SimpleModule collectionModule = new SimpleModule("CollectionModule");

		try {
			// Handle ArrayList$SubList - convert to ArrayList during deserialization
			Class<?> subListClass = Class.forName("java.util.ArrayList$SubList");
			@SuppressWarnings("unchecked")
			Class<Object> subListObjectClass = (Class<Object>) subListClass;
			collectionModule.addDeserializer(subListObjectClass, new SubListDeserializer());
		}
		catch (ClassNotFoundException e) {
			// SubList class not available in some JVM implementations
		}

		try {
			// Handle Arrays$ArrayList - convert to ArrayList during deserialization
			Class<?> arraysListClass = Class.forName("java.util.Arrays$ArrayList");
			@SuppressWarnings("unchecked")
			Class<Object> arraysListObjectClass = (Class<Object>) arraysListClass;
			collectionModule.addDeserializer(arraysListObjectClass, new ArraysListDeserializer());
		}
		catch (ClassNotFoundException e) {
			// Arrays$ArrayList class not available
		}

		mapper.registerModule(collectionModule);
	}

	/**
	 * Register fallback module for automatic HashMap conversion when deserialization
	 * fails
	 */
	private void registerFallbackModule(ObjectMapper mapper) {
		SimpleModule fallbackModule = new SimpleModule("FallbackModule");

		// Add a default deserializer for Object.class to handle fallback cases
		fallbackModule.addDeserializer(Object.class, new FallbackObjectDeserializer());

		mapper.registerModule(fallbackModule);
	}

	@Override
	public String contentType() {
		return "application/json";
	}

	@Override
	public void write(OverAllState object, ObjectOutput out) throws IOException {
		String json = objectMapper.writeValueAsString(object);
		byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
		out.writeInt(jsonBytes.length);
		out.write(jsonBytes);
	}

	@Override
	public OverAllState read(ObjectInput in) throws IOException, ClassNotFoundException {
		int length = in.readInt();
		byte[] jsonBytes = new byte[length];
		in.readFully(jsonBytes);
		String json = new String(jsonBytes, StandardCharsets.UTF_8);
		return objectMapper.readValue(json, getStateType());
	}

	/**
	 * Unified deserializer for all Spring AI Message types
	 */
	public static class SpringAIMessageDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<Object> {

		// Factory map for creating different message types
		private static final java.util.Map<String, java.util.function.Function<String, Object>> MESSAGE_FACTORIES = new java.util.HashMap<>();

		static {
			try {
				// Initialize message factories using reflection
				Class<?> userMessageClass = Class.forName("org.springframework.ai.chat.messages.UserMessage");
				Class<?> assistantMessageClass = Class.forName("org.springframework.ai.chat.messages.AssistantMessage");
				Class<?> systemMessageClass = Class.forName("org.springframework.ai.chat.messages.SystemMessage");

				MESSAGE_FACTORIES.put("USER", content -> createMessage(userMessageClass, content));
				MESSAGE_FACTORIES.put("ASSISTANT", content -> createMessage(assistantMessageClass, content));
				MESSAGE_FACTORIES.put("SYSTEM", content -> createMessage(systemMessageClass, content));

				// Additional aliases
				MESSAGE_FACTORIES.put("USERMESSAGE", content -> createMessage(userMessageClass, content));
				MESSAGE_FACTORIES.put("ASSISTANTMESSAGE", content -> createMessage(assistantMessageClass, content));
				MESSAGE_FACTORIES.put("SYSTEMMESSAGE", content -> createMessage(systemMessageClass, content));

			}
			catch (ClassNotFoundException e) {
				// Spring AI classes not available
			}
		}

		private static Object createMessage(Class<?> messageClass, String content) {
			try {
				return messageClass.getConstructor(String.class).newInstance(content != null ? content : "");
			}
			catch (Exception e) {
				throw new RuntimeException("Failed to create message instance for class: " + messageClass.getName(), e);
			}
		}

		@Override
		public Object deserialize(com.fasterxml.jackson.core.JsonParser p,
				com.fasterxml.jackson.databind.DeserializationContext ctxt) throws java.io.IOException {
			try {
				com.fasterxml.jackson.databind.JsonNode node = p.getCodec().readTree(p);

				// Extract message type - support multiple field names
				String messageType = extractMessageType(node);

				// Extract content - support multiple field names
				String content = extractContent(node);

				// Create corresponding message object based on type
				return java.util.Optional.ofNullable(messageType)
					.map(String::toUpperCase)
					.map(MESSAGE_FACTORIES::get)
					.orElseGet(() -> {
						// Default to USER message if type is unknown
						return MESSAGE_FACTORIES.get("USER");
					})
					.apply(content);

			}
			catch (Exception e) {
				// Fallback: create a UserMessage with empty content
				try {
					Class<?> userMessageClass = Class.forName("org.springframework.ai.chat.messages.UserMessage");
					return createMessage(userMessageClass, "");
				}
				catch (Exception ex) {
					throw new com.fasterxml.jackson.databind.JsonMappingException(p,
							"Cannot deserialize Spring AI Message", ex);
				}
			}
		}

		/**
		 * Extract message type from JsonNode - supports multiple field names
		 */
		private String extractMessageType(com.fasterxml.jackson.databind.JsonNode node) {
			// Try different field names for message type
			return java.util.Optional.ofNullable(node.get("messageType"))
				.map(com.fasterxml.jackson.databind.JsonNode::asText)
				.orElseGet(() -> java.util.Optional.ofNullable(node.get("type"))
					.map(com.fasterxml.jackson.databind.JsonNode::asText)
					.orElseGet(() -> java.util.Optional.ofNullable(node.get("role"))
						.map(n -> n.asText().toUpperCase())
						.orElseGet(() -> {
							// Try to infer from class type information
							return java.util.Optional.ofNullable(node.get("@class"))
								.map(com.fasterxml.jackson.databind.JsonNode::asText)
								.map(this::extractTypeFromClassName)
								.orElse(null);
						})));
		}

		/**
		 * Extract message content from JsonNode - supports multiple field names
		 */
		private String extractContent(com.fasterxml.jackson.databind.JsonNode node) {
			// Try different field names for content
			return java.util.Optional.ofNullable(node.get("content"))
				.map(com.fasterxml.jackson.databind.JsonNode::asText)
				.orElseGet(() -> java.util.Optional.ofNullable(node.get("text"))
					.map(com.fasterxml.jackson.databind.JsonNode::asText)
					.orElseGet(() -> java.util.Optional.ofNullable(node.get("message"))
						.map(com.fasterxml.jackson.databind.JsonNode::asText)
						.orElseGet(() -> {
							// If node is plain text, use it directly
							if (node.isTextual()) {
								return node.asText();
							}
							return ""; // fallback to empty string
						})));
		}

		/**
		 * Extract message type from class name
		 */
		private String extractTypeFromClassName(String className) {
			if (className == null)
				return null;

			String simpleName = className.substring(className.lastIndexOf('.') + 1);
			if (simpleName.endsWith("Message")) {
				return simpleName.substring(0, simpleName.length() - 7).toUpperCase();
			}
			return simpleName.toUpperCase();
		}

	}

	/**
	 * Custom deserializer for ArrayList$SubList - converts to regular ArrayList
	 */
	public static class SubListDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<Object> {

		@Override
		public Object deserialize(com.fasterxml.jackson.core.JsonParser p,
				com.fasterxml.jackson.databind.DeserializationContext ctxt) {
			try {
				// Read as JSON array and convert to ArrayList
				com.fasterxml.jackson.databind.JsonNode node = p.getCodec().readTree(p);
				if (node.isArray()) {
					java.util.List<Object> list = new java.util.ArrayList<>();
					for (com.fasterxml.jackson.databind.JsonNode element : node) {
						if (element.isTextual()) {
							list.add(element.asText());
						}
						else if (element.isNumber()) {
							list.add(element.numberValue());
						}
						else if (element.isBoolean()) {
							list.add(element.asBoolean());
						}
						else if (element.isNull()) {
							list.add(null);
						}
						else {
							// For complex objects, deserialize recursively
							list.add(ctxt.readTreeAsValue(element, Object.class));
						}
					}
					return list;
				}
				return new java.util.ArrayList<>();
			}
			catch (Exception e) {
				// Fallback to empty ArrayList
				return new java.util.ArrayList<>();
			}
		}

	}

	/**
	 * Custom deserializer for Arrays$ArrayList - converts to regular ArrayList
	 */
	public static class ArraysListDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<Object> {

		@Override
		public Object deserialize(com.fasterxml.jackson.core.JsonParser p,
				com.fasterxml.jackson.databind.DeserializationContext ctxt) throws java.io.IOException {
			try {
				// Read as JSON array and convert to ArrayList
				com.fasterxml.jackson.databind.JsonNode node = p.getCodec().readTree(p);
				if (node.isArray()) {
					java.util.List<Object> list = new java.util.ArrayList<>();
					for (com.fasterxml.jackson.databind.JsonNode element : node) {
						if (element.isTextual()) {
							list.add(element.asText());
						}
						else if (element.isNumber()) {
							list.add(element.numberValue());
						}
						else if (element.isBoolean()) {
							list.add(element.asBoolean());
						}
						else if (element.isNull()) {
							list.add(null);
						}
						else {
							// For complex objects, deserialize recursively
							list.add(ctxt.readTreeAsValue(element, Object.class));
						}
					}
					return list;
				}
				return new java.util.ArrayList<>();
			}
			catch (Exception e) {
				// Fallback to empty ArrayList
				return new java.util.ArrayList<>();
			}
		}

	}

	/**
	 * Custom problem handler that automatically falls back to HashMap for failed
	 * deserializations
	 */
	public static class FallbackDeserializationProblemHandler
			extends com.fasterxml.jackson.databind.deser.DeserializationProblemHandler {

		@Override
		public Object handleInstantiationProblem(com.fasterxml.jackson.databind.DeserializationContext ctxt,
				Class<?> instClass, Object argument, Throwable t) {

			// If instantiation fails and it's a complex object, fallback to HashMap
			if (shouldFallbackToHashMap(instClass)) {
				// Return empty HashMap as fallback - the actual data will be handled by
				// other mechanisms
				return new java.util.HashMap<>();
			}

			return com.fasterxml.jackson.databind.deser.DeserializationProblemHandler.NOT_HANDLED;
		}

		/**
		 * Determines if a class should fallback to HashMap when deserialization fails
		 */
		private boolean shouldFallbackToHashMap(Class<?> clazz) {
			if (clazz == null)
				return false;

			// Don't fallback for primitive types and their wrappers
			if (clazz.isPrimitive() || clazz == String.class || Number.class.isAssignableFrom(clazz)
					|| Boolean.class == clazz || Character.class == clazz) {
				return false;
			}

			// Don't fallback for standard collections (they have their own handling)
			if (java.util.Collection.class.isAssignableFrom(clazz) || java.util.Map.class.isAssignableFrom(clazz)) {
				return false;
			}

			// Don't fallback for arrays
			if (clazz.isArray()) {
				return false;
			}

			// Fallback for complex objects, inner classes, etc.
			return true;
		}

		/**
		 * Converts current JSON to HashMap
		 */
		private Object convertToHashMap(com.fasterxml.jackson.core.JsonParser p,
				com.fasterxml.jackson.databind.DeserializationContext ctxt) throws java.io.IOException {
			try {
				com.fasterxml.jackson.databind.JsonNode node = p.readValueAsTree();
				return convertJsonNodeToHashMap(node, ctxt);
			}
			catch (Exception e) {
				// Final fallback
				return new java.util.HashMap<>();
			}
		}

		/**
		 * Recursively converts JsonNode to HashMap/ArrayList structure
		 */
		private Object convertJsonNodeToHashMap(com.fasterxml.jackson.databind.JsonNode node,
				com.fasterxml.jackson.databind.DeserializationContext ctxt) {
			if (node == null || node.isNull()) {
				return null;
			}
			else if (node.isObject()) {
				java.util.Map<String, Object> map = new java.util.HashMap<>();
				node.fields().forEachRemaining(entry -> {
					map.put(entry.getKey(), convertJsonNodeToHashMap(entry.getValue(), ctxt));
				});
				return map;
			}
			else if (node.isArray()) {
				java.util.List<Object> list = new java.util.ArrayList<>();
				for (com.fasterxml.jackson.databind.JsonNode element : node) {
					list.add(convertJsonNodeToHashMap(element, ctxt));
				}
				return list;
			}
			else if (node.isTextual()) {
				return node.asText();
			}
			else if (node.isNumber()) {
				return node.numberValue();
			}
			else if (node.isBoolean()) {
				return node.asBoolean();
			}
			else {
				return node.asText(); // fallback to string
			}
		}

	}

	/**
	 * Fallback deserializer for Object.class that tries standard deserialization first,
	 * then falls back to HashMap if that fails
	 */
	public static class FallbackObjectDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<Object> {

		@Override
		public Object deserialize(com.fasterxml.jackson.core.JsonParser p,
				com.fasterxml.jackson.databind.DeserializationContext ctxt) throws java.io.IOException {

			try {
				// Try to read the value as tree first
				com.fasterxml.jackson.databind.JsonNode node = p.readValueAsTree();

				// Check if there's type information
				if (node.isObject() && node.has("@class")) {
					String className = node.get("@class").asText();
					try {
						Class<?> targetClass = Class.forName(className);
						// Try to deserialize to the target class
						return ctxt.readTreeAsValue(node, targetClass);
					}
					catch (Exception e) {
						// If specific class deserialization fails, fallback to HashMap
						return convertToHashMapStructure(node);
					}
				}
				else {
					// No type information, convert to appropriate basic type
					return convertToHashMapStructure(node);
				}

			}
			catch (Exception e) {
				// Final fallback
				return new java.util.HashMap<>();
			}
		}

		/**
		 * Converts JsonNode to HashMap/ArrayList/primitive structure
		 */
		private Object convertToHashMapStructure(com.fasterxml.jackson.databind.JsonNode node) {
			if (node == null || node.isNull()) {
				return null;
			}
			else if (node.isObject()) {
				java.util.Map<String, Object> map = new java.util.HashMap<>();
				node.fields().forEachRemaining(entry -> {
					map.put(entry.getKey(), convertToHashMapStructure(entry.getValue()));
				});
				return map;
			}
			else if (node.isArray()) {
				java.util.List<Object> list = new java.util.ArrayList<>();
				for (com.fasterxml.jackson.databind.JsonNode element : node) {
					list.add(convertToHashMapStructure(element));
				}
				return list;
			}
			else if (node.isTextual()) {
				return node.asText();
			}
			else if (node.isNumber()) {
				return node.numberValue();
			}
			else if (node.isBoolean()) {
				return node.asBoolean();
			}
			else {
				return node.asText();
			}
		}

	}

}
