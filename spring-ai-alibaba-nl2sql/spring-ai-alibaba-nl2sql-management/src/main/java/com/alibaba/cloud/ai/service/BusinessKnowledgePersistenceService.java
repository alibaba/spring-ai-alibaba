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
			       	created_time,
			        	updated_time
				) VALUES (?, ?, ?, ?, ?, ?, ?)
			""";

	private static final String FIELD_UPDATE = """
				UPDATE business_knowledge
				SET
					business_term = ?,
					description = ?,
					synonyms = ?,
					is_recall = ?,
					data_set_id = ?,
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
			      	created_time,
			       	updated_time
			FROM business_knowledge WHERE data_set_id = ?
			""";

	private static final String FIELD_CLEAR = """
			DELETE FROM business_knowledge WHERE id = ?
			""";

	// 模糊搜素
	private static final String FIELD_SEARCH = """
			   		SELECT
				id,
				business_term,
				description,
			       	synonyms,
				is_recall,
				data_set_id,
			      	created_time,
			       	updated_time
			FROM business_knowledge WHERE business_term LIKE ? OR description LIKE ? OR synonyms LIKE ?
			""";

	private final JdbcTemplate jdbcTemplate;

	public BusinessKnowledgePersistenceService(JdbcTemplate jdbcTemplate) {
		Assert.notNull(jdbcTemplate, "jdbcTemplate cannot be null");
		this.jdbcTemplate = jdbcTemplate;
	}

	// 新增智能体字段
	public void addKnowledge(BusinessKnowledgeDTO knowledgeDTO) {
		BusinessKnowledge knowledge = new BusinessKnowledge();
		BeanUtils.copyProperties(knowledgeDTO, knowledge);
		knowledge.setCreateTime(LocalDateTime.now());
		knowledge.setUpdateTime(LocalDateTime.now());
		jdbcTemplate.update(FIELD_ADD, knowledge.getBusinessTerm(), knowledge.getDescription(), knowledge.getSynonyms(),
				knowledge.getDefaultRecall(), knowledge.getDatasetId(), knowledge.getCreateTime(),
				knowledge.getUpdateTime());
	}

	// 批量新增智能体字段
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

	// 获取数据集列表
	public List<String> getDataSetIds() {
		return this.jdbcTemplate.query(FIELD_GET_DATASET_IDS, (rs, rowNum) -> rs.getString("data_set_id"));
	}

	// 根据data_set_id获取智能体字段
	public List<BusinessKnowledge> getFieldByDataSetId(String dataSetId) {
		return this.jdbcTemplate.query(FIELD_GET_BY_DATASET_IDS, new Object[] { dataSetId }, (rs, rowNum) -> {
			return new BusinessKnowledge(rs.getObject("id", Long.class), // id
					rs.getString("business_term"), // businessTerm
					rs.getString("description"), // description
					rs.getString("synonyms"), // synonyms
					rs.getObject("is_recall", boolean.class), // defaultRecall (convert to
																// Boolean)
					rs.getString("data_set_id"), // datasetId
					rs.getTimestamp("created_time").toLocalDateTime(), // createTime
					rs.getTimestamp("updated_time").toLocalDateTime() // updateTime
			);
		});
	}

	// 搜索
	public List<BusinessKnowledge> searchFields(String keyword) {
		Objects.requireNonNull(keyword, "searchKeyword cannot be null");
		return jdbcTemplate.query(FIELD_SEARCH,
				new Object[] { "%" + keyword + "%", "%" + keyword + "%", "%" + keyword + "%" }, (rs, rowNum) -> {
					return new BusinessKnowledge(rs.getObject("id", Long.class), rs.getString("business_term"),
							rs.getString("description"), rs.getString("synonyms"),
							rs.getObject("is_recall", boolean.class), rs.getString("data_set_id"),
							rs.getTimestamp("created_time").toLocalDateTime(),
							rs.getTimestamp("updated_time").toLocalDateTime());
				});
	}

	// 根据id删除智能体字段
	public void deleteFieldById(long id) {
		jdbcTemplate.update(FIELD_CLEAR, id);
	}

	// 更新智能体字段
	public void updateField(BusinessKnowledgeDTO knowledgeDTO, long id) {
		jdbcTemplate.update(FIELD_UPDATE, knowledgeDTO.getBusinessTerm(), knowledgeDTO.getDescription(),
				knowledgeDTO.getSynonyms(), knowledgeDTO.getDefaultRecall(), knowledgeDTO.getDatasetId(),
				Timestamp.valueOf(LocalDateTime.now()), id);
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
			ps.setTimestamp(6, Timestamp.valueOf(field.getCreateTime())); // created_time
			ps.setTimestamp(7, Timestamp.valueOf(field.getUpdateTime())); // updated_time
		}

		@Override
		public int getBatchSize() {
			return this.knowledgeDTOList.size();
		}
	}

}
