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

import com.alibaba.cloud.ai.entity.SemanticModel;
import com.alibaba.cloud.ai.entity.SemanticModelDTO;
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
public class SemanticModelPersistenceService {

	private static final String FIELD_ADD = """
				INSERT INTO semantic_model (
					agent_id,
					field_name,
					synonyms,
					origin_name,
					description,
					origin_description,
					type,
					is_recall,
					status,
					created_time,
					updated_time
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
			""";

	private static final String FIELD_UPDATE = """
				UPDATE semantic_model
				SET
					field_name = ?,
					synonyms = ?,
					origin_name = ?,
					description = ?,
					origin_description = ?,
					type = ?,
					is_recall = ?,
					status = ?,
					updated_time = ?
				WHERE id = ?
			""";

	private static final String FIELD_ENABLE = """
				UPDATE semantic_model SET status = 1 WHERE id = ?
			""";

	private static final String FIELD_DISABLE = """
				UPDATE semantic_model SET status = 0 WHERE id = ?
			""";

	private static final String FIELD_GET_BY_AGENT_ID = """
			SELECT
			    id,
			    agent_id,
				field_name,
				synonyms,
				origin_name,
				description,
				origin_description,
				type,
				is_recall,
				status,
				created_time,
				updated_time
			FROM semantic_model WHERE agent_id = ?
			""";

	private static final String FIELD_CLEAR = """
			DELETE FROM semantic_model WHERE id = ?
			""";

	// Fuzzy search
	private static final String FIELD_SEARCH = """
			   		SELECT
				id,
				field_name,
				synonyms,
				origin_name,
				description,
				origin_description,
				type,
				is_recall,
				status,
				created_time,
				updated_time
			FROM semantic_model WHERE field_name LIKE ? OR origin_name LIKE ? OR synonyms LIKE ?
			""";

	private final JdbcTemplate jdbcTemplate;

	public SemanticModelPersistenceService(JdbcTemplate jdbcTemplate) {
		Assert.notNull(jdbcTemplate, "jdbcTemplate cannot be null");
		this.jdbcTemplate = jdbcTemplate;
	}

	// Add agent field
	public void addField(SemanticModelDTO semanticModelDTO) {
		SemanticModel semanticModel = new SemanticModel();
		BeanUtils.copyProperties(semanticModelDTO, semanticModel);
		semanticModel.setCreateTime(LocalDateTime.now());
		semanticModel.setUpdateTime(LocalDateTime.now());
		jdbcTemplate.update(FIELD_ADD, semanticModel.getAgentId(), semanticModel.getAgentFieldName(),
				semanticModel.getFieldSynonyms(), semanticModel.getOriginalFieldName(),
				semanticModel.getFieldDescription(), semanticModel.getOriginalDescription(),
				semanticModel.getFieldType(), semanticModel.getDefaultRecall(), semanticModel.getEnabled(),
				semanticModel.getCreateTime(), semanticModel.getUpdateTime());
	}

	// Batch add agent fields
	public void addFields(List<SemanticModelDTO> semanticModelDTOS) {
		List<SemanticModel> semanticModels = semanticModelDTOS.stream().map(semanticModelDTO -> {
			SemanticModel semanticModel = new SemanticModel();
			BeanUtils.copyProperties(semanticModelDTO, semanticModel);
			semanticModel.setCreateTime(LocalDateTime.now());
			semanticModel.setUpdateTime(LocalDateTime.now());
			return semanticModel;
		}).collect(Collectors.toList());
		jdbcTemplate.batchUpdate(FIELD_ADD, new AddBatchPreparedStatement(semanticModels));
	}

