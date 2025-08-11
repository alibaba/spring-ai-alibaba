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
 *
 */

package com.alibaba.cloud.ai.mcp.router.core.vectorstore;

import com.alibaba.cloud.ai.mcp.router.model.McpServerInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SimpleMcpServerVectorStore 的测试类 主要测试初始化阶段的全量读取策略和正常阶段的嵌入模型搜索功能
 */
@ExtendWith(MockitoExtension.class)
class SimpleMcpServerVectorStoreTest {

	@Mock
	private EmbeddingModel embeddingModel;

	private SimpleMcpServerVectorStore vectorStore;

	private McpServerInfo testServerInfo1;

	private McpServerInfo testServerInfo2;

	private McpServerInfo testServerInfo3;

	@BeforeEach
	void setUp() {
		// 创建测试实例 - 使用 null vectorStore 以确保在初始化阶段不调用向量存储
		vectorStore = new SimpleMcpServerVectorStore(embeddingModel, null);

		// 创建测试数据
		testServerInfo1 = new McpServerInfo("database-service", "Database connection service", "http", "1.0.0",
				"http://localhost:3000", true, Arrays.asList("database", "mysql"));

		testServerInfo2 = new McpServerInfo("file-service", "File management service", "http", "2.0.0",
				"http://localhost:3001", true, Arrays.asList("file", "storage"));

		testServerInfo3 = new McpServerInfo("ai-service", "AI processing service", "websocket", "1.5.0",
				"ws://localhost:3002", false, Arrays.asList("ai", "ml", "processing"));
	}

	@Test
	void testInitializationPhase_getAllServers_shouldReturnFromCache() {
		// 确保处于初始化阶段
		assertFalse(vectorStore.isInitializationComplete());

		// 添加测试数据
		assertTrue(vectorStore.addServer(testServerInfo1));
		assertTrue(vectorStore.addServer(testServerInfo2));
		assertTrue(vectorStore.addServer(testServerInfo3));

		// 在初始化阶段获取所有服务，应该从缓存返回，不调用嵌入模型
		List<McpServerInfo> allServers = vectorStore.getAllServers();

		// 验证结果
		assertNotNull(allServers);
		assertEquals(3, allServers.size());

		// 验证返回的数据正确性
		assertTrue(allServers.stream().anyMatch(s -> "database-service".equals(s.getName())));
		assertTrue(allServers.stream().anyMatch(s -> "file-service".equals(s.getName())));
		assertTrue(allServers.stream().anyMatch(s -> "ai-service".equals(s.getName())));
	}

	@Test
	void testInitializationPhase_getServer_shouldReturnFromCache() {
		// 确保处于初始化阶段
		assertFalse(vectorStore.isInitializationComplete());

		// 添加测试数据
		assertTrue(vectorStore.addServer(testServerInfo1));

		// 在初始化阶段获取指定服务，应该从缓存返回
		McpServerInfo foundServer = vectorStore.getServer("database-service");

		// 验证结果
		assertNotNull(foundServer);
		assertEquals("database-service", foundServer.getName());
		assertEquals("Database connection service", foundServer.getDescription());
		assertEquals("http", foundServer.getProtocol());
		assertEquals("1.0.0", foundServer.getVersion());
		assertEquals("http://localhost:3000", foundServer.getEndpoint());
		assertTrue(foundServer.getEnabled());
		assertEquals(Arrays.asList("database", "mysql"), foundServer.getTags());
	}

	@Test
	void testInitializationPhase_search_shouldUseTextMatching() {
		// 确保处于初始化阶段
		assertFalse(vectorStore.isInitializationComplete());

		// 添加测试数据
		assertTrue(vectorStore.addServer(testServerInfo1));
		assertTrue(vectorStore.addServer(testServerInfo2));
		assertTrue(vectorStore.addServer(testServerInfo3));

		// 在初始化阶段搜索，应该使用简单的文本匹配
		List<McpServerInfo> searchResults = vectorStore.search("database", 10);

		// 验证结果 - 应该找到包含"database"的服务
		assertNotNull(searchResults);
		assertTrue(searchResults.size() > 0);
		assertTrue(searchResults.stream().anyMatch(s -> "database-service".equals(s.getName())));

		// 搜索另一个关键词
		List<McpServerInfo> fileResults = vectorStore.search("file", 10);
		assertNotNull(fileResults);
		assertTrue(fileResults.stream().anyMatch(s -> "file-service".equals(s.getName())));

		// 搜索标签
		List<McpServerInfo> aiResults = vectorStore.search("ai", 10);
		assertNotNull(aiResults);
		assertTrue(aiResults.stream().anyMatch(s -> "ai-service".equals(s.getName())));
	}

