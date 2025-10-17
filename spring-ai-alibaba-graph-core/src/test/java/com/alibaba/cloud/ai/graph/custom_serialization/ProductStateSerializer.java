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
package com.alibaba.cloud.ai.graph.custom_serialization;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.TypeMapper;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Custom StateSerializer for Product object serialization
 *
 * @author Libres-coder
 */
public class ProductStateSerializer extends SpringAIJacksonStateSerializer {

	public ProductStateSerializer(AgentStateFactory<OverAllState> stateFactory) {
		super(stateFactory);

		SimpleModule module = new SimpleModule();
		module.addSerializer(Product.class, new ProductSerializer());
		module.addDeserializer(Product.class, new ProductDeserializer());

		objectMapper.registerModule(module);
		typeMapper.register(new TypeMapper.Reference<Product>("PRODUCT") {
		});
	}

	static class ProductSerializer extends StdSerializer<Product> {

		public ProductSerializer() {
			super(Product.class);
		}

		@Override
		public void serialize(Product product, JsonGenerator gen, SerializerProvider provider) throws IOException {
			gen.writeStartObject();
			gen.writeStringField("@type", "PRODUCT");
			gen.writeStringField("id", product.getId());
			gen.writeStringField("name", product.getName());

			if (product.getPrice() != null) {
				gen.writeNumberField("price", product.getPrice());
			}

			if (product.getCategory() != null) {
				gen.writeStringField("category", product.getCategory());
			}

			gen.writeEndObject();
		}

	}

	static class ProductDeserializer extends StdDeserializer<Product> {

		public ProductDeserializer() {
			super(Product.class);
		}

		@Override
		public Product deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
			ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
			ObjectNode node = mapper.readTree(jsonParser);

			String id = node.has("id") ? node.get("id").asText() : null;
			String name = node.has("name") ? node.get("name").asText() : null;
			Double price = node.has("price") && !node.get("price").isNull() ? node.get("price").asDouble() : null;
			String category = node.has("category") ? node.get("category").asText() : null;

			return new Product(id, name, price, category);
		}

	}

}

