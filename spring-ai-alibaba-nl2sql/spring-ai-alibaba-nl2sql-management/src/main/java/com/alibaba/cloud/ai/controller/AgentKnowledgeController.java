/**
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.entity.AgentKnowledge;
import com.alibaba.cloud.ai.service.AgentKnowledgeService;
import com.alibaba.cloud.ai.service.AgentVectorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能体知识管理控制器
 */
@RestController
@RequestMapping("/api/agent-knowledge")
@CrossOrigin(origins = "*")
public class AgentKnowledgeController {

	private final AgentKnowledgeService agentKnowledgeService;

	private final AgentVectorService agentVectorService;

	public AgentKnowledgeController(AgentKnowledgeService agentKnowledgeService,
			AgentVectorService agentVectorService) {
		this.agentKnowledgeService = agentKnowledgeService;
		this.agentVectorService = agentVectorService;
	}

	/**
	 * 根据智能体ID查询知识列表
	 */
	@GetMapping("/agent/{agentId}")
	public ResponseEntity<Map<String, Object>> getKnowledgeByAgentId(@PathVariable Integer agentId,
			@RequestParam(required = false) String type, @RequestParam(required = false) String status,
			@RequestParam(required = false) String keyword) {

		Map<String, Object> response = new HashMap<>();

		try {
			List<AgentKnowledge> knowledgeList;

			if (keyword != null && !keyword.trim().isEmpty()) {
				// 搜索知识
				knowledgeList = agentKnowledgeService.searchKnowledge(agentId, keyword.trim());
			}
			else if (type != null && !type.trim().isEmpty()) {
				// 按类型筛选
				knowledgeList = agentKnowledgeService.getKnowledgeByType(agentId, type);
			}
			else if (status != null && !status.trim().isEmpty()) {
				// 按状态筛选
				knowledgeList = agentKnowledgeService.getKnowledgeByStatus(agentId, status);
			}
			else {
				// 查询所有知识
				knowledgeList = agentKnowledgeService.getKnowledgeByAgentId(agentId);
			}

			response.put("success", true);
			response.put("data", knowledgeList);
			response.put("total", knowledgeList.size());
			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			response.put("success", false);
			response.put("message", "查询知识列表失败：" + e.getMessage());
			return ResponseEntity.badRequest().body(response);
		}
	}

	/**
	 * 根据ID查询知识详情
	 */
	@GetMapping("/{id}")
	public ResponseEntity<Map<String, Object>> getKnowledgeById(@PathVariable Integer id) {
		Map<String, Object> response = new HashMap<>();

		try {
			AgentKnowledge knowledge = agentKnowledgeService.getKnowledgeById(id);
			if (knowledge != null) {
				response.put("success", true);
				response.put("data", knowledge);
				return ResponseEntity.ok(response);
			}
			else {
				response.put("success", false);
				response.put("message", "知识不存在");
				return ResponseEntity.notFound().build();
			}
		}
		catch (Exception e) {
			response.put("success", false);
			response.put("message", "查询知识详情失败：" + e.getMessage());
			return ResponseEntity.badRequest().body(response);
		}
	}

