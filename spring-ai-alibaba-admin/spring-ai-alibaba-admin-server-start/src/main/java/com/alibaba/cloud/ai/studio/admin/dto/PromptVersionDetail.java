package com.alibaba.cloud.ai.studio.admin.dto;

import com.alibaba.cloud.ai.studio.admin.entity.PromptVersionDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptVersionDetail {

    /**
     * 版本号
     */
    private String version;

    /**
     * Prompt名称
     */
    private String promptKey;

    /**
     * Prompt版本描述
     */
    private String versionDescription;

    /**
     * Prompt内容
     */
    private String template;

    /**
     * Prompt中的变量值，JSON
     */
    private String variables;

    /**
     * 使用的模型相关参数，JSON
     */
    private String modelConfig;

    /**
     * Prompt版本创建时间，时间戳毫秒
     */
    private Long createTime;

    /**
     * 前置版本
     */
    private String previousVersion;

    /**
     * 版本状态：pre-预发布版本，release-正式版本
     */
    private String status;

    /**
     * 从DO转换为DTO
     */
    public static PromptVersionDetail fromDO(PromptVersionDO promptVersionDO) {
        if (promptVersionDO == null) {
            return null;
        }
                return PromptVersionDetail.builder()
                .version(promptVersionDO.getVersion())
                .promptKey(promptVersionDO.getPromptKey())
                .versionDescription(promptVersionDO.getVersionDesc())
                .template(promptVersionDO.getTemplate())
                .variables(promptVersionDO.getVariables())
                .modelConfig(promptVersionDO.getModelConfig())
                .createTime(promptVersionDO.getCreateTime() != null ?
                    promptVersionDO.getCreateTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : null)
                .previousVersion(promptVersionDO.getPreviousVersion())
                .status(promptVersionDO.getStatus())
                .build();
    }
}
