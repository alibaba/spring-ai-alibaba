package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.entity.AgentPresetQuestion;
import com.alibaba.cloud.ai.service.AgentPresetQuestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
@CrossOrigin(origins = "*")
public class AgentPresetQuestionController {

    private static final Logger logger = LoggerFactory.getLogger(AgentPresetQuestionController.class);

    @Autowired
    private AgentPresetQuestionService presetQuestionService;

    /**
     * 获取智能体的预设问题列表
     */
    @GetMapping("/{agentId}/preset-questions")
    public ResponseEntity<List<AgentPresetQuestion>> getPresetQuestions(@PathVariable Long agentId) {
        try {
            List<AgentPresetQuestion> questions = presetQuestionService.findByAgentId(agentId);
            return ResponseEntity.ok(questions);
        } catch (Exception e) {
            logger.error("Error getting preset questions for agent {}", agentId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 批量保存智能体的预设问题
     */
    @PostMapping("/{agentId}/preset-questions")
    public ResponseEntity<Map<String, String>> savePresetQuestions(
            @PathVariable Long agentId,
            @RequestBody List<Map<String, String>> questionsData) {
        try {
            // 转换为实体对象
            List<AgentPresetQuestion> questions = questionsData.stream()
                .map(data -> {
                    AgentPresetQuestion question = new AgentPresetQuestion();
                    question.setQuestion(data.get("question"));
                    return question;
                })
                .toList();

            presetQuestionService.batchSave(agentId, questions);
            return ResponseEntity.ok(Map.of("message", "预设问题保存成功"));
        } catch (Exception e) {
            logger.error("Error saving preset questions for agent {}", agentId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "保存预设问题失败: " + e.getMessage()));
        }
    }

    /**
     * 删除预设问题
     */
    @DeleteMapping("/{agentId}/preset-questions/{questionId}")
    public ResponseEntity<Map<String, String>> deletePresetQuestion(
            @PathVariable Long agentId,
            @PathVariable Long questionId) {
        try {
            presetQuestionService.deleteById(questionId);
            return ResponseEntity.ok(Map.of("message", "预设问题删除成功"));
        } catch (Exception e) {
            logger.error("Error deleting preset question {} for agent {}", questionId, agentId, e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "删除预设问题失败: " + e.getMessage()));
        }
    }
}