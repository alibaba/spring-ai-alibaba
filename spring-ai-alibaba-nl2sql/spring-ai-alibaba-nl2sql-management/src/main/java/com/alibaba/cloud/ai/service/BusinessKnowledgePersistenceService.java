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

import com.alibaba.cloud.ai.entity.BusinessKnowledge;
import com.alibaba.cloud.ai.entity.BusinessKnowledgeDTO;
import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class BusinessKnowledgePersistenceService {

	private static final String FIELD_ADD = """
				INSERT INTO business_knowledge (
					business_term,
					description,
			        	synonyms,
					is_recall,
					data_set_id,
					agent_id,
			       	created_time,
			        	updated_time
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
			""";

	private static final String FIELD_UPDATE = """
				UPDATE business_knowledge
				SET
					business_term = ?,
					description = ?,
					synonyms = ?,
					is_recall = ?,
					data_set_id = ?,
					agent_id = ?,
					updated_time = ?
				WHERE id = ?
			""";

	private static final String FIELD_GET_DATASET_IDS = """
			SELECT data_set_id FROM business_knowledge
			""";

	private static final String FIELD_GET_BY_DATASET_IDS = """
			SELECT
			    id,
				business_term,
				description,
			       	synonyms,
				is_recall,
				data_set_id,
				agent_id,
			      	created_time,
			       	updated_time
			FROM business_knowledge WHERE data_set_id = ?
			""";

	private static final String FIELD_CLEAR = """
			DELETE FROM business_knowledge WHERE id = ?
			""";

	// Fuzzy search
	private static final String FIELD_SEARCH = """
			   		SELECT
				id,
				business_term,
				description,
			       	synonyms,
				is_recall,
				data_set_id,
				agent_id,
			      	created_time,
			       	updated_time
			FROM business_knowledge WHERE business_term LIKE ? OR description LIKE ? OR synonyms LIKE ?
			""";

	// Query business knowledge by agent ID
	private static final String FIELD_GET_BY_AGENT_ID = """
			SELECT
				id,
				business_term,
				description,
				synonyms,
				is_recall,
				data_set_id,
				agent_id,
				created_time,
				updated_time
			FROM business_knowledge WHERE agent_id = ?
			""";

	// Delete business knowledge by agent ID
	private static final String FIELD_DELETE_BY_AGENT_ID = """
			DELETE FROM business_knowledge WHERE agent_id = ?
			""";

	// Search business knowledge within agent scope
	private static final String FIELD_SEARCH_IN_AGENT = """
			SELECT
				id,
				business_term,
				description,
				synonyms,
				is_recall,
				data_set_id,
				agent_id,
				created_time,
				updated_time
			FROM business_knowledge
			WHERE agent_id = ? AND (business_term LIKE ? OR description LIKE ? OR synonyms LIKE ?)
			""";

	private final JdbcTemplate jdbcTemplate;

	public BusinessKnowledgePersistenceService(JdbcTemplate jdbcTemplate) {
		Assert.notNull(jdbcTemplate, "jdbcTemplate cannot be null");
		this.jdbcTemplate = jdbcTemplate;
	}

	// Add agent field
	public void addKnowledge(BusinessKnowledgeDTO knowledgeDTO) {
		BusinessKnowledge knowledge = new BusinessKnowledge();
		BeanUtils.copyProperties(knowledgeDTO, knowledge);
		knowledge.setCreateTime(LocalDateTime.now());
		knowledge.setUpdateTime(LocalDateTime.now());
		jdbcTemplate.update(FIELD_ADD, knowledge.getBusinessTerm(), knowledge.getDescription(), knowledge.getSynonyms(),
				knowledge.getDefaultRecall(), knowledge.getDatasetId(), knowledge.getAgentId(),
				knowledge.getCreateTime(), knowledge.getUpdateTime());
	}

	// Batch add agent fields
	public void addKnowledgeList(List<BusinessKnowledgeDTO> knowledgeDTOList) {
		List<BusinessKnowledge> knowledgeList = knowledgeDTOList.stream().map(knowledgeDTO -> {
			BusinessKnowledge knowledge = new BusinessKnowledge();
			BeanUtils.copyProperties(knowledgeDTO, knowledge);
			knowledge.setCreateTime(LocalDateTime.now());
			knowledge.setUpdateTime(LocalDateTime.now());
			return knowledge;
		}).collect(Collectors.toList());
		jdbcTemplate.batchUpdate(FIELD_ADD, new AddBatchPreparedStatement(knowledgeList));
	}

	// Get dataset list
	public List<String> getDataSetIds() {
		return this.jdbcTemplate.query(FIELD_GET_DATASET_IDS, (rs, rowNum) -> rs.getString("data_set_id"));
	}

	// Get agent fields by data_set_id
	public List<BusinessKnowledge> getFieldByDataSetId(String dataSetId) {
		return this.jdbcTemplate.query(FIELD_GET_BY_DATASET_IDS, new Object[] { dataSetId }, (rs, rowNum) -> {
			return new BusinessKnowledge(rs.getObject("id", Long.class), // id
					rs.getString("business_term"), // businessTerm
					rs.getString("description"), // description
					rs.getString("synonyms"), // synonyms
					rs.getObject("is_recall", boolean.class), // defaultRecall (convert to
																// Boolean)
					rs.getString("data_set_id"), // datasetId
					rs.getString("agent_id"), // agentId
					rs.getTimestamp("created_time").toLocalDateTime(), // createTime
					rs.getTimestamp("updated_time").toLocalDateTime() // updateTime
			);
		});
	}

	// Search
	public List<BusinessKnowledge> searchFields(String keyword) {
		Objects.requireNonNull(keyword, "searchKeyword cannot be null");
		return jdbcTemplate.query(FIELD_SEARCH,
				new Object[] { "%" + keyword + "%", "%" + keyword + "%", "%" + keyword + "%" }, (rs, rowNum) -> {
					return new BusinessKnowledge(rs.getObject("id", Long.class), rs.getString("business_term"),
							rs.getString("description"), rs.getString("synonyms"),
							rs.getObject("is_recall", boolean.class), rs.getString("data_set_id"),
							rs.getString("agent_id"), rs.getTimestamp("created_time").toLocalDateTime(),
							rs.getTimestamp("updated_time").toLocalDateTime());
				});
	}

	// Delete agent field by id
	public void deleteFieldById(long id) {
		jdbcTemplate.update(FIELD_CLEAR, id);
	}

	// Update agent field
	public void updateField(BusinessKnowledgeDTO knowledgeDTO, long id) {
		jdbcTemplate.update(FIELD_UPDATE, knowledgeDTO.getBusinessTerm(), knowledgeDTO.getDescription(),
				knowledgeDTO.getSynonyms(), knowledgeDTO.getDefaultRecall(), knowledgeDTO.getDatasetId(),
				knowledgeDTO.getAgentId(), Timestamp.valueOf(LocalDateTime.now()), id);
	}

	// Get business knowledge list by agent ID
	public List<BusinessKnowledge> getKnowledgeByAgentId(String agentId) {
		return jdbcTemplate.query(FIELD_GET_BY_AGENT_ID, new Object[] { agentId }, (rs, rowNum) -> {
			return new BusinessKnowledge(rs.getObject("id", Long.class), rs.getString("business_term"),
					rs.getString("description"), rs.getString("synonyms"), rs.getObject("is_recall", boolean.class),
					rs.getString("data_set_id"), rs.getString("agent_id"),
					rs.getTimestamp("created_time").toLocalDateTime(),
					rs.getTimestamp("updated_time").toLocalDateTime());
		});
	}

	// Delete all business knowledge by agent ID
	public void deleteKnowledgeByAgentId(String agentId) {
		jdbcTemplate.update(FIELD_DELETE_BY_AGENT_ID, agentId);
	}

	// Search business knowledge within agent scope
	public List<BusinessKnowledge> searchKnowledgeInAgent(String agentId, String keyword) {
		Objects.requireNonNull(agentId, "agentId cannot be null");
		Objects.requireNonNull(keyword, "searchKeyword cannot be null");
		return jdbcTemplate.query(FIELD_SEARCH_IN_AGENT,
				new Object[] { agentId, "%" + keyword + "%", "%" + keyword + "%", "%" + keyword + "%" },
				(rs, rowNum) -> {
					return new BusinessKnowledge(rs.getObject("id", Long.class), rs.getString("business_term"),
							rs.getString("description"), rs.getString("synonyms"),
							rs.getObject("is_recall", boolean.class), rs.getString("data_set_id"),
							rs.getString("agent_id"), rs.getTimestamp("created_time").toLocalDateTime(),
							rs.getTimestamp("updated_time").toLocalDateTime());
				});
	}

	private record AddBatchPreparedStatement(
			List<BusinessKnowledge> knowledgeDTOList) implements BatchPreparedStatementSetter {

		@Override
		public void setValues(PreparedStatement ps, int i) throws SQLException {
			var field = this.knowledgeDTOList.get(i);
			ps.setString(1, field.getBusinessTerm()); // field_name
			ps.setString(2, field.getDescription()); // field_description
			ps.setString(3, field.getSynonyms()); // synonyms
			ps.setObject(4, field.getDefaultRecall()); // is_recall
			ps.setObject(5, field.getDatasetId()); // data_set_id
			ps.setObject(6, field.getAgentId()); // agent_id
			ps.setTimestamp(7, Timestamp.valueOf(field.getCreateTime())); // created_time
			ps.setTimestamp(8, Timestamp.valueOf(field.getUpdateTime())); // updated_time
		}

		@Override
		public int getBatchSize() {
			return this.knowledgeDTOList.size();
		}
	}

}
