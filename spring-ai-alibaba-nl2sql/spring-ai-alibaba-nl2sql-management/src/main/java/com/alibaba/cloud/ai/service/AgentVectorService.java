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
package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.entity.AgentKnowledge;
import com.alibaba.cloud.ai.request.SchemaInitRequest;
import com.alibaba.cloud.ai.service.simple.SimpleVectorStoreService;
import com.alibaba.cloud.ai.connector.bo.DbQueryParameter;
import com.alibaba.cloud.ai.connector.bo.TableInfoBO;
import com.alibaba.cloud.ai.connector.config.DbConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 智能体向量存储服务 专门处理智能体相关的向量存储操作，确保数据隔离
 */
@Service
public class AgentVectorService {

	private static final Logger log = LoggerFactory.getLogger(AgentVectorService.class);

	@Autowired
	private SimpleVectorStoreService vectorStoreService;

	/**
	 * 为智能体初始化数据库Schema
	 * @param agentId 智能体ID
	 * @param schemaInitRequest Schema初始化请求
	 * @return 是否成功
	 */
	public Boolean initializeSchemaForAgent(Long agentId, SchemaInitRequest schemaInitRequest) {
		try {
			String agentIdStr = String.valueOf(agentId);
			log.info("Initializing schema for agent: {}", agentIdStr);

			return vectorStoreService.schemaForAgent(agentIdStr, schemaInitRequest);
		}
		catch (Exception e) {
			log.error("Failed to initialize schema for agent: {}", agentId, e);
			throw new RuntimeException("Failed to initialize schema for agent " + agentId + ": " + e.getMessage(), e);
		}
	}

	/**
	 * 为智能体添加知识文档到向量库
	 * @param agentId 智能体ID
	 * @param knowledge 知识内容
	 */
	public void addKnowledgeToVector(Long agentId, AgentKnowledge knowledge) {
		try {
			String agentIdStr = String.valueOf(agentId);
			log.info("Adding knowledge to vector store for agent: {}, knowledge ID: {}", agentIdStr, knowledge.getId());

			// 创建文档
			Document document = createDocumentFromKnowledge(agentIdStr, knowledge);

			// 添加到向量库
			vectorStoreService.getAgentVectorStoreManager().addDocuments(agentIdStr, List.of(document));

			log.info("Successfully added knowledge to vector store for agent: {}", agentIdStr);
		}
		catch (Exception e) {
			log.error("Failed to add knowledge to vector store for agent: {}, knowledge ID: {}", agentId,
					knowledge.getId(), e);
			throw new RuntimeException("Failed to add knowledge to vector store: " + e.getMessage(), e);
		}
	}

	/**
	 * 为智能体批量添加知识文档到向量库
	 * @param agentId 智能体ID
	 * @param knowledgeList 知识列表
	 */
	public void addKnowledgeListToVector(Long agentId, List<AgentKnowledge> knowledgeList) {
		if (knowledgeList == null || knowledgeList.isEmpty()) {
			log.warn("No knowledge to add for agent: {}", agentId);
			return;
		}

		try {
			String agentIdStr = String.valueOf(agentId);
			log.info("Adding {} knowledge items to vector store for agent: {}", knowledgeList.size(), agentIdStr);

			// 创建文档列表
			List<Document> documents = knowledgeList.stream()
				.map(knowledge -> createDocumentFromKnowledge(agentIdStr, knowledge))
				.toList();

			// 批量添加到向量库
			vectorStoreService.getAgentVectorStoreManager().addDocuments(agentIdStr, documents);

			log.info("Successfully added {} knowledge items to vector store for agent: {}", documents.size(),
					agentIdStr);
		}
		catch (Exception e) {
			log.error("Failed to add knowledge list to vector store for agent: {}", agentId, e);
			throw new RuntimeException("Failed to add knowledge list to vector store: " + e.getMessage(), e);
		}
	}

	/**
	 * 从向量库中搜索相关知识
	 * @param agentId 智能体ID
	 * @param query 查询文本
	 * @param topK 返回结果数量
	 * @return 相关文档列表
	 */
	public List<Document> searchKnowledge(Long agentId, String query, int topK) {
		try {
			String agentIdStr = String.valueOf(agentId);
			log.debug("Searching knowledge for agent: {}, query: {}, topK: {}", agentIdStr, query, topK);

			List<Document> results = vectorStoreService.getAgentVectorStoreManager()
				.similaritySearch(agentIdStr, query, topK);

			log.info("Found {} knowledge documents for agent: {}", results.size(), agentIdStr);
			return results;
		}
		catch (Exception e) {
			log.error("Failed to search knowledge for agent: {}", agentId, e);
			throw new RuntimeException("Failed to search knowledge: " + e.getMessage(), e);
		}
	}

