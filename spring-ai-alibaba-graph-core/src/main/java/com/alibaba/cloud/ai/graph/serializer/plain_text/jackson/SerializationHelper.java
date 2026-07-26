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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.IndexedListSerializer;
import com.fasterxml.jackson.databind.ser.impl.IndexedStringListSerializer;
import com.fasterxml.jackson.databind.ser.impl.StringArraySerializer;
import com.fasterxml.jackson.databind.ser.impl.StringCollectionSerializer;
import com.fasterxml.jackson.databind.ser.std.AsArraySerializerBase;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;
import com.fasterxml.jackson.databind.ser.std.CollectionSerializer;
import com.fasterxml.jackson.databind.ser.std.MapSerializer;
import com.fasterxml.jackson.databind.ser.std.ObjectArraySerializer;

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
		if (value instanceof Map<?, ?> map) {
			if (hasClassSerializationOverrides(provider, map.getClass())
					|| !isStandardMapSerializer(provider.findValueSerializer(map.getClass()))) {
				return map;
			}
			Map<Object, Object> normalized = new LinkedHashMap<>(map.size());
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				normalized.put(entry.getKey(), normalizeMetadataValue(provider, entry.getValue()));
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
			List<Object> normalized = new ArrayList<>(length);
			for (int i = 0; i < length; i++) {
				normalized.add(normalizeMetadataValue(provider, Array.get(value, i)));
			}
			return normalized;
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
						normalized.put(property.getName(), normalizeMetadataValue(provider, propertyValue));
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
			if (property instanceof com.fasterxml.jackson.databind.ser.BeanPropertyWriter beanProperty
					&& beanProperty.getSerializer() != null
					&& !beanProperty.getSerializer().getClass().getPackageName().startsWith("com.fasterxml.jackson")) {
				return true;
			}
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
