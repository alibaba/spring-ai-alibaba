/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.example.deepresearch.dispatcher;

import com.alibaba.cloud.ai.graph.OverAllState;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Test cases for UserFileRagDispatcher
 *
 * @author tfh-yqf
 * @since 2025/8/22
 */
@RunWith(MockitoJUnitRunner.class)
public class UserFileRagDispatcherTest {

	@Mock
	private OverAllState mockState;

	private UserFileRagDispatcher dispatcher;

	@BeforeEach
	void setUp() {
		dispatcher = new UserFileRagDispatcher();
	}

	@Test
	void testApply_WhenPreviousDecisionIsUserFileRag_ShouldReturnBackgroundInvestigator() {
		// Given: 前置节点决定进入user_file_rag
		Map<String, Object> stateValues = new HashMap<>();
		stateValues.put("rewrite_multi_query_next_node", "user_file_rag");

		when(mockState.value("rewrite_multi_query_next_node", END)).thenReturn("user_file_rag");

		// When: 调用dispatcher
		String result = dispatcher.apply(mockState);

		// Then: 应该返回background_investigator
		assertEquals("background_investigator", result);
	}

	@Test
    void testApply_WhenPreviousDecisionIsBackgroundInvestigator_ShouldReturnEnd() {
        // Given: 前置节点决定进入background_investigator
        when(mockState.value("rewrite_multi_query_next_node", END)).thenReturn("background_investigator");

        // When: 调用dispatcher
        String result = dispatcher.apply(mockState);

        // Then: 应该返回END
        assertEquals(END, result);
    }

	@Test
    void testApply_WhenPreviousDecisionIsEnd_ShouldReturnEnd() {
        // Given: 前置节点决定结束
        when(mockState.value("rewrite_multi_query_next_node", END)).thenReturn(END);

        // When: 调用dispatcher
        String result = dispatcher.apply(mockState);

        // Then: 应该返回END
        assertEquals(END, result);
    }

	@Test
    void testApply_WhenPreviousDecisionIsNull_ShouldReturnEnd() {
        // Given: 前置节点没有设置决策值
        when(mockState.value("rewrite_multi_query_next_node", END)).thenReturn(null);

        // When: 调用dispatcher
        String result = dispatcher.apply(mockState);

        // Then: 应该返回默认值END
        assertEquals(END, result);
    }

	@Test
    void testApply_WhenPreviousDecisionIsUnknown_ShouldReturnEnd() {
        // Given: 前置节点设置了未知的决策值
        when(mockState.value("rewrite_multi_query_next_node", END)).thenReturn("unknown_node");

        // When: 调用dispatcher
        String result = dispatcher.apply(mockState);

        // Then: 应该返回END
        assertEquals(END, result);
    }

}
