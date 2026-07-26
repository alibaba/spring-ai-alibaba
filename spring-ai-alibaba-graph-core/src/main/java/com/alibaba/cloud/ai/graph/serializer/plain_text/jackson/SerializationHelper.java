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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
import com.fasterxml.jackson.databind.JavaType;
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
import com.fasterxml.jackson.databind.util.BeanUtil;
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
			boolean preserveContainer = hasClassSerializationOverrides(provider, map.getClass());
			Object serializer = provider.findValueSerializer(map.getClass());
			if (!isStandardMapSerializer(serializer)
					|| preserveContainer && hasAssignedMapContentSerializer(serializer)) {
				return map;
			}
			boolean incompatible = hasIncompatibleContainerValue(
					provider.constructType(map.getClass()), map);
			Map<Object, Object> normalized = new LinkedHashMap<>(map.size());
			boolean changed = false;
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				if (shouldInclude(provider, contentInclusion, entry.getValue())) {
					Object normalizedValue = normalizeMetadataValue(provider, entry.getValue());
					normalized.put(entry.getKey(), normalizedValue);
					changed |= normalizedValue != entry.getValue();
				}
				else {
					changed = true;
				}
			}
			return preserveContainer && !incompatible
					? preserveMapType(provider, map, normalized, changed)
					: normalized;
		}
		if (value instanceof Collection<?> collection) {
			boolean preserveContainer = hasClassSerializationOverrides(provider, collection.getClass());
			Object serializer = provider.findValueSerializer(collection.getClass());
			if (!isStandardCollectionSerializer(serializer)
					|| preserveContainer && hasAssignedCollectionContentSerializer(serializer)) {
				return collection;
			}
			boolean incompatible = hasIncompatibleContainerValue(
					provider.constructType(collection.getClass()), collection);
			List<Object> normalized = new ArrayList<>(collection.size());
			boolean changed = false;
			for (Object item : collection) {
				Object normalizedItem = normalizeMetadataValue(provider, item);
				normalized.add(normalizedItem);
				changed |= normalizedItem != item;
			}
			return preserveContainer && !incompatible
					? preserveCollectionType(provider, collection, normalized, changed)
					: normalized;
		}
		if (value != null && value.getClass().isArray() && !value.getClass().getComponentType().isPrimitive()) {
			if (!isStandardObjectArraySerializer(provider.findValueSerializer(value.getClass()))) {
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
					|| !requiresRecordNormalization(provider, record, properties)) {
				return record;
			}
			Map<String, Object> normalized = new LinkedHashMap<>();
			JsonInclude.Value recordInclusion = getRecordInclusion(provider, record.getClass());
			List<BeanPropertyWriter> propertyWriters = getEffectivePropertyWriters(beanSerializer);
			List<BeanPropertyWriter> remainingWriters = new ArrayList<>(propertyWriters);
			for (BeanPropertyDefinition property : properties) {
				if (!property.couldSerialize()) {
					continue;
				}
				AnnotatedMember accessor = property.getAccessor();
				if (accessor == null) {
					continue;
				}
				BeanPropertyWriter propertyWriter = findEffectivePropertyWriter(propertyWriters, property);
				if (propertyWriter == null) {
					continue;
				}
				remainingWriters.remove(propertyWriter);
				try {
					accessor.fixAccess(provider.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
					Object propertyValue = accessor.getValue(record);
					JsonInclude.Value propertyInclusion =
							getPropertyInclusion(provider, accessor, recordInclusion);
					if (shouldInclude(provider, propertyInclusion, property, propertyValue)) {
						boolean hasAssignedSerializer = propertyWriter.isUnwrapping()
								|| !propertyWriter.getName().equals(property.getName())
								|| propertyWriter.getTypeSerializer() != null
								|| (propertyValue == null
										? propertyWriter.hasNullSerializer()
										: propertyWriter.hasSerializer());
						if (hasAssignedSerializer) {
							normalized.putAll(applyPropertyWriter(provider, propertyWriter, record));
						}
						else {
							normalized.put(property.getName(),
									normalizeMetadataValue(provider, propertyValue,
											propertyInclusion.getContentInclusion()));
						}
					}
				}
				catch (IllegalArgumentException ex) {
					throw new IOException("Failed to serialize metadata record " + record.getClass().getName(), ex);
				}
			}
			for (BeanPropertyWriter propertyWriter : remainingWriters) {
				normalized.putAll(applyPropertyWriter(provider, propertyWriter, record));
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

	private static boolean hasIncompatibleContainerValue(JavaType declaredType, Object value) {
		if (declaredType.isCollectionLikeType() && value instanceof Collection<?> collection) {
			return hasIncompatibleValue(declaredType.getContentType(), collection);
		}
		if (declaredType.isMapLikeType() && value instanceof Map<?, ?> map) {
			return hasIncompatibleValue(declaredType.getKeyType(), map.keySet())
					|| hasIncompatibleValue(declaredType.getContentType(), map.values());
		}
		return false;
	}

	private static boolean hasIncompatibleValue(JavaType declaredType, Collection<?> values) {
		if (declaredType == null || declaredType.getRawClass() == Object.class) {
			return false;
		}
		return values.stream()
			.filter(item -> item != null)
			.anyMatch(item -> !declaredType.getRawClass().isInstance(item)
					|| hasIncompatibleContainerValue(declaredType, item));
	}

	private static boolean isStandardMapSerializer(Object serializer) {
		return serializer instanceof MapSerializer;
	}

	private static boolean hasAssignedMapContentSerializer(Object serializer) {
		return serializer instanceof MapSerializer mapSerializer
				&& mapSerializer.getContentSerializer() != null;
	}

	private static boolean isStandardCollectionSerializer(Object serializer) {
		return serializer instanceof CollectionSerializer || serializer instanceof IndexedListSerializer
				|| serializer instanceof IndexedStringListSerializer
				|| serializer instanceof StringCollectionSerializer;
	}

	private static boolean hasAssignedCollectionContentSerializer(Object serializer) {
		return serializer instanceof AsArraySerializerBase<?> arraySerializer
				&& arraySerializer.getContentSerializer() != null;
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
				|| hasContainerConfigOverride(provider, valueClass)
				|| !JsonFormat.Value.empty().equals(provider.getConfig().getDefaultPropertyFormat(valueClass))
				|| !valueClass.isRecord() && provider.getConfig().findMixInClassFor(valueClass) != null
				|| hasNonStructuralJacksonAnnotation(List.of(valueClass.getAnnotations()))
				|| introspector.findSerializer(classInfo) != null
				|| introspector.findKeySerializer(classInfo) != null
				|| introspector.findContentSerializer(classInfo) != null
				|| introspector.findNullSerializer(classInfo) != null
				|| introspector.findSerializationConverter(classInfo) != null
				|| introspector.findFilterId(classInfo) != null;
	}

	private static boolean hasContainerConfigOverride(SerializerProvider provider, Class<?> valueClass) {
		if (valueClass.isRecord()) {
			return false;
		}
		var override = provider.getConfig().findConfigOverride(valueClass);
		return override != null
				&& (override.getFormat() != null || override.getInclude() != null
						|| override.getIncludeAsProperty() != null || override.getIgnorals() != null
						|| override.getIsIgnoredType() != null || override.getVisibility() != null);
	}

	private static JsonInclude.Value getRecordInclusion(SerializerProvider provider, Class<?> recordClass) {
		JsonInclude.Value defaultInclusion = provider.getDefaultPropertyInclusion(recordClass);
		return provider.getConfig()
			.introspect(provider.constructType(recordClass))
			.findPropertyInclusion(defaultInclusion);
	}

	private static JsonInclude.Value getPropertyInclusion(SerializerProvider provider,
			AnnotatedMember accessor, JsonInclude.Value recordInclusion) {
		JsonInclude.Value propertyInclusion =
				provider.getAnnotationIntrospector().findPropertyInclusion(accessor);
		return propertyInclusion != null ? recordInclusion.withOverrides(propertyInclusion) : recordInclusion;
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

	private static boolean shouldInclude(SerializerProvider provider, JsonInclude.Value inclusion,
			BeanPropertyDefinition property, Object value) throws IOException {
		return switch (inclusion.getValueInclusion()) {
			case NON_DEFAULT -> {
				Object defaultValue = property.getMetadata().getDefaultValue();
				if (defaultValue == null) {
					defaultValue = BeanUtil.getDefaultValue(property.getPrimaryType());
				}
				yield !Objects.deepEquals(value, defaultValue);
			}
			case CUSTOM -> {
				Class<?> filterClass = inclusion.getValueFilter();
				if (filterClass == null) {
					yield true;
				}
				Object filter = provider.includeFilterInstance(property, filterClass);
				yield value == null
						? !provider.includeFilterSuppressNulls(filter)
						: !filter.equals(value);
			}
			default -> shouldInclude(provider, inclusion.getValueInclusion(), value);
		};
	}

	private static List<BeanPropertyWriter> getEffectivePropertyWriters(BeanSerializerBase serializer) {
		List<BeanPropertyWriter> writers = new ArrayList<>();
		var properties = serializer.properties();
		while (properties.hasNext()) {
			PropertyWriter property = properties.next();
			if (property instanceof BeanPropertyWriter beanProperty) {
				writers.add(beanProperty);
			}
		}
		return writers;
	}

	private static BeanPropertyWriter findEffectivePropertyWriter(List<BeanPropertyWriter> writers,
			BeanPropertyDefinition property) {
		AnnotatedMember accessor = property.getAccessor();
		for (BeanPropertyWriter writer : writers) {
			if (accessor != null && writer.getMember() != null
					&& accessor.getMember().equals(writer.getMember().getMember())) {
				return writer;
			}
			if (writer.getName().equals(property.getName())) {
				return writer;
			}
		}
		return null;
	}

	private static Map<String, Object> applyPropertyWriter(SerializerProvider provider,
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
				return codec.readValue(parser, new TypeReference<>() {
				});
			}
		}
	}

	private static Object preserveMapType(SerializerProvider provider, Map<?, ?> original,
			Map<Object, Object> normalized, boolean changed) {
		if (!changed) {
			return original;
		}
		Map<Object, Object> copy = instantiateContainer(provider, original.getClass(), Map.class);
		if (copy == null) {
			return normalized;
		}
		copy.putAll(normalized);
		return copy;
	}

	private static Object preserveCollectionType(SerializerProvider provider, Collection<?> original,
			List<Object> normalized, boolean changed) {
		if (!changed) {
			return original;
		}
		Collection<Object> copy = instantiateContainer(provider, original.getClass(), Collection.class);
		if (copy == null) {
			return normalized;
		}
		copy.addAll(normalized);
		return copy;
	}

	@SuppressWarnings("unchecked")
	private static <T> T instantiateContainer(SerializerProvider provider, Class<?> containerClass,
			Class<?> expectedType) {
		try {
			var constructor = containerClass.getDeclaredConstructor();
			if (provider.isEnabled(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS)) {
				constructor.setAccessible(true);
			}
			Object instance = constructor.newInstance();
			return expectedType.isInstance(instance) ? (T) instance : null;
		}
		catch (ReflectiveOperationException | RuntimeException ex) {
			return null;
		}
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