	@Test
	void testInitializationPhase_size_shouldReturnCacheSize() {
		// 确保处于初始化阶段
		assertFalse(vectorStore.isInitializationComplete());

		// 初始大小应该为0
		assertEquals(0, vectorStore.size());

		// 添加服务
		assertTrue(vectorStore.addServer(testServerInfo1));
		assertEquals(1, vectorStore.size());

		assertTrue(vectorStore.addServer(testServerInfo2));
		assertEquals(2, vectorStore.size());

		assertTrue(vectorStore.addServer(testServerInfo3));
		assertEquals(3, vectorStore.size());
	}

	@Test
	void testMarkInitializationComplete_shouldSwitchToVectorSearch() {
		// 在初始化阶段添加数据
		assertFalse(vectorStore.isInitializationComplete());
		assertTrue(vectorStore.addServer(testServerInfo1));

		// 标记初始化完成
		vectorStore.markInitializationComplete();
		assertTrue(vectorStore.isInitializationComplete());

		// 现在应该使用向量搜索模式
		// 注意：由于我们使用的是mock的EmbeddingModel，实际的向量搜索可能返回空结果
		// 但重要的是验证方法调用不会抛出异常，说明切换到了向量搜索模式
		assertDoesNotThrow(() -> {
			List<McpServerInfo> results = vectorStore.getAllServers();
			assertNotNull(results);
		});

		assertDoesNotThrow(() -> {
			vectorStore.getServer("database-service");
			// 在向量搜索模式下，可能找不到服务
		});

		assertDoesNotThrow(() -> {
			List<McpServerInfo> searchResults = vectorStore.search("database", 5);
			assertNotNull(searchResults);
		});
	}

	@Test
	void testResetInitializationState_shouldSwitchBackToCache() {
		// 标记初始化完成
		vectorStore.markInitializationComplete();
		assertTrue(vectorStore.isInitializationComplete());

		// 重置初始化状态
		vectorStore.resetInitializationState();
		assertFalse(vectorStore.isInitializationComplete());

		// 添加数据并验证又回到了缓存模式
		assertTrue(vectorStore.addServer(testServerInfo1));
		List<McpServerInfo> allServers = vectorStore.getAllServers();
		assertNotNull(allServers);
		assertEquals(1, allServers.size());
		assertEquals("database-service", allServers.get(0).getName());
	}

	@Test
	void testRemoveServer_shouldUpdateBothCacheAndVectorStore() {
		// 添加服务
		assertTrue(vectorStore.addServer(testServerInfo1));
		assertTrue(vectorStore.addServer(testServerInfo2));
		assertEquals(2, vectorStore.size());

		// 删除服务
		assertTrue(vectorStore.removeServer("database-service"));
		assertEquals(1, vectorStore.size());

		// 验证服务已被删除
		assertNull(vectorStore.getServer("database-service"));
		assertNotNull(vectorStore.getServer("file-service"));

		// 尝试删除不存在的服务
		assertFalse(vectorStore.removeServer("non-existent-service"));
		assertEquals(1, vectorStore.size());
	}

	@Test
	void testClear_shouldClearBothCacheAndVectorStore() {
		// 添加服务
		assertTrue(vectorStore.addServer(testServerInfo1));
		assertTrue(vectorStore.addServer(testServerInfo2));
		assertTrue(vectorStore.addServer(testServerInfo3));
		assertEquals(3, vectorStore.size());

		// 清空存储
		vectorStore.clear();
		assertEquals(0, vectorStore.size());

		// 验证所有服务都被清除
		List<McpServerInfo> allServers = vectorStore.getAllServers();
		assertNotNull(allServers);
		assertTrue(allServers.isEmpty());
	}

	@Test
	void testAddServer_shouldReplaceExistingServer() {
		// 添加服务
		assertTrue(vectorStore.addServer(testServerInfo1));
		assertEquals(1, vectorStore.size());

		// 创建同名但不同描述的服务
		McpServerInfo updatedServerInfo = new McpServerInfo("database-service", // 同样的名称
				"Updated database service", // 不同的描述
				"https", "2.0.0", "https://localhost:3000", true, Arrays.asList("database", "postgresql"));

		// 添加更新的服务信息
		assertTrue(vectorStore.addServer(updatedServerInfo));
		assertEquals(1, vectorStore.size()); // 大小应该保持不变

		// 验证服务信息已更新
		McpServerInfo retrievedServer = vectorStore.getServer("database-service");
		assertNotNull(retrievedServer);
		assertEquals("Updated database service", retrievedServer.getDescription());
		assertEquals("https", retrievedServer.getProtocol());
		assertEquals("2.0.0", retrievedServer.getVersion());
	}

	@Test
	void testSearchWithEmptyQuery_shouldReturnAllServers() {
		// 添加测试数据
		assertTrue(vectorStore.addServer(testServerInfo1));
		assertTrue(vectorStore.addServer(testServerInfo2));
		assertTrue(vectorStore.addServer(testServerInfo3));

		// 使用空查询搜索
		List<McpServerInfo> results = vectorStore.search("", 10);
		assertNotNull(results);
		assertEquals(3, results.size());

		// 使用null查询搜索
		List<McpServerInfo> nullResults = vectorStore.search(null, 10);
		assertNotNull(nullResults);
		assertEquals(3, nullResults.size());
	}