	// Batch enable
	public void enableFields(List<Long> ids) {
		jdbcTemplate.batchUpdate(FIELD_ENABLE, new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				ps.setLong(1, ids.get(i));
			}

			@Override
			public int getBatchSize() {
				return 0;
			}
		});
	}

	// Batch disable
	public void disableFields(List<Long> ids) {
		jdbcTemplate.batchUpdate(FIELD_DISABLE, new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				ps.setLong(1, ids.get(i));
			}

			@Override
			public int getBatchSize() {
				return 0;
			}
		});
	}

	// Get semantic model by agent ID
	public List<SemanticModel> getFieldByAgentId(Long agentId) {
		return this.jdbcTemplate.query(FIELD_GET_BY_AGENT_ID, new Object[] { agentId }, (rs, rowNum) -> {
			SemanticModel model = new SemanticModel();
			model.setId(rs.getObject("id", Long.class));
			model.setAgentId(rs.getObject("agent_id", Long.class));
			model.setOriginalFieldName(rs.getString("origin_name"));
			model.setAgentFieldName(rs.getString("field_name"));
			model.setFieldSynonyms(rs.getString("synonyms"));
			model.setFieldDescription(rs.getString("description"));
			model.setDefaultRecall(rs.getObject("is_recall", Boolean.class));
			model.setEnabled(rs.getObject("status", Boolean.class));
			model.setFieldType(rs.getString("type"));
			model.setOriginalDescription(rs.getString("origin_description"));
			model.setCreateTime(rs.getTimestamp("created_time").toLocalDateTime());
			model.setUpdateTime(rs.getTimestamp("updated_time").toLocalDateTime());
			return model;
		});
	}

	// Search
	public List<SemanticModel> searchFields(String keyword) {
		Objects.requireNonNull(keyword, "searchKeyword cannot be null");
		return jdbcTemplate.query(FIELD_SEARCH,
				new Object[] { "%" + keyword + "%", "%" + keyword + "%", "%" + keyword + "%" }, (rs, rowNum) -> {
					SemanticModel model = new SemanticModel();
					model.setId(rs.getObject("id", Long.class));
					model.setAgentId(rs.getObject("agent_id", Long.class)); // Add agentId
					model.setOriginalFieldName(rs.getString("origin_name"));
					model.setAgentFieldName(rs.getString("field_name"));
					model.setFieldSynonyms(rs.getString("synonyms"));
					model.setFieldDescription(rs.getString("description"));
					model.setDefaultRecall(rs.getObject("is_recall", Boolean.class));
					model.setEnabled(rs.getObject("status", Boolean.class));
					model.setFieldType(rs.getString("type"));
					model.setOriginalDescription(rs.getString("origin_description"));
					model.setCreateTime(rs.getTimestamp("created_time").toLocalDateTime());
					model.setUpdateTime(rs.getTimestamp("updated_time").toLocalDateTime());
					return model;
				});
	}

	// Delete agent field by id
	public void deleteFieldById(long id) {
		jdbcTemplate.update(FIELD_CLEAR, id);
	}

	// Update agent field
	public void updateField(SemanticModelDTO semanticModelDTO, long id) {
		jdbcTemplate.update(FIELD_UPDATE, semanticModelDTO.getAgentFieldName(), semanticModelDTO.getFieldSynonyms(),
				semanticModelDTO.getOriginalFieldName(), semanticModelDTO.getFieldDescription(),
				semanticModelDTO.getOriginalDescription(), semanticModelDTO.getFieldType(),
				semanticModelDTO.getDefaultRecall(), semanticModelDTO.getEnabled(),
				Timestamp.valueOf(LocalDateTime.now()), id);
	}

	private record AddBatchPreparedStatement(
			List<SemanticModel> semanticModels) implements BatchPreparedStatementSetter {

		@Override
		public void setValues(PreparedStatement ps, int i) throws SQLException {
			var field = this.semanticModels.get(i);
			ps.setString(1, field.getAgentFieldName()); // field_name
			ps.setString(2, field.getFieldSynonyms()); // synonyms
			ps.setString(3, field.getOriginalFieldName()); // origin_name
			ps.setString(4, field.getFieldDescription()); // field_description
			ps.setString(5, field.getOriginalDescription()); // origin_description
			ps.setString(6, field.getFieldType()); // type
			ps.setObject(7, field.getDefaultRecall()); // is_recall
			ps.setObject(8, field.getEnabled()); // status
			ps.setTimestamp(9, Timestamp.valueOf(field.getCreateTime())); // created_time
			ps.setTimestamp(10, Timestamp.valueOf(field.getUpdateTime())); // updated_time
		}

		@Override
		public int getBatchSize() {
			return this.semanticModels.size();
		}
	}

}
