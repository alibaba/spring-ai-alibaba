/*
 * Copyright 2025-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.state;

import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrimitiveArrayAppendTest {

	@Test
	void appendStrategySupportsPrimitiveArrays() {
		AppendStrategy strategy = new AppendStrategy();

		Object result = strategy.apply(List.of(0.5F), new float[] { 1.5F, 2.5F });

		assertEquals(List.of(0.5F, 1.5F, 2.5F), result);
	}

	@Test
	void appenderChannelSupportsPrimitiveArrays() {
		AppenderChannel<Integer> channel = AppenderChannel.of(ArrayList::new);

		Object result = channel.update("values", new ArrayList<>(List.of(1)), new int[] { 2, 3 });

		assertEquals(List.of(1, 2, 3), result);
	}

}
