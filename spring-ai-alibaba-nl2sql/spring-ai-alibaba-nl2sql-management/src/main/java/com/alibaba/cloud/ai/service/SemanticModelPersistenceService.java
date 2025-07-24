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
					field_name,
					synonyms,
					origin_name,
					description,
					origin_description,
					type,
					is_recall,
					status,
					data_set_id,
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
					data_set_id = ?,
					updated_time = ?
				WHERE id = ?
			""";

	private static final String FIELD_ENABLE = """
				UPDATE semantic_model SET status = 1 WHERE id = ?
			""";

	private static final String FIELD_DISABLE = """
				UPDATE semantic_model SET status = 0 WHERE id = ?
			""";

	private static final String FIELD_GET_DATASET_IDS = """
			SELECT data_set_id FROM semantic_model
			""";

	private static final String FIELD_GET_BY_DATASET_IDS = """
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
				data_set_id,
			          created_time,
			             updated_time
			FROM semantic_model WHERE data_set_id = ?
			""";

	private static final String FIELD_CLEAR = """
			DELETE FROM semantic_model WHERE id = ?
			""";

	// 模糊搜索
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
				data_set_id,
			          	created_time,
			             updated_time
			FROM semantic_model WHERE field_name LIKE ? OR origin_name LIKE ? OR synonyms LIKE ?
			""";

	private final JdbcTemplate jdbcTemplate;

	public SemanticModelPersistenceService(JdbcTemplate jdbcTemplate) {
		Assert.notNull(jdbcTemplate, "jdbcTemplate cannot be null");
		this.jdbcTemplate = jdbcTemplate;
	}

	// 新增智能体字段
	public void addField(SemanticModelDTO semanticModelDTO) {
		SemanticModel semanticModel = new SemanticModel();
		BeanUtils.copyProperties(semanticModelDTO, semanticModel);
		semanticModel.setCreateTime(LocalDateTime.now());
		semanticModel.setUpdateTime(LocalDateTime.now());
		jdbcTemplate.update(FIELD_ADD, semanticModel.getAgentFieldName(), semanticModel.getFieldSynonyms(),
				semanticModel.getOriginalFieldName(), semanticModel.getFieldDescription(),
				semanticModel.getOriginalDescription(), semanticModel.getFieldType(), semanticModel.getDefaultRecall(),
				semanticModel.getEnabled(), semanticModel.getDatasetId(), semanticModel.getCreateTime(),
				semanticModel.getUpdateTime());
	}

	// 批量新增智能体字段
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

	// 批量启用
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

	// 批量禁用
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

	// 获取数据集列表
	public List<String> getDataSetIds() {
		return this.jdbcTemplate.query(FIELD_GET_DATASET_IDS, (rs, rowNum) -> rs.getString("data_set_id"));
	}

	// 根据data_set_id获取智能体字段
	public List<SemanticModel> getFieldByDataSetId(String dataSetId) {
		return this.jdbcTemplate.query(FIELD_GET_BY_DATASET_IDS, new Object[] { dataSetId }, (rs, rowNum) -> {
			return new SemanticModel(rs.getObject("id", Long.class), rs.getString("data_set_id"),
					rs.getString("origin_name"), rs.getString("field_name"), rs.getString("synonyms"),
					rs.getString("description"), rs.getObject("is_recall", boolean.class),
					rs.getObject("status", boolean.class), rs.getString("type"), rs.getString("origin_description"),
					rs.getTimestamp("created_time").toLocalDateTime(),
					rs.getTimestamp("updated_time").toLocalDateTime());
		});
	}

	// 搜索
	public List<SemanticModel> searchFields(String keyword) {
		Objects.requireNonNull(keyword, "searchKeyword cannot be null");
		return jdbcTemplate.query(FIELD_SEARCH,
				new Object[] { "%" + keyword + "%", "%" + keyword + "%", "%" + keyword + "%" }, (rs, rowNum) -> {
					return new SemanticModel(rs.getObject("id", Long.class), rs.getString("data_set_id"),
							rs.getString("origin_name"), rs.getString("field_name"), rs.getString("synonyms"),
							rs.getString("description"), rs.getObject("is_recall", boolean.class),
							rs.getObject("status", boolean.class), rs.getString("type"),
							rs.getString("origin_description"), rs.getTimestamp("created_time").toLocalDateTime(),
							rs.getTimestamp("updated_time").toLocalDateTime());
				});
	}

	// 根据id删除智能体字段
	public void deleteFieldById(long id) {
		jdbcTemplate.update(FIELD_CLEAR, id);
	}

	// 更新智能体字段
	public void updateField(SemanticModelDTO semanticModelDTO, long id) {
		jdbcTemplate.update(FIELD_UPDATE, semanticModelDTO.getAgentFieldName(), semanticModelDTO.getFieldSynonyms(),
				semanticModelDTO.getOriginalFieldName(), semanticModelDTO.getFieldDescription(),
				semanticModelDTO.getOriginalDescription(), semanticModelDTO.getFieldType(),
				semanticModelDTO.getDefaultRecall(), semanticModelDTO.getEnabled(), semanticModelDTO.getDatasetId(),
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
			ps.setObject(9, field.getDatasetId()); // data_set_id
			ps.setTimestamp(10, Timestamp.valueOf(field.getCreateTime())); // created_time
			ps.setTimestamp(11, Timestamp.valueOf(field.getUpdateTime())); // updated_time
		}

		@Override
		public int getBatchSize() {
			return this.semanticModels.size();
		}
	}

}
