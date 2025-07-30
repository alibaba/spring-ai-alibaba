package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.entity.AgentPresetQuestion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Service
public class AgentPresetQuestionService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String SELECT_BY_AGENT_ID = """
            SELECT * FROM agent_preset_question 
            WHERE agent_id = ? AND is_active = 1 
            ORDER BY sort_order ASC, id ASC
            """;

    private static final String INSERT = """
            INSERT INTO agent_preset_question (agent_id, question, sort_order, is_active, create_time, update_time)
            VALUES (?, ?, ?, ?, NOW(), NOW())
            """;

    private static final String UPDATE = """
            UPDATE agent_preset_question 
            SET question = ?, sort_order = ?, is_active = ?, update_time = NOW()
            WHERE id = ?
            """;

    private static final String DELETE = """
            DELETE FROM agent_preset_question WHERE id = ?
            """;

    private static final String DELETE_BY_AGENT_ID = """
            DELETE FROM agent_preset_question WHERE agent_id = ?
            """;

    /**
     * 根据智能体ID获取预设问题列表
     */
    public List<AgentPresetQuestion> findByAgentId(Long agentId) {
        return jdbcTemplate.query(SELECT_BY_AGENT_ID, new BeanPropertyRowMapper<>(AgentPresetQuestion.class), agentId);
    }

    /**
     * 创建预设问题
     */
    public AgentPresetQuestion create(AgentPresetQuestion question) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, question.getAgentId());
            ps.setString(2, question.getQuestion());
            ps.setInt(3, question.getSortOrder() != null ? question.getSortOrder() : 0);
            ps.setBoolean(4, question.getIsActive() != null ? question.getIsActive() : true);
            return ps;
        }, keyHolder);
        question.setId(keyHolder.getKey().longValue());
        return question;
    }

    /**
     * 更新预设问题
     */
    public void update(Long id, AgentPresetQuestion question) {
        jdbcTemplate.update(UPDATE, 
            question.getQuestion(), 
            question.getSortOrder(), 
            question.getIsActive(), 
            id);
    }

    /**
     * 删除预设问题
     */
    public void deleteById(Long id) {
        jdbcTemplate.update(DELETE, id);
    }

    /**
     * 删除智能体的所有预设问题
     */
    public void deleteByAgentId(Long agentId) {
        jdbcTemplate.update(DELETE_BY_AGENT_ID, agentId);
    }

    /**
     * 批量保存预设问题（先删除再插入）
     */
    public void batchSave(Long agentId, List<AgentPresetQuestion> questions) {
        // 先删除该智能体的所有预设问题
        deleteByAgentId(agentId);
        // 批量插入新的预设问题
        for (int i = 0; i < questions.size(); i++) {
            AgentPresetQuestion question = questions.get(i);
            question.setAgentId(agentId);
            question.setSortOrder(i);
            question.setIsActive(true);
            create(question);
        }
    }
}