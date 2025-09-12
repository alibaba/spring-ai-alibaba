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
