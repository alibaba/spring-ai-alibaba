package com.alibaba.cloud.ai.studio.admin.dto;

import com.alibaba.cloud.ai.studio.admin.entity.PromptDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Prompt {

    /**
     * Prompt名称
     */
    private String promptKey;

    /**
     * Prompt描述
     */
    private String promptDescription;

    /**
     * 最新版本
     */
    private String latestVersion;

    /**
     * 最新版本状态：pre-预发布版本，release-正式版本
     */
    private String latestVersionStatus;

    /**
     * 标签，逗号分隔
     */
    private String tags;

    /**
     * Prompt创建时间，时间戳毫秒
     */
    private Long createTime;

    /**
     * Prompt变更时间，时间戳毫秒
     */
    private Long updateTime;

    /**
     * 从DO转换为DTO
     */
    public static Prompt fromDO(PromptDO promptDO) {
        return fromDO(promptDO, null);
    }

    /**
     * 从DO转换为DTO，包含最新版本状态
     */
    public static Prompt fromDO(PromptDO promptDO, String latestVersionStatus) {
        if (promptDO == null) {
            return null;
        }
        return Prompt.builder()
                .promptKey(promptDO.getPromptKey())
                .promptDescription(promptDO.getPromptDesc())
                .latestVersion(promptDO.getLatestVersion())
                .latestVersionStatus(latestVersionStatus)
                .tags(promptDO.getTags())
                .createTime(promptDO.getCreateTime() != null ? 
                    promptDO.getCreateTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : null)
                .updateTime(promptDO.getUpdateTime() != null ? 
                    promptDO.getUpdateTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : null)
                .build();
    }
}
