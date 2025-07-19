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

import com.alibaba.cloud.ai.schema.AgentField;
import com.alibaba.cloud.ai.schema.AgentFieldDTO;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Service
public class AgentFieldService {

	private static final String FIELD_GET_IDS = """
			SELECT id FROM agent_field_config
			""";

	private static final String FIELD_ADD = """
			INSERT INTO agent_field_config (field_name, synonyms, field_description) VALUES (?, ?, ?)
			""";

	private static final String FIELD_GET_BY_ID = """
			SELECT field_name, synonyms, field_description FROM agent_field_config WHERE id = ?
			""";

	private static final String FIELD_UPDATE = """
			UPDATE agent_field_config SET field_name = ?, synonyms = ?, field_description = ? WHERE id = ?
			""";

	private static final String FIELD_GET_DATA = """
			SELECT id, field_name, synonyms, field_description FROM agent_field_config
			""";

	private static final String FIELD_CLEAR = """
			DELETE FROM agent_field_config WHERE id = ?
			""";

	private final JdbcTemplate jdbcTemplate;

	public AgentFieldService(JdbcTemplate jdbcTemplate) {
		Assert.notNull(jdbcTemplate, "jdbcTemplate cannot be null");
		this.jdbcTemplate = jdbcTemplate;
	}

	public void addFields(List<AgentFieldDTO> agentFields) {
		jdbcTemplate.batchUpdate(FIELD_ADD, new AddBatchPreparedStatement(agentFields));
	}

	public List<String> getFieldIds() {
		return this.jdbcTemplate.query(FIELD_GET_IDS, (rs, rowNum) -> rs.getString("id"));
	}

	public AgentField getFieldById(Integer id) {
		return jdbcTemplate.query(FIELD_GET_BY_ID, new Object[] { id }, (rs, rowNum) -> {
			return new AgentField(id, rs.getString("field_name"), rs.getString("synonyms"),
					rs.getString("field_description"));
		}).stream().findFirst().orElse(null);
	}

	public void deleteFieldById(String id) {
		jdbcTemplate.update(FIELD_CLEAR, id);
	}

	public void updateField(AgentFieldDTO agentField, String id) {
		jdbcTemplate.update(FIELD_UPDATE, agentField.getFieldName(), agentField.getSynonyms(),
				agentField.getFieldDescription(), id);
	}

	public List<AgentField> getAllFields() {
		return jdbcTemplate.query(FIELD_GET_DATA, (rs, rowNum) -> {
			return new AgentField(rs.getInt("id"), rs.getString("field_name"), rs.getString("synonyms"),
					rs.getString("field_description"));
		});
	}

	private record AddBatchPreparedStatement(List<AgentFieldDTO> agentFields) implements BatchPreparedStatementSetter {

		@Override
		public void setValues(PreparedStatement ps, int i) throws SQLException {
			var field = this.agentFields.get(i);
			ps.setString(1, field.getFieldName());
			ps.setString(2, field.getSynonyms());
			ps.setString(3, field.getFieldDescription());
		}

		@Override
		public int getBatchSize() {
			return this.agentFields.size();
		}
	}

}
