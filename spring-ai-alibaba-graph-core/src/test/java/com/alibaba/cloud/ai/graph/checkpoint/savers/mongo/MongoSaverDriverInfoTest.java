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
package com.alibaba.cloud.ai.graph.checkpoint.savers.mongo;

import com.mongodb.MongoDriverInformation;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MongoSaverDriverInfoTest {

	@Test
	void driverInfo_hasExpectedName() throws Exception {
		Field field = MongoSaver.class.getDeclaredField("DRIVER_INFO");
		field.setAccessible(true);
		MongoDriverInformation info = (MongoDriverInformation) field.get(null);
		assertEquals("spring-ai-alibaba", info.getDriverNames().get(0));
	}

	@Test
	void constructor_silentlySkipsAppendMetadataWhenUnavailable() {
		// MongoClient (driver 5.5.2) has no appendMetadata — the reflection guard
		// must not throw.
		MongoClient mockClient = mock(MongoClient.class);
		MongoDatabase mockDb = mock(MongoDatabase.class);
		when(mockClient.getDatabase(anyString())).thenReturn(mockDb);

		MongoSaver saver = MongoSaver.builder()
				.client(mockClient)
				.build();

		// If we reach here without exception the guard worked correctly.
		assertTrue(true);
	}

	@Test
	void constructor_invokesAppendMetadataWhenAvailable() throws Exception {
		// Simulate a client that exposes appendMetadata via a subinterface.
		List<MongoDriverInformation> captured = new ArrayList<>();

		MongoClientWithMetadata clientWithMetadata = mock(MongoClientWithMetadata.class);
		MongoDatabase mockDb = mock(MongoDatabase.class);
		when(clientWithMetadata.getDatabase(anyString())).thenReturn(mockDb);
		org.mockito.Mockito.doAnswer(inv -> {
			captured.add(inv.getArgument(0));
			return null;
		}).when(clientWithMetadata).appendMetadata(org.mockito.ArgumentMatchers.any(MongoDriverInformation.class));

		MongoSaver.builder()
				.client(clientWithMetadata)
				.build();

		assertFalse(captured.isEmpty(), "appendMetadata should have been called");
		assertEquals("spring-ai-alibaba", captured.get(0).getDriverNames().get(0));
	}

	/** Extends MongoClient with appendMetadata to simulate driver >= 5.6.0. */
	interface MongoClientWithMetadata extends MongoClient {
		void appendMetadata(MongoDriverInformation driverInformation);
	}

}
