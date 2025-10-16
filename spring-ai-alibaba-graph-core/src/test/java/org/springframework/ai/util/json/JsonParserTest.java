/*
 * Copyright 2023-2025 the original author or authors.
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

package org.springframework.ai.util.json;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JsonParser} to verify support for primitive types, wrapper classes,
 * collections, and arrays.
 */
class JsonParserTest {

	// Test Primitive Wrapper Types
	@Test
	void testFromJsonWithInteger() {
		Integer result = JsonParser.fromJson("42", Integer.class);
		assertThat(result).isEqualTo(42);
	}

	@Test
	void testFromJsonWithLong() {
		Long result = JsonParser.fromJson("9223372036854775807", Long.class);
		assertThat(result).isEqualTo(9223372036854775807L);
	}

	@Test
	void testFromJsonWithDouble() {
		Double result = JsonParser.fromJson("3.14159", Double.class);
		assertThat(result).isEqualTo(3.14159);
	}

	@Test
	void testFromJsonWithFloat() {
		Float result = JsonParser.fromJson("2.5", Float.class);
		assertThat(result).isEqualTo(2.5f);
	}

	@Test
	void testFromJsonWithBoolean() {
		Boolean result = JsonParser.fromJson("true", Boolean.class);
		assertThat(result).isTrue();
	}

	@Test
	void testFromJsonWithString() {
		String result = JsonParser.fromJson("\"Hello, World!\"", String.class);
		assertThat(result).isEqualTo("Hello, World!");
	}

	// Test String type with JSON object/array - should return JSON string as-is
	@Test
	void testFromJsonWithStringTypeButJsonObject() {
		String json = "{\"name\":\"Alice\",\"age\":30}";
		String result = JsonParser.fromJson(json, String.class);
		assertThat(result).isEqualTo(json);
	}

	@Test
	void testFromJsonWithStringTypeButJsonArray() {
		String json = "[\"apple\", \"banana\", \"cherry\"]";
		String result = JsonParser.fromJson(json, String.class);
		assertThat(result).isEqualTo(json);
	}

	// NEW: Test String type with string values containing '[' or '{' characters
	@Test
	void testFromJsonWithStringContainingBrackets() {
		// JSON string value that contains '[' and ']' characters
		String json = "\"[1, 2, 3]\"";
		String result = JsonParser.fromJson(json, String.class);
		// Should return the actual string content WITHOUT quotes: [1, 2, 3]
		assertThat(result).isEqualTo("[1, 2, 3]");
	}

	@Test
	void testFromJsonWithStringContainingBraces() {
		// JSON string value that contains '{' and '}' characters
		String json = "\"{\\\"a\\\":\\\"b\\\"}\"";
		String result = JsonParser.fromJson(json, String.class);
		// Should return the actual string content WITHOUT quotes: {"a":"b"}
		assertThat(result).isEqualTo("{\"a\":\"b\"}");
	}

	@Test
	void testFromJsonWithStringContainingMixedContent() {
		// JSON string value with mixed content
		String json = "\"This is an array: [1,2,3] and object: {a:1}\"";
		String result = JsonParser.fromJson(json, String.class);
		assertThat(result).isEqualTo("This is an array: [1,2,3] and object: {a:1}");
	}

	@Test
	void testFromJsonWithStringTypeAndTypeParameterButJsonObject() {
		String json = "{\"key\":\"value\"}";
		Type type = String.class;
		String result = JsonParser.fromJson(json, type);
		assertThat(result).isEqualTo(json);
	}

	@Test
	void testFromJsonWithStringTypeAndTypeParameterButJsonArray() {
		String json = "[1, 2, 3]";
		Type type = String.class;
		String result = JsonParser.fromJson(json, type);
		assertThat(result).isEqualTo(json);
	}

	// NEW: Test Type parameter with string containing brackets
	@Test
	void testFromJsonWithTypeParameterStringContainingBrackets() {
		String json = "\"[test]\"";
		Type type = String.class;
		String result = JsonParser.fromJson(json, type);
		assertThat(result).isEqualTo("[test]");
	}

	@Test
	void testFromJsonWithByte() {
		Byte result = JsonParser.fromJson("127", Byte.class);
		assertThat(result).isEqualTo((byte) 127);
	}

	@Test
	void testFromJsonWithShort() {
		Short result = JsonParser.fromJson("32767", Short.class);
		assertThat(result).isEqualTo((short) 32767);
	}

	// Test List
	@Test
	void testFromJsonWithListOfStrings() {
		String json = "[\"apple\", \"banana\", \"cherry\"]";
		List<String> result = JsonParser.fromJson(json, new TypeReference<List<String>>() {});
		assertThat(result).containsExactly("apple", "banana", "cherry");
	}

	@Test
	void testFromJsonWithListOfIntegers() {
		String json = "[1, 2, 3, 4, 5]";
		List<Integer> result = JsonParser.fromJson(json, new TypeReference<List<Integer>>() {});
		assertThat(result).containsExactly(1, 2, 3, 4, 5);
	}

