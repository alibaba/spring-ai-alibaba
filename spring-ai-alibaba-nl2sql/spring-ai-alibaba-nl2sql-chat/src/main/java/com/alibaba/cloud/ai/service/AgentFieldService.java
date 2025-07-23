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

import com.alibaba.cloud.ai.dto.AgentField;
import com.alibaba.cloud.ai.dto.schema.AgentFieldDTO;
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
public class AgentFieldService {

	private static final String FIELD_ADD = """
				INSERT INTO agent_field_config (
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
				UPDATE agent_field_config
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
				UPDATE agent_field_config SET status = 1 WHERE id = ?
			""";

	private static final String FIELD_DISABLE = """
				UPDATE agent_field_config SET status = 0 WHERE id = ?
			""";

	private static final String FIELD_GET_DATASET_IDS = """
			SELECT data_set_id FROM agent_field_config
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
			FROM agent_field_config WHERE data_set_id = ?
			""";

	private static final String FIELD_CLEAR = """
			DELETE FROM agent_field_config WHERE id = ?
			""";

	// 模糊搜素
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
			FROM agent_field_config WHERE field_name LIKE ? OR origin_name LIKE ? OR synonyms LIKE ?
			""";

	private final JdbcTemplate jdbcTemplate;

	public AgentFieldService(JdbcTemplate jdbcTemplate) {
		Assert.notNull(jdbcTemplate, "jdbcTemplate cannot be null");
		this.jdbcTemplate = jdbcTemplate;
	}

	// 新增智能体字段
	public void addField(AgentFieldDTO agentFieldDTO) {
		AgentField agentField = new AgentField();
		BeanUtils.copyProperties(agentFieldDTO, agentField);
		agentField.setCreatedTime(Timestamp.valueOf(LocalDateTime.now()));
		agentField.setUpdatedTime(Timestamp.valueOf(LocalDateTime.now()));
		jdbcTemplate.update(FIELD_ADD, agentField.getFieldName(), agentField.getSynonyms(), agentField.getOriginName(),
				agentField.getFieldDescription(), agentField.getOriginDescription(), agentField.getType(),
				agentField.getIsRecall(), agentField.getStatus(), agentField.getDataSetId(),
				agentField.getCreatedTime(), agentField.getUpdatedTime());
	}

	// 批量新增智能体字段
	public void addFields(List<AgentFieldDTO> agentFieldDTOS) {
		List<AgentField> agentFields = agentFieldDTOS.stream().map(agentFieldDTO -> {
			AgentField agentField = new AgentField();
			BeanUtils.copyProperties(agentFieldDTO, agentField);
			agentField.setCreatedTime(Timestamp.valueOf(LocalDateTime.now()));
			agentField.setUpdatedTime(Timestamp.valueOf(LocalDateTime.now()));
			return agentField;
		}).collect(Collectors.toList());
		jdbcTemplate.batchUpdate(FIELD_ADD, new AddBatchPreparedStatement(agentFields));
	}

	// 批量启用
	public void enableFields(List<Integer> ids) {
		for (int id : ids)
			jdbcTemplate.update(FIELD_ENABLE, id);
	}

	// 批量禁用
	public void disableFields(List<Integer> ids) {
		for (int id : ids)
			jdbcTemplate.update(FIELD_DISABLE, id);
	}

	// 获取数据集列表
	public List<String> getDataSetIds() {
		return this.jdbcTemplate.query(FIELD_GET_DATASET_IDS, (rs, rowNum) -> rs.getString("data_set_id"));
	}

	// 根据data_set_id获取智能体字段
	public List<AgentField> getFieldByDataSetId(String dataSetId) {
		return this.jdbcTemplate.query(FIELD_GET_BY_DATASET_IDS, new Object[] { dataSetId }, (rs, rowNum) -> {
			return new AgentField(rs.getObject("id", Integer.class), rs.getString("field_name"),
					rs.getString("synonyms"), rs.getString("origin_name"), rs.getString("description"),
					rs.getString("origin_description"), rs.getString("type"), rs.getObject("is_recall", Integer.class),
					rs.getObject("status", Integer.class), rs.getString("data_set_id"), rs.getTimestamp("created_time"),
					rs.getTimestamp("updated_time"));
		});
	}

	// 搜索
	public List<AgentField> searchFields(String keyword) {
		assert keyword != null;
		return jdbcTemplate.query(FIELD_SEARCH,
				new Object[] { "%" + keyword + "%", "%" + keyword + "%", "%" + keyword + "%" }, (rs, rowNum) -> {
					return new AgentField(rs.getObject("id", Integer.class), rs.getString("field_name"),
							rs.getString("synonyms"), rs.getString("origin_name"), rs.getString("description"),
							rs.getString("origin_description"), rs.getString("type"),
							rs.getObject("is_recall", Integer.class), rs.getObject("status", Integer.class),
							rs.getString("data_set_id"), rs.getTimestamp("created_time"),
							rs.getTimestamp("updated_time"));
				});
	}

	// 根据id删除智能体字段
	public void deleteFieldById(int id) {
		jdbcTemplate.update(FIELD_CLEAR, id);
	}

	// 更新智能体字段
	public void updateField(AgentFieldDTO agentField, int id) {
		jdbcTemplate.update(FIELD_UPDATE, agentField.getFieldName(), agentField.getSynonyms(),
				agentField.getOriginName(), agentField.getFieldDescription(), agentField.getOriginDescription(),
				agentField.getType(), agentField.getIsRecall(), agentField.getStatus(), agentField.getDataSetId(),
				Timestamp.valueOf(LocalDateTime.now()), id);
	}

	private record AddBatchPreparedStatement(List<AgentField> agentFields) implements BatchPreparedStatementSetter {

		@Override
		public void setValues(PreparedStatement ps, int i) throws SQLException {
			var field = this.agentFields.get(i);
			ps.setString(1, field.getFieldName()); // field_name
			ps.setString(2, field.getSynonyms()); // synonyms
			ps.setString(3, field.getOriginName()); // origin_name
			ps.setString(4, field.getFieldDescription()); // field_description
			ps.setString(5, field.getOriginDescription()); // origin_description
			ps.setString(6, field.getType()); // type
			ps.setObject(7, field.getIsRecall()); // is_recall
			ps.setObject(8, field.getStatus()); // status
			ps.setObject(9, field.getDataSetId()); // data_set_id
			ps.setTimestamp(10, field.getCreatedTime()); // created_time
			ps.setTimestamp(11, field.getUpdatedTime()); // updated_time
		}

		@Override
		public int getBatchSize() {
			return this.agentFields.size();
		}
	}

}
