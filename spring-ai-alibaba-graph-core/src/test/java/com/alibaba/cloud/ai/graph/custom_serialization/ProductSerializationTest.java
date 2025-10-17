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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test custom object serialization in Graph Workflow
 *
 * @author Libres-coder
 */
class ProductSerializationTest {

	private ProductStateSerializer serializer;

	@BeforeEach
	void setUp() {
		serializer = new ProductStateSerializer(OverAllState::new);
	}

	@Test
	void testSingleProductSerialization() throws Exception {
		Product product = new Product("P001", "Laptop", 999.99, "Electronics");

		Map<String, Object> data = new HashMap<>();
		data.put("product", product);
		data.put("timestamp", System.currentTimeMillis());

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		serializer.writeData(data, oos);
		oos.flush();

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream ois = new ObjectInputStream(bais);
		Map<String, Object> deserializedData = serializer.readData(ois);

		assertNotNull(deserializedData);
		assertTrue(deserializedData.containsKey("product"));

		Object deserializedProduct = deserializedData.get("product");
		assertInstanceOf(Product.class, deserializedProduct,
				"Expected Product instance, but got " + deserializedProduct.getClass().getName());

		Product resultProduct = (Product) deserializedProduct;
		assertEquals("P001", resultProduct.getId());
		assertEquals("Laptop", resultProduct.getName());
		assertEquals(999.99, resultProduct.getPrice());
		assertEquals("Electronics", resultProduct.getCategory());
	}

	@Test
	void testProductListSerialization() throws Exception {
		Product product1 = new Product("P001", "Laptop", 999.99, "Electronics");
		Product product2 = new Product("P002", "Mouse", 29.99, "Accessories");
		Product product3 = new Product("P003", "Keyboard", 79.99, "Accessories");

		List<Product> products = List.of(product1, product2, product3);

		Map<String, Object> data = new HashMap<>();
		data.put("products", products);
		data.put("total_count", products.size());

		Map<String, Object> deserializedData = serializeAndDeserialize(data);

		assertNotNull(deserializedData);
		assertTrue(deserializedData.containsKey("products"));

		@SuppressWarnings("unchecked")
		List<Object> deserializedProducts = (List<Object>) deserializedData.get("products");
		assertEquals(3, deserializedProducts.size());

		for (int i = 0; i < deserializedProducts.size(); i++) {
			Object item = deserializedProducts.get(i);
			assertInstanceOf(Product.class, item, "List item " + i + " should be Product instance");
		}

		Product firstProduct = (Product) deserializedProducts.get(0);
		assertEquals("P001", firstProduct.getId());
		assertEquals("Laptop", firstProduct.getName());
	}

	@Test
	void testComplexStateSerialization() throws Exception {
		Product product = new Product("P001", "Laptop", 999.99, "Electronics");

		Map<String, Object> metadata = new HashMap<>();
		metadata.put("source", "web");
		metadata.put("user_id", "user123");

		Map<String, Object> data = new HashMap<>();
		data.put("product", product);
		data.put("metadata", metadata);
		data.put("score", 0.95);
		data.put("tags", List.of("featured", "bestseller", "new"));

		Map<String, Object> deserializedData = serializeAndDeserialize(data);

		assertInstanceOf(Product.class, deserializedData.get("product"));
		Product deserializedProduct = (Product) deserializedData.get("product");
		assertEquals(product.getId(), deserializedProduct.getId());
		assertEquals(product.getName(), deserializedProduct.getName());

		assertEquals(0.95, deserializedData.get("score"));

		@SuppressWarnings("unchecked")
		Map<String, Object> deserializedMetadata = (Map<String, Object>) deserializedData.get("metadata");
		assertEquals("web", deserializedMetadata.get("source"));
		assertEquals("user123", deserializedMetadata.get("user_id"));
	}

	@Test
	void testProductWithNullValues() throws Exception {
		Product product = new Product("P001", "Laptop", null, null);

		Map<String, Object> data = new HashMap<>();
		data.put("product", product);

		Map<String, Object> deserializedData = serializeAndDeserialize(data);

		assertInstanceOf(Product.class, deserializedData.get("product"));
		Product deserializedProduct = (Product) deserializedData.get("product");
		assertEquals("P001", deserializedProduct.getId());
		assertEquals("Laptop", deserializedProduct.getName());
		assertEquals(null, deserializedProduct.getPrice());
		assertEquals(null, deserializedProduct.getCategory());
	}

	private Map<String, Object> serializeAndDeserialize(Map<String, Object> data) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		serializer.writeData(data, oos);
		oos.flush();

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream ois = new ObjectInputStream(bais);
		return serializer.readData(ois);
	}

}