	/**
	 * 从向量库中搜索特定类型的知识
	 * @param agentId 智能体ID
	 * @param query 查询文本
	 * @param topK 返回结果数量
	 * @param knowledgeType 知识类型
	 * @return 相关文档列表
	 */
	public List<Document> searchKnowledgeByType(Long agentId, String query, int topK, String knowledgeType) {
		try {
			String agentIdStr = String.valueOf(agentId);
			log.debug("Searching knowledge by type for agent: {}, query: {}, topK: {}, type: {}", agentIdStr, query,
					topK, knowledgeType);

			List<Document> results = vectorStoreService.getAgentVectorStoreManager()
				.similaritySearchWithFilter(agentIdStr, query, topK, "knowledge:" + knowledgeType);

			log.info("Found {} knowledge documents of type '{}' for agent: {}", results.size(), knowledgeType,
					agentIdStr);
			return results;
		}
		catch (Exception e) {
			log.error("Failed to search knowledge by type for agent: {}", agentId, e);
			throw new RuntimeException("Failed to search knowledge by type: " + e.getMessage(), e);
		}
	}

	/**
	 * 删除智能体的特定知识文档
	 * @param agentId 智能体ID
	 * @param knowledgeId 知识ID
	 */
	public void deleteKnowledgeFromVector(Long agentId, Integer knowledgeId) {
		try {
			String agentIdStr = String.valueOf(agentId);
			String documentId = agentIdStr + ":knowledge:" + knowledgeId;

			log.info("Deleting knowledge from vector store for agent: {}, knowledge ID: {}", agentIdStr, knowledgeId);

			vectorStoreService.getAgentVectorStoreManager().deleteDocuments(agentIdStr, List.of(documentId));

			log.info("Successfully deleted knowledge from vector store for agent: {}", agentIdStr);
		}
		catch (Exception e) {
			log.error("Failed to delete knowledge from vector store for agent: {}, knowledge ID: {}", agentId,
					knowledgeId, e);
			throw new RuntimeException("Failed to delete knowledge from vector store: " + e.getMessage(), e);
		}
	}

	/**
	 * 删除智能体的所有向量数据
	 * @param agentId 智能体ID
	 */
	public void deleteAllVectorDataForAgent(Long agentId) {
		try {
			String agentIdStr = String.valueOf(agentId);
			log.info("Deleting all vector data for agent: {}", agentIdStr);

			vectorStoreService.getAgentVectorStoreManager().deleteAgentData(agentIdStr);

			log.info("Successfully deleted all vector data for agent: {}", agentIdStr);
		}
		catch (Exception e) {
			log.error("Failed to delete all vector data for agent: {}", agentId, e);
			throw new RuntimeException("Failed to delete all vector data: " + e.getMessage(), e);
		}
	}

	/**
	 * 获取智能体向量存储统计信息
	 * @param agentId 智能体ID
	 * @return 统计信息
	 */
	public Map<String, Object> getVectorStatistics(Long agentId) {
		try {
			String agentIdStr = String.valueOf(agentId);
			Map<String, Object> stats = new HashMap<>();

			boolean hasData = vectorStoreService.getAgentVectorStoreManager().hasAgentData(agentIdStr);
			int documentCount = vectorStoreService.getAgentVectorStoreManager().getDocumentCount(agentIdStr);

			stats.put("agentId", agentId);
			stats.put("hasData", hasData);
			stats.put("documentCount", documentCount);

			log.debug("Vector statistics for agent {}: hasData={}, documentCount={}", agentIdStr, hasData,
					documentCount);

			return stats;
		}
		catch (Exception e) {
			log.error("Failed to get vector statistics for agent: {}", agentId, e);
			throw new RuntimeException("Failed to get vector statistics: " + e.getMessage(), e);
		}
	}

