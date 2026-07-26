/*
 * Copyright 2024-2026 the original author or authors.
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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.IndexedListSerializer;
import com.fasterxml.jackson.databind.ser.impl.IndexedStringListSerializer;
import com.fasterxml.jackson.databind.ser.impl.StringArraySerializer;
import com.fasterxml.jackson.databind.ser.impl.StringCollectionSerializer;
import com.fasterxml.jackson.databind.ser.std.AsArraySerializerBase;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;
import com.fasterxml.jackson.databind.ser.std.CollectionSerializer;
import com.fasterxml.jackson.databind.ser.std.MapSerializer;
import com.fasterxml.jackson.databind.ser.std.ObjectArraySerializer;
import com.fasterxml.jackson.databind.util.TokenBuffer;

class SerializationHelper {

	static final String METADATA_FIELD = "metadata";

	static Map<String, Object> deserializeMetadata(ObjectMapper mapper, JsonNode parentNode)
			throws JsonProcessingException {
		if (parentNode == null) {
			return Map.of();
		}

		var node = parentNode.findValue(METADATA_FIELD);

		if (node == null || node.isNull() || node.isEmpty()) {
			return Map.of();
		}
		if (!node.isObject()) {
			throw new IllegalStateException("Metadata must be an object");
		}
		return mapper.treeToValue(node, new TypeReference<>() {
		});
	}

	static void serializeMetadata(JsonGenerator gen, SerializerProvider provider, Map<String, Object> metadata)
			throws IOException {
		gen.writeObjectField(METADATA_FIELD, normalizeMetadataValue(provider, metadata));
	}

	private static Object normalizeMetadataValue(SerializerProvider provider, Object value) throws IOException {
		return normalizeMetadataValue(provider, value, JsonInclude.Include.ALWAYS);
	}

	private static Object normalizeMetadataValue(SerializerProvider provider, Object value,
			JsonInclude.Include contentInclusion) throws IOException {
		if (value instanceof Map<?, ?> map) {
			if (hasClassSerializationOverrides(provider, map.getClass())
					|| !isStandardMapSerializer(provider.findValueSerializer(map.getClass()))) {
				return map;
			}
			Map<Object, Object> normalized = new LinkedHashMap<>(map.size());
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				if (shouldInclude(provider, contentInclusion, entry.getValue())) {
					normalized.put(entry.getKey(), normalizeMetadataValue(provider, entry.getValue()));
				}
			}
			return normalized;
		}
		if (value instanceof Collection<?> collection) {
			if (hasClassSerializationOverrides(provider, collection.getClass())
					|| !isStandardCollectionSerializer(provider.findValueSerializer(collection.getClass()))) {
				return collection;
			}
			List<Object> normalized = new ArrayList<>(collection.size());
			for (Object item : collection) {
				normalized.add(normalizeMetadataValue(provider, item));
			}
			return normalized;
		}
		if (value != null && value.getClass().isArray() && !value.getClass().getComponentType().isPrimitive()) {
			if (hasClassSerializationOverrides(provider, value.getClass())
					|| !isStandardObjectArraySerializer(provider.findValueSerializer(value.getClass()))) {
				return value;
			}
			int length = Array.getLength(value);
			Class<?> componentType = value.getClass().getComponentType();
			Object normalizedArray = Array.newInstance(componentType, length);
			List<Object> normalizedList = null;
			for (int i = 0; i < length; i++) {
				Object item = Array.get(value, i);
				Object normalizedItem = normalizeMetadataValue(provider, item);
				if (normalizedList == null && (normalizedItem == null || componentType.isInstance(normalizedItem))) {
					Array.set(normalizedArray, i, normalizedItem);
				}
				else {
					if (normalizedList == null) {
						normalizedList = new ArrayList<>(length);
						for (int j = 0; j < i; j++) {
							normalizedList.add(Array.get(normalizedArray, j));
						}
					}
					normalizedList.add(normalizedItem);
				}
			}
			return normalizedList != null ? normalizedList : normalizedArray;
		}
		if (value instanceof Record record) {
			if (!(provider.findValueSerializer(record.getClass()) instanceof BeanSerializerBase beanSerializer)) {
				return record;
			}
			List<BeanPropertyDefinition> properties = provider.getConfig()
				.introspect(provider.constructType(record.getClass()))
				.findProperties();
			if (hasClassSerializationOverrides(provider, record.getClass())
					|| hasCustomPropertySerializer(provider, beanSerializer, properties)
					|| !requiresRecordNormalization(provider, record, properties)) {
				return record;
			}
			Map<String, Object> normalized = new LinkedHashMap<>();
			JsonInclude.Value recordInclusion = getRecordInclusion(provider, record.getClass());
			Map<String, BeanPropertyWriter> propertyWriters = getAssignedPropertyWriters(beanSerializer);
			for (BeanPropertyDefinition property : properties) {
				if (!property.couldSerialize()) {
					continue;
				}
				AnnotatedMember accessor = property.getAccessor();
				if (accessor == null) {
					continue;
				}
				try {
					accessor.fixAccess(provider.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
					Object propertyValue = accessor.getValue(record);
					if (shouldInclude(provider, recordInclusion.getValueInclusion(), propertyValue)) {
						BeanPropertyWriter propertyWriter = propertyWriters.get(property.getName());
						boolean hasAssignedSerializer = propertyWriter != null
								&& (propertyValue == null
										? propertyWriter.hasNullSerializer()
										: propertyWriter.hasSerializer());
						if (hasAssignedSerializer) {
							SerializedProperty serialized = applyPropertyWriter(provider, propertyWriter, record);
							if (serialized.present()) {
								normalized.put(property.getName(), serialized.value());
							}
						}
						else {
							normalized.put(property.getName(),
									normalizeMetadataValue(provider, propertyValue,
											recordInclusion.getContentInclusion()));
						}
					}
				}
				catch (IllegalArgumentException ex) {
					throw new IOException("Failed to serialize metadata record " + record.getClass().getName(), ex);
				}
			}
			return normalized;
		}
		return value;
	}

	private static boolean requiresRecordNormalization(SerializerProvider provider, Record record,
			List<BeanPropertyDefinition> properties) throws IOException {
		for (BeanPropertyDefinition property : properties) {
			AnnotatedMember accessor = property.getAccessor();
			if (accessor == null) {
				continue;
			}
			try {
				accessor.fixAccess(provider.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
				Object propertyValue = accessor.getValue(record);
				if (hasIncompatibleContainerValue(property.getPrimaryType(), propertyValue)) {
					return true;
				}
			}
			catch (IllegalArgumentException ex) {
				throw new IOException("Failed to inspect metadata record " + record.getClass().getName(), ex);
			}
		}
		return false;
	}

	private static boolean hasIncompatibleContainerValue(com.fasterxml.jackson.databind.JavaType declaredType,
			Object value) {
		if (declaredType.isCollectionLikeType() && value instanceof Collection<?> collection) {
			return hasIncompatibleValue(declaredType.getContentType().getRawClass(), collection);
		}
		if (declaredType.isMapLikeType() && value instanceof Map<?, ?> map) {
			return hasIncompatibleValue(declaredType.getKeyType().getRawClass(), map.keySet())
					|| hasIncompatibleValue(declaredType.getContentType().getRawClass(), map.values());
		}
		return false;
	}

	private static boolean hasIncompatibleValue(Class<?> declaredType, Collection<?> values) {
		return declaredType != Object.class && values.stream()
			.filter(item -> item != null)
			.anyMatch(item -> !declaredType.isInstance(item));
	}

	private static boolean isStandardMapSerializer(Object serializer) {
		return serializer instanceof MapSerializer mapSerializer
				&& mapSerializer.getContentSerializer() == null;
	}

	private static boolean isStandardCollectionSerializer(Object serializer) {
		if (serializer instanceof AsArraySerializerBase<?> arraySerializer
				&& arraySerializer.getContentSerializer() != null) {
			return false;
		}
		return serializer instanceof CollectionSerializer || serializer instanceof IndexedListSerializer
				|| serializer instanceof IndexedStringListSerializer
				|| serializer instanceof StringCollectionSerializer;
	}

	private static boolean isStandardObjectArraySerializer(Object serializer) {
		return (serializer instanceof ObjectArraySerializer arraySerializer
				&& arraySerializer.getContentSerializer() == null)
				|| serializer instanceof StringArraySerializer;
	}

	private static boolean hasClassSerializationOverrides(SerializerProvider provider, Class<?> valueClass) {
		var introspector = provider.getAnnotationIntrospector();
		var classInfo = provider.getConfig()
			.introspectClassAnnotations(provider.constructType(valueClass))
			.getClassInfo();
		JsonInclude.Value globalInclusion = provider.getConfig().getDefaultPropertyInclusion();
		JsonInclude.Value defaultInclusion = provider.getDefaultPropertyInclusion(valueClass);
		JsonInclude.Value effectiveInclusion = provider.getConfig()
			.introspect(provider.constructType(valueClass))
			.findPropertyInclusion(defaultInclusion);
		boolean unsupportedInclusion = valueClass.isRecord()
				? hasUnsupportedRecordInclusion(effectiveInclusion)
				: !defaultInclusion.equals(globalInclusion) || !effectiveInclusion.equals(defaultInclusion);
		return unsupportedInclusion
				|| !JsonFormat.Value.empty().equals(provider.getConfig().getDefaultPropertyFormat(valueClass))
				|| provider.getConfig().findMixInClassFor(valueClass) != null
				|| hasNonStructuralJacksonAnnotation(List.of(valueClass.getAnnotations()))
				|| introspector.findSerializer(classInfo) != null
				|| introspector.findKeySerializer(classInfo) != null
				|| introspector.findContentSerializer(classInfo) != null
				|| introspector.findNullSerializer(classInfo) != null
				|| introspector.findSerializationConverter(classInfo) != null
				|| introspector.findFilterId(classInfo) != null;
	}

	private static JsonInclude.Value getRecordInclusion(SerializerProvider provider, Class<?> recordClass) {
		JsonInclude.Value defaultInclusion = provider.getDefaultPropertyInclusion(recordClass);
		return provider.getConfig()
			.introspect(provider.constructType(recordClass))
			.findPropertyInclusion(defaultInclusion);
	}

	private static boolean hasUnsupportedRecordInclusion(JsonInclude.Value inclusion) {
		JsonInclude.Include value = inclusion.getValueInclusion();
		JsonInclude.Include content = inclusion.getContentInclusion();
		return value == JsonInclude.Include.NON_DEFAULT || value == JsonInclude.Include.CUSTOM
				|| content != JsonInclude.Include.ALWAYS && content != JsonInclude.Include.USE_DEFAULTS
						&& content != JsonInclude.Include.NON_NULL && content != JsonInclude.Include.NON_EMPTY
						&& content != JsonInclude.Include.NON_ABSENT;
	}

	private static boolean shouldInclude(SerializerProvider provider, JsonInclude.Include inclusion, Object value)
			throws IOException {
		return switch (inclusion) {
			case NON_NULL -> value != null;
			case NON_EMPTY -> value != null && !provider.findValueSerializer(value.getClass()).isEmpty(provider, value);
			case NON_ABSENT -> value != null
					&& (!provider.constructType(value.getClass()).isReferenceType()
							|| !provider.findValueSerializer(value.getClass()).isEmpty(provider, value));
			default -> true;
		};
	}

	private static boolean hasCustomPropertySerializer(SerializerProvider provider, BeanSerializerBase serializer,
			List<BeanPropertyDefinition> definitions) {
		var introspector = provider.getAnnotationIntrospector();
		var properties = serializer.properties();
		Set<String> effectiveNames = new HashSet<>();
		while (properties.hasNext()) {
			PropertyWriter property = properties.next();
			effectiveNames.add(property.getName());
			AnnotatedMember member = property.getMember();
			if (member != null && hasNonStructuralJacksonAnnotation(member.annotations())) {
				return true;
			}
			JsonInclude.Value inclusion = member != null ? introspector.findPropertyInclusion(member) : null;
			if (inclusion != null && !JsonInclude.Value.empty().equals(inclusion)) {
				return true;
			}
			if (member != null && (introspector.findSerializer(member) != null
					|| introspector.findKeySerializer(member) != null
					|| introspector.findContentSerializer(member) != null
					|| introspector.findNullSerializer(member) != null
					|| introspector.findSerializationConverter(member) != null
					|| introspector.findSerializationContentConverter(member) != null)) {
				return true;
			}
		}
		Set<String> declaredNames = new HashSet<>();
		for (BeanPropertyDefinition definition : definitions) {
			if (definition.couldSerialize()) {
				declaredNames.add(definition.getName());
			}
		}
		return !effectiveNames.equals(declaredNames);
	}

	private static Map<String, BeanPropertyWriter> getAssignedPropertyWriters(BeanSerializerBase serializer) {
		Map<String, BeanPropertyWriter> writers = new LinkedHashMap<>();
		var properties = serializer.properties();
		while (properties.hasNext()) {
			PropertyWriter property = properties.next();
			if (property instanceof BeanPropertyWriter beanProperty
					&& (beanProperty.hasSerializer() || beanProperty.hasNullSerializer())) {
				writers.put(property.getName(), beanProperty);
			}
		}
		return writers;
	}

	private static SerializedProperty applyPropertyWriter(SerializerProvider provider,
			BeanPropertyWriter writer, Record record) throws IOException {
		ObjectCodec codec = provider.getGenerator().getCodec();
		try (TokenBuffer buffer = new TokenBuffer(codec, false)) {
			buffer.writeStartObject();
			try {
				writer.serializeAsField(record, buffer, provider);
			}
			catch (Exception ex) {
				throw new IOException("Failed to apply serializer for metadata property " + writer.getName(), ex);
			}
			buffer.writeEndObject();
			try (JsonParser parser = buffer.asParser(codec)) {
				Map<?, ?> serialized = codec.readValue(parser, Map.class);
				return new SerializedProperty(serialized.containsKey(writer.getName()),
						serialized.get(writer.getName()));
			}
		}
	}

	private record SerializedProperty(boolean present, Object value) {
	}

	private static boolean hasNonStructuralJacksonAnnotation(Iterable<Annotation> annotations) {
		for (Annotation annotation : annotations) {
			Class<? extends Annotation> type = annotation.annotationType();
			if (type.getPackageName().startsWith("com.fasterxml.jackson")
					&& type != JsonProperty.class && type != JsonIgnore.class && type != JsonInclude.class) {
				return true;
			}
		}
		return false;
	}

}