	/**
	 * 创建知识
	 */
	@PostMapping
	public ResponseEntity<Map<String, Object>> createKnowledge(@RequestBody AgentKnowledge knowledge) {
		Map<String, Object> response = new HashMap<>();

		try {
			// 验证必填字段
			if (knowledge.getAgentId() == null) {
				response.put("success", false);
				response.put("message", "智能体ID不能为空");
				return ResponseEntity.badRequest().body(response);
			}

			if (knowledge.getTitle() == null || knowledge.getTitle().trim().isEmpty()) {
				response.put("success", false);
				response.put("message", "知识标题不能为空");
				return ResponseEntity.badRequest().body(response);
			}

			// 创建知识到数据库
			AgentKnowledge createdKnowledge = agentKnowledgeService.createKnowledge(knowledge);

			// 如果知识内容不为空且状态为active，则添加到向量库
			if (createdKnowledge.getContent() != null && !createdKnowledge.getContent().trim().isEmpty()
					&& "active".equals(createdKnowledge.getStatus())) {
				try {
					agentVectorService.addKnowledgeToVector(Long.valueOf(createdKnowledge.getAgentId()),
							createdKnowledge);
					// 更新嵌入状态为已完成
					createdKnowledge.setEmbeddingStatus("completed");
					agentKnowledgeService.updateKnowledge(createdKnowledge.getId(), createdKnowledge);
				}
				catch (Exception vectorException) {
					// 向量存储失败，更新嵌入状态为失败
					createdKnowledge.setEmbeddingStatus("failed");
					agentKnowledgeService.updateKnowledge(createdKnowledge.getId(), createdKnowledge);
					// 记录日志但不影响主流程
					response.put("vectorWarning", "知识已保存，但向量化失败：" + vectorException.getMessage());
				}
			}

			response.put("success", true);
			response.put("data", createdKnowledge);
			response.put("message", "知识创建成功");
			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			response.put("success", false);
			response.put("message", "创建知识失败：" + e.getMessage());
			return ResponseEntity.badRequest().body(response);
		}
	}

	/**
	 * 更新知识
	 */
	@PutMapping("/{id}")
	public ResponseEntity<Map<String, Object>> updateKnowledge(@PathVariable Integer id,
			@RequestBody AgentKnowledge knowledge) {

		Map<String, Object> response = new HashMap<>();

		try {
			// 验证必填字段
			if (knowledge.getTitle() == null || knowledge.getTitle().trim().isEmpty()) {
				response.put("success", false);
				response.put("message", "知识标题不能为空");
				return ResponseEntity.badRequest().body(response);
			}

			// 先获取原有知识信息
			AgentKnowledge originalKnowledge = agentKnowledgeService.getKnowledgeById(id);
			if (originalKnowledge == null) {
				response.put("success", false);
				response.put("message", "知识不存在");
				return ResponseEntity.notFound().build();
			}

			// 更新数据库中的知识
			AgentKnowledge updatedKnowledge = agentKnowledgeService.updateKnowledge(id, knowledge);
			if (updatedKnowledge != null) {
				// 处理向量存储的更新
				try {
					Long agentId = Long.valueOf(updatedKnowledge.getAgentId());

					// 如果内容有变化或状态变为active，需要重新向量化
					boolean contentChanged = !java.util.Objects.equals(originalKnowledge.getContent(),
							updatedKnowledge.getContent());
					boolean statusChangedToActive = !"active".equals(originalKnowledge.getStatus())
							&& "active".equals(updatedKnowledge.getStatus());
					boolean statusChangedFromActive = "active".equals(originalKnowledge.getStatus())
							&& !"active".equals(updatedKnowledge.getStatus());

					if (statusChangedFromActive) {
						// 状态从active变为其他，删除向量数据
						agentVectorService.deleteKnowledgeFromVector(agentId, id);
						updatedKnowledge.setEmbeddingStatus("pending");
					}
					else if ((contentChanged || statusChangedToActive) && "active".equals(updatedKnowledge.getStatus())
							&& updatedKnowledge.getContent() != null
							&& !updatedKnowledge.getContent().trim().isEmpty()) {
						// 内容变化或状态变为active，重新向量化
						agentVectorService.deleteKnowledgeFromVector(agentId, id); // 先删除旧的
						agentVectorService.addKnowledgeToVector(agentId, updatedKnowledge); // 再添加新的
						updatedKnowledge.setEmbeddingStatus("completed");
						agentKnowledgeService.updateKnowledge(id, updatedKnowledge); // 更新嵌入状态
					}
				}
				catch (Exception vectorException) {
					// 向量存储操作失败，更新嵌入状态为失败
					updatedKnowledge.setEmbeddingStatus("failed");
					agentKnowledgeService.updateKnowledge(id, updatedKnowledge);
					response.put("vectorWarning", "知识已更新，但向量化失败：" + vectorException.getMessage());
				}

				response.put("success", true);
				response.put("data", updatedKnowledge);
				response.put("message", "知识更新成功");
				return ResponseEntity.ok(response);
			}
			else {
				response.put("success", false);
				response.put("message", "知识更新失败");
				return ResponseEntity.badRequest().body(response);
			}
		}
		catch (Exception e) {
			response.put("success", false);
			response.put("message", "更新知识失败：" + e.getMessage());
			return ResponseEntity.badRequest().body(response);
		}
	}

