package com.alibaba.cloud.ai.studio.admin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "prompt_version")
public class PromptVersionDO {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 版本号
     */
    private String version;

    /**
     * Prompt Key
     */
    private String promptKey;

    /**
     * 版本描述
     */
    private String versionDesc;

    /**
     * Prompt模版内容
     */
    @Column(columnDefinition = "LONGTEXT")
    private String template;

    /**
     * Prompt模版里的可变参数，JSON格式
     */
    private String variables;

    /**
     * 调试该prompt的模型参数，JSON格式
     */
    private String modelConfig;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 前置版本，用于对比
     */
    private String previousVersion;

    /**
     * 版本状态：pre-预发布版本，release-正式版本
     */
    @Builder.Default
    private String status = "pre";
}
