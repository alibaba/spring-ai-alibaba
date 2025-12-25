package com.alibaba.cloud.ai.studio.admin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "prompt_build_template")
public class PromptTemplateDO {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Prompt模板Key
     */
    private String promptTemplateKey;

    /**
     * 标签，逗号分隔
     */
    private String tags;

    /**
     * 模板描述
     */
    private String templateDesc;

    /**
     * Prompt模版内容
     */
    @Column(columnDefinition = "LONGTEXT")
    private String template;

    /**
     * Prompt模版里的可变参数
     */
    private String variables;

    /**
     * 推荐使用的模型参数，JSON格式
     */
    @Column(columnDefinition = "TEXT")
    private String modelConfig;
}