	/**
	 * 删除知识
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Map<String, Object>> deleteKnowledge(@PathVariable Integer id) {
		Map<String, Object> response = new HashMap<>();

		try {
			// 先获取知识信息，用于删除向量数据
			AgentKnowledge knowledge = agentKnowledgeService.getKnowledgeById(id);
			if (knowledge == null) {
				response.put("success", false);
				response.put("message", "知识不存在");
				return ResponseEntity.notFound().build();
			}

			// 删除数据库中的知识
			boolean deleted = agentKnowledgeService.deleteKnowledge(id);
			if (deleted) {
				// 同时删除向量数据
				try {
					Long agentId = Long.valueOf(knowledge.getAgentId());
					agentVectorService.deleteKnowledgeFromVector(agentId, id);
				}
				catch (Exception vectorException) {
					// 向量删除失败，记录警告但不影响主流程
					response.put("vectorWarning", "知识已删除，但向量数据删除失败：" + vectorException.getMessage());
				}

				response.put("success", true);
				response.put("message", "知识删除成功");
				return ResponseEntity.ok(response);
			}
			else {
				response.put("success", false);
				response.put("message", "知识删除失败");
				return ResponseEntity.badRequest().body(response);
			}
		}
		catch (Exception e) {
			response.put("success", false);
			response.put("message", "删除知识失败：" + e.getMessage());
			return ResponseEntity.badRequest().body(response);
		}
	}

	/**
	 * 批量更新知识状态
	 */
	@PutMapping("/batch/status")
	public ResponseEntity<Map<String, Object>> batchUpdateStatus(@RequestBody Map<String, Object> request) {

		Map<String, Object> response = new HashMap<>();

		try {
			@SuppressWarnings("unchecked")
			List<Integer> ids = (List<Integer>) request.get("ids");
			String status = (String) request.get("status");

			if (ids == null || ids.isEmpty()) {
				response.put("success", false);
				response.put("message", "ID列表不能为空");
				return ResponseEntity.badRequest().body(response);
			}

			if (status == null || status.trim().isEmpty()) {
				response.put("success", false);
				response.put("message", "状态不能为空");
				return ResponseEntity.badRequest().body(response);
			}

			int updatedCount = agentKnowledgeService.batchUpdateStatus(ids, status);
			response.put("success", true);
			response.put("message", "批量更新成功，共更新 " + updatedCount + " 条记录");
			response.put("updatedCount", updatedCount);
			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			response.put("success", false);
			response.put("message", "批量更新失败：" + e.getMessage());
			return ResponseEntity.badRequest().body(response);
		}
	}

	/**
	 * 获取智能体知识统计信息
	 */
	@GetMapping("/statistics/{agentId}")
	public ResponseEntity<Map<String, Object>> getKnowledgeStatistics(@PathVariable Integer agentId) {
		Map<String, Object> response = new HashMap<>();

		try {
			int totalCount = agentKnowledgeService.countKnowledgeByAgent(agentId);
			List<Object[]> typeStatistics = agentKnowledgeService.countKnowledgeByType(agentId);

			Map<String, Object> statistics = new HashMap<>();
			statistics.put("totalCount", totalCount);
			statistics.put("typeStatistics", typeStatistics);

			response.put("success", true);
			response.put("data", statistics);
			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			response.put("success", false);
			response.put("message", "获取统计信息失败：" + e.getMessage());
			return ResponseEntity.badRequest().body(response);
		}
	}

}