	@Test
	void testFromJsonWithListOfObjects() {
		String json = "[{\"name\":\"Alice\",\"age\":30},{\"name\":\"Bob\",\"age\":25}]";
		List<Person> result = JsonParser.fromJson(json, new TypeReference<List<Person>>() {});
		assertThat(result).hasSize(2);
		assertThat(result.get(0).name).isEqualTo("Alice");
		assertThat(result.get(0).age).isEqualTo(30);
	}

	// Test Set
	@Test
	void testFromJsonWithSet() {
		String json = "[\"apple\", \"banana\", \"cherry\"]";
		Set<String> result = JsonParser.fromJson(json, new TypeReference<Set<String>>() {});
		assertThat(result).containsExactlyInAnyOrder("apple", "banana", "cherry");
	}

	// Test Map
	@Test
	void testFromJsonWithMap() {
		String json = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
		Map<String, String> result = JsonParser.fromJson(json, new TypeReference<Map<String, String>>() {});
		assertThat(result).containsEntry("key1", "value1").containsEntry("key2", "value2");
	}

	@Test
	void testFromJsonWithMapOfIntegerValues() {
		String json = "{\"one\":1,\"two\":2,\"three\":3}";
		Map<String, Integer> result = JsonParser.fromJson(json, new TypeReference<Map<String, Integer>>() {});
		assertThat(result).containsEntry("one", 1).containsEntry("two", 2).containsEntry("three", 3);
	}

	@Test
	void testFromJsonWithNestedMap() {
		String json = "{\"outer\":{\"inner\":\"value\"}}";
		Map<String, Map<String, String>> result = JsonParser.fromJson(json, new TypeReference<Map<String, Map<String, String>>>() {});
		assertThat(result.get("outer")).containsEntry("inner", "value");
	}

	// Test Array
	@Test
	void testFromJsonWithStringArray() {
		String json = "[\"apple\", \"banana\", \"cherry\"]";
		String[] result = JsonParser.fromJson(json, String[].class);
		assertThat(result).containsExactly("apple", "banana", "cherry");
	}

	@Test
	void testFromJsonWithIntegerArray() {
		String json = "[1, 2, 3, 4, 5]";
		Integer[] result = JsonParser.fromJson(json, Integer[].class);
		assertThat(result).containsExactly(1, 2, 3, 4, 5);
	}

	@Test
	void testFromJsonWithObjectArray() {
		String json = "[{\"name\":\"Alice\",\"age\":30},{\"name\":\"Bob\",\"age\":25}]";
		Person[] result = JsonParser.fromJson(json, Person[].class);
		assertThat(result).hasSize(2);
		assertThat(result[0].name).isEqualTo("Alice");
	}

	// Test with Type parameter
	@Test
	void testFromJsonWithTypeParameterList() {
		String json = "[\"a\", \"b\", \"c\"]";
		Type type = new TypeReference<List<String>>() {}.getType();
		List<String> result = JsonParser.fromJson(json, type);
		assertThat(result).containsExactly("a", "b", "c");
	}

	@Test
	void testFromJsonWithTypeParameterMap() {
		String json = "{\"x\":10,\"y\":20}";
		Type type = new TypeReference<Map<String, Integer>>() {}.getType();
		Map<String, Integer> result = JsonParser.fromJson(json, type);
		assertThat(result).containsEntry("x", 10).containsEntry("y", 20);
	}

	// Test Complex Nested Types
	@Test
	void testFromJsonWithListOfMaps() {
		String json = "[{\"name\":\"Alice\"},{\"name\":\"Bob\"}]";
		List<Map<String, String>> result = JsonParser.fromJson(json, new TypeReference<List<Map<String, String>>>() {});
		assertThat(result).hasSize(2);
		assertThat(result.get(0)).containsEntry("name", "Alice");
	}

	@Test
	void testFromJsonWithMapOfLists() {
		String json = "{\"fruits\":[\"apple\",\"banana\"],\"colors\":[\"red\",\"blue\"]}";
		Map<String, List<String>> result = JsonParser.fromJson(json, new TypeReference<Map<String, List<String>>>() {});
		assertThat(result.get("fruits")).containsExactly("apple", "banana");
		assertThat(result.get("colors")).containsExactly("red", "blue");
	}

	// Test toJson
	@Test
	void testToJsonWithList() {
		List<String> list = Arrays.asList("a", "b", "c");
		String json = JsonParser.toJson(list);
		assertThat(json).isEqualTo("[\"a\",\"b\",\"c\"]");
	}

	@Test
	void testToJsonWithMap() {
		Map<String, Integer> map = new HashMap<>();
		map.put("x", 10);
		map.put("y", 20);
		String json = JsonParser.toJson(map);
		assertThat(json).contains("\"x\":10").contains("\"y\":20");
	}

	@Test
	void testToJsonWithArray() {
		String[] array = {"a", "b", "c"};
		String json = JsonParser.toJson(array);
		assertThat(json).isEqualTo("[\"a\",\"b\",\"c\"]");
	}

	@Test
	void testToJsonWithPrimitiveWrapper() {
		Integer value = 42;
		String json = JsonParser.toJson(value);
		assertThat(json).isEqualTo("42");
	}

	// Helper class for testing
	public static class Person {
		public String name;
		public int age;

		public Person() {
		}

		public Person(String name, int age) {
			this.name = name;
			this.age = age;
		}
	}
}
