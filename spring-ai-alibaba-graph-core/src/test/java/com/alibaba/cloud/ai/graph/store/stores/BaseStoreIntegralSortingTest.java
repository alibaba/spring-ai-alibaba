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
package com.alibaba.cloud.ai.graph.store.stores;

import com.alibaba.cloud.ai.graph.store.Store;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import com.alibaba.cloud.ai.graph.store.StoreSearchRequest;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BaseStoreIntegralSortingTest {

	private static final List<String> NAMESPACE = List.of("integral-sorting");

	@TempDir
	Path tempDirectory;

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void memoryStoreSortsIntegralTypesExactly(boolean ascending) {
		MemoryStore store = new MemoryStore();
		List<Number> values = List.of(new BigInteger("-9223372036854775809"), Long.MIN_VALUE,
				BigInteger.valueOf(Long.MIN_VALUE).add(BigInteger.ONE), (short) -2, (byte) -1, 0,
				(short) 2, (byte) 10, 2147483648L, 9007199254740992L, new BigInteger("9007199254740993"),
				Long.MAX_VALUE, new BigInteger("9223372036854775808"), BigInteger.TEN.pow(100));
		List<String> expectedKeys = new ArrayList<>();
		for (int index = values.size() - 1; index >= 0; index--) {
			store.putItem(item("value-" + index, values.get(index)));
		}
		for (int index = 0; index < values.size(); index++) {
			expectedKeys.add("value-" + index);
		}

		assertSortedKeys(store, ascending, expectedKeys, "score");
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void fileSystemStoreSortsLongValuesAfterJsonRoundTrip(boolean ascending) {
		FileSystemStore store = new FileSystemStore(tempDirectory);
		store.putItem(item("large", 2147483648L));
		store.putItem(item("small", 1L));

		assertThat(store.getItem(NAMESPACE, "small").orElseThrow().getValue().get("score"))
			.isInstanceOf(Integer.class);
		assertThat(store.getItem(NAMESPACE, "large").orElseThrow().getValue().get("score"))
			.isInstanceOf(Long.class);
		assertSortedKeys(store, ascending, List.of("small", "large"), "score");
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void fileSystemStoreSortsBeyondLongRange(boolean ascending) {
		FileSystemStore store = new FileSystemStore(tempDirectory);
		store.putItem(item("above-long", new BigInteger("9223372036854775808")));
		store.putItem(item("long-max", Long.MAX_VALUE));
		store.putItem(item("above-double-precision", new BigInteger("9007199254740993")));
		store.putItem(item("double-precision-boundary", 9007199254740992L));
		store.putItem(item("long-min", Long.MIN_VALUE));
		store.putItem(item("below-long", new BigInteger("-9223372036854775809")));

		assertThat(store.getItem(NAMESPACE, "above-long").orElseThrow().getValue().get("score"))
			.isInstanceOf(BigInteger.class);
		assertSortedKeys(store, ascending, List.of("below-long", "long-min", "double-precision-boundary",
				"above-double-precision", "long-max", "above-long"), "score");
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void equivalentIntegralValuesUseSecondarySortField(boolean ascending) {
		MemoryStore store = new MemoryStore();
		store.putItem(item("e", (byte) 1));
		store.putItem(item("d", (short) 1));
		store.putItem(item("c", 1));
		store.putItem(item("b", 1L));
		store.putItem(item("a", BigInteger.ONE));

		assertSortedKeys(store, ascending, List.of("a", "b", "c", "d", "e"), "score", "key");
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void nullAndMissingValuesKeepTheirOrdering(boolean ascending) {
		MemoryStore store = new MemoryStore();
		store.putItem(item("integer", 1));
		store.putItem(item("long", 2L));
		store.putItem(item("a-null", null));
		store.putItem(StoreItem.of(NAMESPACE, "b-missing", Map.of()));

		assertSortedKeys(store, ascending, List.of("a-null", "b-missing", "integer", "long"), "score", "key");
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void stringValuesKeepLexicographicOrdering(boolean ascending) {
		MemoryStore store = new MemoryStore();
		store.putItem(item("two", "2"));
		store.putItem(item("ten", "10"));

		assertSortedKeys(store, ascending, List.of("ten", "two"), "score");
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void integralComparatorSatisfiesContract(boolean ascending) {
		Comparator<StoreItem> comparator = new MemoryStore().createComparator(request(ascending, "score"));
		List<Number> values = List.of(new BigInteger("-9223372036854775809"), Long.MIN_VALUE, (byte) -1,
				(short) 0, 0, 0L, BigInteger.ZERO, (byte) 1, (short) 1, 1, 1L, BigInteger.ONE,
				9007199254740992L, new BigInteger("9007199254740993"), Long.MAX_VALUE,
				new BigInteger("9223372036854775808"), BigInteger.TEN.pow(100));
		for (Number first : values) {
			for (Number second : values) {
				StoreItem firstItem = item("first", first);
				StoreItem secondItem = item("second", second);
				int comparison = Integer.signum(comparator.compare(firstItem, secondItem));
				int expected = new BigInteger(first.toString()).compareTo(new BigInteger(second.toString()));
				assertThat(comparison).isEqualTo(ascending ? Integer.signum(expected) : -Integer.signum(expected));
				assertThat(comparison).isEqualTo(-Integer.signum(comparator.compare(secondItem, firstItem)));
				for (Number third : values) {
					StoreItem thirdItem = item("third", third);
					int secondToThird = Integer.signum(comparator.compare(secondItem, thirdItem));
					int firstToThird = Integer.signum(comparator.compare(firstItem, thirdItem));
					if (comparison > 0 && secondToThird > 0) {
						assertThat(firstToThird).isPositive();
					}
					if (comparison == 0) {
						assertThat(firstToThird).isEqualTo(secondToThird);
					}
				}
			}
		}
	}

	@Test
	void otherComparableTypesKeepTheirBehavior() {
		Comparator<StoreItem> comparator = new MemoryStore().createComparator(request(true, "score"));
		assertThat(comparator.compare(item("first", -0.0d), item("second", 0.0d))).isNegative();
		assertThat(comparator.compare(item("first", Double.NEGATIVE_INFINITY), item("second", 0.0d)))
			.isNegative();
		assertThat(comparator.compare(item("first", Double.POSITIVE_INFINITY), item("second", Double.NaN)))
			.isNegative();
		assertThat(comparator.compare(item("first", -0.0f), item("second", 0.0f))).isNegative();
		assertThat(comparator.compare(item("first", new BigDecimal("1.0")),
				item("second", new BigDecimal("1.00")))).isZero();
		assertThat(comparator.compare(item("first", false), item("second", true))).isNegative();
		for (Object value : List.of("1", 1.0d, 1.0f, new BigDecimal("1.0"))) {
			assertThrows(ClassCastException.class, () -> comparator.compare(item("first", 1), item("second", value)));
			assertThrows(ClassCastException.class, () -> comparator.compare(item("first", value), item("second", 1)));
		}
	}

	private static StoreItem item(String key, Object value) {
		Map<String, Object> values = new HashMap<>();
		values.put("score", value);
		return StoreItem.of(NAMESPACE, key, values);
	}

	private static StoreSearchRequest request(boolean ascending, String... fields) {
		return StoreSearchRequest.builder().sortFields(List.of(fields)).ascending(ascending).limit(100).build();
	}

	private static void assertSortedKeys(Store store, boolean ascending, List<String> ascendingKeys, String... fields) {
		List<String> expectedKeys = new ArrayList<>(ascendingKeys);
		if (!ascending) {
			Collections.reverse(expectedKeys);
		}
		assertThat(store.searchItems(request(ascending, fields)).getItems())
			.extracting(StoreItem::getKey)
			.containsExactlyElementsOf(expectedKeys);
	}

}
