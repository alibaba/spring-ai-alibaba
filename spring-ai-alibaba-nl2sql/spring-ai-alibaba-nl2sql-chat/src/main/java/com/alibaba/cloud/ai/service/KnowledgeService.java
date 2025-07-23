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

import com.alibaba.cloud.ai.dto.Knowledge;
import com.alibaba.cloud.ai.dto.schema.KnowledgeDTO;
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
import java.util.stream.Collectors;

@Service
public class KnowledgeService {

	private static final String FIELD_ADD = """
				INSERT INTO profession_knowledge (
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
				UPDATE profession_knowledge
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
			SELECT data_set_id FROM profession_knowledge
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
			FROM profession_knowledge WHERE data_set_id = ?
			""";

	private static final String FIELD_CLEAR = """
			DELETE FROM profession_knowledge WHERE id = ?
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
			FROM profession_knowledge WHERE business_term LIKE ? OR description LIKE ? OR synonyms LIKE ?
			""";

	private final JdbcTemplate jdbcTemplate;

	public KnowledgeService(JdbcTemplate jdbcTemplate) {
		Assert.notNull(jdbcTemplate, "jdbcTemplate cannot be null");
		this.jdbcTemplate = jdbcTemplate;
	}

	// 新增智能体字段
	public void addKnowledge(KnowledgeDTO knowledgeDTO) {
		Knowledge knowledge = new Knowledge();
		BeanUtils.copyProperties(knowledgeDTO, knowledge);
		knowledge.setCreatedTime(Timestamp.valueOf(LocalDateTime.now()));
		knowledge.setUpdatedTime(Timestamp.valueOf(LocalDateTime.now()));
		jdbcTemplate.update(FIELD_ADD, knowledge.getBusinessTerm(), knowledge.getDescription(), knowledge.getSynonyms(), knowledge.getIsRecall(), knowledge.getDataSetId(), knowledge.getCreatedTime(), knowledge.getUpdatedTime());
	}

	// 批量新增智能体字段
	public void addKnowledgeList(List<KnowledgeDTO> knowledgeDTOList) {
		List<Knowledge> knowledgeList = knowledgeDTOList.stream().map(knowledgeDTO -> {
			Knowledge knowledge = new Knowledge();
			BeanUtils.copyProperties(knowledgeDTO, knowledge);
			knowledge.setCreatedTime(Timestamp.valueOf(LocalDateTime.now()));
			knowledge.setUpdatedTime(Timestamp.valueOf(LocalDateTime.now()));
			return knowledge;
		}).collect(Collectors.toList());
		jdbcTemplate.batchUpdate(FIELD_ADD, new AddBatchPreparedStatement(knowledgeList));
	}

	// 获取数据集列表
	public List<String> getDataSetIds() {
		return this.jdbcTemplate.query(FIELD_GET_DATASET_IDS, (rs, rowNum) -> rs.getString("data_set_id"));
	}

	// 根据data_set_id获取智能体字段
	public List<Knowledge> getFieldByDataSetId(String dataSetId) {
		return this.jdbcTemplate.query(FIELD_GET_BY_DATASET_IDS, new Object[] { dataSetId }, (rs, rowNum) -> {
			return new Knowledge(rs.getObject("id", Integer.class), rs.getString("business_term"),
					rs.getString("description"), rs.getString("synonyms"), rs.getObject("is_recall", Integer.class),
					rs.getString("data_set_id"), rs.getTimestamp("created_time"), rs.getTimestamp("updated_time"));
		});
	}

	// 搜索
	public List<Knowledge> searchFields(String keyword) {
		return jdbcTemplate.query(FIELD_SEARCH, new Object[] { "%" + keyword + "%", "%" + keyword + "%", "%" + keyword + "%" }, (rs, rowNum) -> {
			return new Knowledge(rs.getObject("id", Integer.class), rs.getString("business_term"),
					rs.getString("description"), rs.getString("synonyms"), rs.getObject("is_recall", Integer.class),
					rs.getString("data_set_id"), rs.getTimestamp("created_time"), rs.getTimestamp("updated_time"));
		});
	}

	// 根据id删除智能体字段
	public void deleteFieldById(int id) {
		jdbcTemplate.update(FIELD_CLEAR, id);
	}

	// 更新智能体字段
	public void updateField(KnowledgeDTO knowledgeDTO, int id) {
		jdbcTemplate.update(FIELD_UPDATE, knowledgeDTO.getBusinessTerm(), knowledgeDTO.getDescription(),
				knowledgeDTO.getSynonyms(), knowledgeDTO.getIsRecall(), knowledgeDTO.getDataSetId(),
				Timestamp.valueOf(LocalDateTime.now()), id);
	}

	private record AddBatchPreparedStatement(
			List<Knowledge> knowledgeDTOList) implements BatchPreparedStatementSetter {

		@Override
		public void setValues(PreparedStatement ps, int i) throws SQLException {
			var field = this.knowledgeDTOList.get(i);
			ps.setString(1, field.getBusinessTerm()); // field_name
			ps.setString(2, field.getDescription()); // field_description
			ps.setString(3, field.getSynonyms()); // synonyms
			ps.setObject(4, field.getIsRecall()); // is_recall
			ps.setObject(5, field.getDataSetId()); // data_set_id
			ps.setTimestamp(6, field.getCreatedTime()); // created_time
			ps.setTimestamp(7, field.getUpdatedTime()); // updated_time
		}

		@Override
		public int getBatchSize() {
			return this.knowledgeDTOList.size();
		}
	}

}
