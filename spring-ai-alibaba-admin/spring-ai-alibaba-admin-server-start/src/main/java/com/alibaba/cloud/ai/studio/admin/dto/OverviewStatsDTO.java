package com.alibaba.cloud.ai.studio.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OverviewStatsDTO {

    @JsonProperty("operation.count")
    private StatDetail operationCount;

    @JsonProperty("model.count")
    private StatDetail modelCount;

    @JsonProperty("usage.tokens")
    private StatDetail usageTokens;

    @Data
    @Builder
    public static class StatDetail {
        private Long total;
        private List<StatItem> detail;
    }

    @Data
    @Builder
    public static class StatItem {
        private String operationName;
        private String modelName;
        private Long total;
    }
}