	/**
	 * 从AgentKnowledge创建Document
	 */
	private Document createDocumentFromKnowledge(String agentId, AgentKnowledge knowledge) {
		String documentId = agentId + ":knowledge:" + knowledge.getId();
		String content = knowledge.getContent();
		if (content == null || content.trim().isEmpty()) {
			content = knowledge.getTitle(); // 如果内容为空，使用标题
		}

		Map<String, Object> metadata = new HashMap<>();
		metadata.put("agentId", agentId);
		metadata.put("knowledgeId", knowledge.getId());
		metadata.put("title", knowledge.getTitle());
		metadata.put("type", knowledge.getType());
		metadata.put("category", knowledge.getCategory());
		metadata.put("tags", knowledge.getTags());
		metadata.put("status", knowledge.getStatus());
		metadata.put("vectorType", "knowledge:" + knowledge.getType());
		metadata.put("sourceUrl", knowledge.getSourceUrl());
		metadata.put("fileType", knowledge.getFileType());
		metadata.put("embeddingStatus", knowledge.getEmbeddingStatus());
		metadata.put("createTime", knowledge.getCreateTime());

		return new Document(documentId, content, metadata);
	}

	@Autowired
	private DatasourceService datasourceService;

	@Autowired
	@Qualifier("mysqlAccessor")
	private com.alibaba.cloud.ai.connector.accessor.Accessor dbAccessor;

	/**
	 * 获取智能体配置的数据源列表 从数据库中查询智能体关联的数据源信息
	 */
	public List<Map<String, Object>> getAgentDatasources(Long agentId) {
		try {
			log.info("Getting datasources for agent: {}", agentId);

			// 调用DatasourceService获取智能体关联的数据源
			List<com.alibaba.cloud.ai.entity.AgentDatasource> agentDatasources = datasourceService
				.getAgentDatasources(agentId.intValue());

			List<Map<String, Object>> datasources = new ArrayList<>();

			for (com.alibaba.cloud.ai.entity.AgentDatasource agentDatasource : agentDatasources) {
				// 只返回激活状态的数据源
				if (agentDatasource.getIsActive() != null) {
					com.alibaba.cloud.ai.entity.Datasource datasource = agentDatasource.getDatasource();
					if (datasource != null) {
						Map<String, Object> dsMap = new HashMap<>();
						dsMap.put("id", datasource.getId());
						dsMap.put("name", datasource.getName());
						dsMap.put("type", datasource.getType());
						dsMap.put("host", datasource.getHost());
						dsMap.put("port", datasource.getPort());
						dsMap.put("databaseName", datasource.getDatabaseName());
						dsMap.put("username", datasource.getUsername());
						dsMap.put("password", datasource.getPassword()); // 添加密码字段
						dsMap.put("connectionUrl", datasource.getConnectionUrl());
						dsMap.put("status", datasource.getStatus());
						dsMap.put("testStatus", datasource.getTestStatus());
						dsMap.put("description", datasource.getDescription());
						dsMap.put("isActive", agentDatasource.getIsActive());
						dsMap.put("createTime", agentDatasource.getCreateTime());

						datasources.add(dsMap);
					}
				}
			}

			log.info("Found {} active datasources for agent: {}", datasources.size(), agentId);
			return datasources;

		}
		catch (Exception e) {
			log.error("Failed to get datasources for agent: {}", agentId, e);
			throw new RuntimeException("Failed to get datasources: " + e.getMessage(), e);
		}
	}

	/**
	 * 获取数据源的表列表
	 * @param datasourceId 数据源ID
	 * @return 表名列表
	 */
	public List<String> getDatasourceTables(Integer datasourceId) {
		try {
			log.info("Getting tables for datasource: {}", datasourceId);

			// 获取数据源信息
			com.alibaba.cloud.ai.entity.Datasource datasource = datasourceService.getDatasourceById(datasourceId);
			if (datasource == null) {
				throw new RuntimeException("Datasource not found with id: " + datasourceId);
			}

			// 检查数据源类型，目前只支持MySQL
			if (!"mysql".equalsIgnoreCase(datasource.getType())) {
				log.warn("Unsupported datasource type: {}, only MySQL is supported currently", datasource.getType());
				return new ArrayList<>();
			}

			// 创建数据库配置
			DbConfig dbConfig = createDbConfigFromDatasource(datasource);

			// 创建查询参数
			DbQueryParameter queryParam = DbQueryParameter.from(dbConfig);
			queryParam.setSchema(datasource.getDatabaseName());

			// 查询表列表
			List<TableInfoBO> tableInfoList = dbAccessor.showTables(dbConfig, queryParam);

			// 提取表名
			List<String> tableNames = tableInfoList.stream()
				.map(TableInfoBO::getName)
				.filter(name -> name != null && !name.trim().isEmpty())
				.sorted()
				.toList();

			log.info("Found {} tables for datasource: {}", tableNames.size(), datasourceId);
			return tableNames;

		}
		catch (Exception e) {
			log.error("Failed to get tables for datasource: {}", datasourceId, e);
			throw new RuntimeException("Failed to get tables: " + e.getMessage(), e);
		}
	}

