package com.alibaba.cloud.ai.graph.serializer.plain_text.jackson;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;

public class TypeMapper {

	public static String TYPE_PROPERTY = "@type";
	private final Set<Reference<?>> references = new HashSet<>();

	public <T> TypeMapper register(Reference<T> reference) {
		Objects.requireNonNull(reference, "reference cannot be null");
		references.add(reference);
		return this;
	}

	public <T> boolean unregister(Reference<T> reference) {
		Objects.requireNonNull(reference, "reference cannot be null");
		return references.remove(reference);
	}

	public Optional<Reference<?>> getReference(String type) {
		Objects.requireNonNull(type, "type cannot be null");
		return references.stream().filter(ref -> Objects.equals(ref.getTypeName(), type)).findFirst();
	}

	public static abstract class Reference<T> extends TypeReference<T> {

		private final String typeName;

		public Reference(String typeName) {
			super();
			this.typeName = Objects.requireNonNull(typeName, "typeName cannot be null");
		}

		public String getTypeName() {
			return typeName;
		}

	}

}