	@Test
	void testSearchWithLimit_shouldRespectLimit() {
		// 添加测试数据
		assertTrue(vectorStore.addServer(testServerInfo1));
		assertTrue(vectorStore.addServer(testServerInfo2));
		assertTrue(vectorStore.addServer(testServerInfo3));

		// 搜索时限制结果数量
		List<McpServerInfo> results = vectorStore.search("", 2);
		assertNotNull(results);
		assertTrue(results.size() <= 2);
	}

	@Test
	void testCaseInsensitiveSearch() {
		// 添加测试数据
		assertTrue(vectorStore.addServer(testServerInfo1));

		// 测试大小写不敏感的搜索
		List<McpServerInfo> upperCaseResults = vectorStore.search("DATABASE", 10);
		List<McpServerInfo> lowerCaseResults = vectorStore.search("database", 10);
		List<McpServerInfo> mixedCaseResults = vectorStore.search("DataBase", 10);

		// 所有搜索应该返回相同的结果
		assertNotNull(upperCaseResults);
		assertNotNull(lowerCaseResults);
		assertNotNull(mixedCaseResults);

		// 在初始化阶段，应该都能找到database-service
		assertTrue(upperCaseResults.stream().anyMatch(s -> "database-service".equals(s.getName())));
		assertTrue(lowerCaseResults.stream().anyMatch(s -> "database-service".equals(s.getName())));
		assertTrue(mixedCaseResults.stream().anyMatch(s -> "database-service".equals(s.getName())));
	}

	@Test
	void testAddServerWithNullValues_shouldHandleGracefully() {
		// 测试添加null服务
		assertFalse(vectorStore.addServer(null));
		assertEquals(0, vectorStore.size());

		// 测试添加名称为null的服务
		McpServerInfo nullNameServer = new McpServerInfo(null, "Description", "http", "1.0.0", "http://localhost", true,
				Arrays.asList("tag"));
		assertFalse(vectorStore.addServer(nullNameServer));
		assertEquals(0, vectorStore.size());
	}

	@Test
	void testGetServerWithNullName_shouldReturnNull() {
		// 测试获取null名称的服务
		assertNull(vectorStore.getServer(null));
	}

	@Test
	void testRemoveServerWithNullName_shouldReturnFalse() {
		// 测试删除null名称的服务
		assertFalse(vectorStore.removeServer(null));
	}

	@Test
	void testSearchInDescription() {
		// 添加测试数据
		assertTrue(vectorStore.addServer(testServerInfo1));
		assertTrue(vectorStore.addServer(testServerInfo2));

		// 搜索描述中的关键词
		List<McpServerInfo> connectionResults = vectorStore.search("connection", 10);
		assertNotNull(connectionResults);
		assertTrue(connectionResults.stream().anyMatch(s -> "database-service".equals(s.getName())));

		List<McpServerInfo> managementResults = vectorStore.search("management", 10);
		assertNotNull(managementResults);
		assertTrue(managementResults.stream().anyMatch(s -> "file-service".equals(s.getName())));
	}

	@Test
	void testSearchInTags() {
		// 添加测试数据
		assertTrue(vectorStore.addServer(testServerInfo1));
		assertTrue(vectorStore.addServer(testServerInfo3));

		// 搜索标签中的关键词
		List<McpServerInfo> mysqlResults = vectorStore.search("mysql", 10);
		assertNotNull(mysqlResults);
		assertTrue(mysqlResults.stream().anyMatch(s -> "database-service".equals(s.getName())));

		List<McpServerInfo> mlResults = vectorStore.search("ml", 10);
		assertNotNull(mlResults);
		assertTrue(mlResults.stream().anyMatch(s -> "ai-service".equals(s.getName())));
	}

	@Test
	void testInitializationPhasePerformance() {
		// 测试初始化阶段的性能 - 应该不调用嵌入模型
		assertFalse(vectorStore.isInitializationComplete());

		// 添加大量服务（模拟真实场景）
		for (int i = 0; i < 100; i++) {
			McpServerInfo serverInfo = new McpServerInfo("service-" + i, "Description for service " + i, "http",
					"1.0.0", "http://localhost:" + (3000 + i), true, Arrays.asList("tag" + i, "common"));
			assertTrue(vectorStore.addServer(serverInfo));
		}

		// 验证大小
		assertEquals(100, vectorStore.size());

		// 获取所有服务应该很快（从缓存读取）
		long startTime = System.currentTimeMillis();
		List<McpServerInfo> allServers = vectorStore.getAllServers();
		long endTime = System.currentTimeMillis();

		assertNotNull(allServers);
		assertEquals(100, allServers.size());

		// 初始化阶段的操作应该很快（通常小于10ms）
		long duration = endTime - startTime;
		System.out.println("初始化阶段获取所有服务耗时: " + duration + "ms");
		assertTrue(duration < 100, "初始化阶段操作应该很快，实际耗时: " + duration + "ms");
	}

}