	/**
	 * 从数据源实体创建数据库配置
	 */
	private DbConfig createDbConfigFromDatasource(com.alibaba.cloud.ai.entity.Datasource datasource) {
		DbConfig dbConfig = new DbConfig();

		// 设置基本连接信息
		dbConfig.setUrl(datasource.getConnectionUrl());
		dbConfig.setUsername(datasource.getUsername());
		dbConfig.setPassword(datasource.getPassword());

		// 设置数据库类型
		if ("mysql".equalsIgnoreCase(datasource.getType())) {
			dbConfig.setConnectionType("jdbc");
			dbConfig.setDialectType("mysql");
		}
		// 其他数据库类型的支持可以在这里扩展
		// else if ("postgresql".equalsIgnoreCase(datasource.getType())) {
		// dbConfig.setConnectionType("jdbc");
		// dbConfig.setDialectType("postgresql");
		// }

		// 设置Schema为数据源的数据库名称
		dbConfig.setSchema(datasource.getDatabaseName());

		log.debug("Created DbConfig for datasource {}: url={}, schema={}, type={}", datasource.getId(),
				dbConfig.getUrl(), dbConfig.getSchema(), dbConfig.getDialectType());

		return dbConfig;
	}

	/**
	 * 使用数据源ID为智能体初始化数据库Schema
	 * @param agentId 智能体ID
	 * @param datasourceId 数据源ID
	 * @param tables 表列表
	 * @return 是否成功
	 */
	public Boolean initializeSchemaForAgentWithDatasource(Long agentId, Integer datasourceId, List<String> tables) {
		try {
			String agentIdStr = String.valueOf(agentId);
			log.info("Initializing schema for agent: {} with datasource: {}, tables: {}", agentIdStr, datasourceId,
					tables);

			// 获取数据源信息
			com.alibaba.cloud.ai.entity.Datasource datasource = datasourceService.getDatasourceById(datasourceId);
			if (datasource == null) {
				throw new RuntimeException("Datasource not found with id: " + datasourceId);
			}

			// 创建数据库配置
			DbConfig dbConfig = createDbConfigFromDatasource(datasource);

			// 创建SchemaInitRequest
			SchemaInitRequest schemaInitRequest = new SchemaInitRequest();
			schemaInitRequest.setDbConfig(dbConfig);
			schemaInitRequest.setTables(tables);

			log.info("Created SchemaInitRequest for agent: {}, dbConfig: {}, tables: {}", agentIdStr, dbConfig, tables);

			// 调用原有的初始化方法
			return vectorStoreService.schemaForAgent(agentIdStr, schemaInitRequest);

		}
		catch (Exception e) {
			log.error("Failed to initialize schema for agent: {} with datasource: {}", agentId, datasourceId, e);
			throw new RuntimeException("Failed to initialize schema for agent " + agentId + ": " + e.getMessage(), e);
		}
	}

	/**
	 * 智能体聊天功能
	 * @param agentId 智能体ID
	 * @param query 用户查询
	 * @return 智能体回答
	 */
	public String chatWithAgent(Long agentId, String query) {
		try {
			String agentIdStr = String.valueOf(agentId);
			log.info("Processing chat request for agent: {}, query: {}", agentIdStr, query);

			// 检查智能体是否已初始化
			boolean hasData = vectorStoreService.getAgentVectorStoreManager().hasAgentData(agentIdStr);
			if (!hasData) {
				return "智能体尚未初始化数据源，请先在「初始化信息源」中配置数据源和表结构。";
			}

			// 获取智能体的数据源信息
			List<Map<String, Object>> datasources = getAgentDatasources(agentId);
			if (datasources.isEmpty()) {
				return "智能体没有配置可用的数据源，请先配置数据源。";
			}

			// 使用第一个激活的数据源
			Map<String, Object> datasource = datasources.get(0);

			// 创建数据库配置
			com.alibaba.cloud.ai.entity.Datasource dsEntity = datasourceService
				.getDatasourceById((Integer) datasource.get("id"));
			if (dsEntity == null) {
				return "数据源配置不存在，请检查数据源配置。";
			}

			DbConfig dbConfig = createDbConfigFromDatasource(dsEntity);

			// 使用SimpleNl2SqlService处理查询
			// 注意：这里需要注入SimpleNl2SqlService，但为了简化，我们先返回一个基本的响应
			String response = processAgentQuery(agentIdStr, query, dbConfig);

			log.info("Generated response for agent: {}", agentIdStr);
			return response;

		}
		catch (Exception e) {
			log.error("Failed to process chat request for agent: {}", agentId, e);
			return "处理查询时发生错误：" + e.getMessage() + "。请检查数据源配置和网络连接。";
		}
	}

