package com.alibaba.cloud.ai.studio.admin.enums;

public enum ExperimentStatus {

    /**
     * 草稿
     */
    DRAFT("DRAFT", "草稿"),

    /**
     * 运行中
     */
    RUNNING("RUNNING", "运行中"),

    /**
     * 已完成
     */
    COMPLETED("COMPLETED", "已完成"),

    /**
     * 失败
     */
    FAILED("FAILED", "失败"),

    /**
     * 已停止
     */
    STOPPED("STOPPED", "已停止");

    private final String code;
    private final String description;

    ExperimentStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ExperimentStatus fromCode(String code) {
        for (ExperimentStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown experiment status code: " + code);
    }
} 