	/**
	 * 处理智能体查询（简化版本）
	 */
	private String processAgentQuery(String agentId, String query, DbConfig dbConfig) {
		try {
			// 这里是一个简化的实现
			// 在实际应用中，应该集成完整的NL2SQL处理流程

			// 1. 检查是否是简单的问候语
			if (isGreeting(query)) {
				return "您好！我是您的数据分析助手。您可以用自然语言询问数据相关的问题，我会帮您查询和分析数据。\n\n" + "例如：\n" + "• 查询用户总数\n" + "• 显示最近一周的订单统计\n"
						+ "• 分析销售趋势\n\n" + "请告诉我您想了解什么数据信息？";
			}

			// 2. 获取相关的表和列信息
			List<org.springframework.ai.document.Document> relevantDocs = vectorStoreService
				.getAgentVectorStoreManager()
				.similaritySearch(agentId, query, 10);

			if (relevantDocs.isEmpty()) {
				return "抱歉，我没有找到与您的问题相关的数据表信息。请确保已正确初始化数据源，或者尝试用不同的方式描述您的问题。";
			}

			// 3. 构建响应
			StringBuilder response = new StringBuilder();
			response.append("根据您的问题「").append(query).append("」，我找到了以下相关信息：\n\n");

			// 分析相关的表和列
			Set<String> tables = new HashSet<>();
			List<String> columns = new ArrayList<>();

			for (org.springframework.ai.document.Document doc : relevantDocs) {
				Map<String, Object> metadata = doc.getMetadata();
				String vectorType = (String) metadata.get("vectorType");

				if ("table".equals(vectorType)) {
					tables.add((String) metadata.get("name"));
				}
				else if ("column".equals(vectorType)) {
					String tableName = (String) metadata.get("tableName");
					String columnName = (String) metadata.get("name");
					String description = (String) metadata.get("description");

					tables.add(tableName);
					columns.add(String.format("• %s.%s%s", tableName, columnName,
							description != null && !description.isEmpty() ? " - " + description : ""));
				}
			}

			if (!tables.isEmpty()) {
				response.append("📊 **相关数据表：**\n");
				for (String table : tables) {
					response.append("• ").append(table).append("\n");
				}
				response.append("\n");
			}

			if (!columns.isEmpty()) {
				response.append("📋 **相关字段：**\n");
				for (String column : columns.subList(0, Math.min(columns.size(), 8))) { // 限制显示数量
					response.append(column).append("\n");
				}
				if (columns.size() > 8) {
					response.append("... 还有 ").append(columns.size() - 8).append(" 个相关字段\n");
				}
				response.append("\n");
			}

			response.append("💡 **建议：**\n");
			response.append("基于找到的数据结构，您可以询问更具体的问题，比如：\n");
			if (tables.contains("users")) {
				response.append("• 用户总数是多少？\n");
				response.append("• 最近注册的用户有哪些？\n");
			}
			if (tables.contains("orders")) {
				response.append("• 今天的订单数量是多少？\n");
				response.append("• 最近一周的销售额是多少？\n");
			}
			if (tables.contains("products")) {
				response.append("• 有哪些产品分类？\n");
				response.append("• 最受欢迎的产品是什么？\n");
			}

			response.append("\n⚠️ **注意：** 当前为调试模式，显示的是数据结构分析。完整的SQL查询和数据分析功能正在开发中。");

			return response.toString();

		}
		catch (Exception e) {
			log.error("Error processing agent query: {}", e.getMessage(), e);
			return "处理查询时发生错误：" + e.getMessage();
		}
	}

	/**
	 * 检查是否是问候语
	 */
	private boolean isGreeting(String query) {
		String lowerQuery = query.toLowerCase().trim();
		return lowerQuery.matches(".*(你好|hello|hi|您好|嗨|hey).*") || lowerQuery.equals("你好") || lowerQuery.equals("您好")
				|| lowerQuery.equals("hello") || lowerQuery.equals("hi");
	}

}