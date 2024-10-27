package com.alibaba.cloud.ai.dashscope.rag;

/**
 * @author HeYQ
 * @version 1.0
 * @date 2024-10-23 20:22
 * @describe
 */
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticdbConfig {

    private String accessKeyId;

    private String accessKeySecret;

    private String regionId;

    private String DBInstanceId;

    private String managerAccount;

    private String managerAccountPassword;

    private String namespace;

    private String namespacePassword;

    private String metrics = "cosine";

    private Integer readTimeout = 60000;

    private Long embeddingDimension = 1536L;

    public AnalyticdbConfig() {

    }


    public AnalyticdbConfig(String accessKeyId, String accessKeySecret, String regionId, String DBInstanceId,
                            String managerAccount, String managerAccountPassword,
                            String namespace, String namespacePassword,
                            String metrics, Integer readTimeout, Long embeddingDimension) {
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.regionId = regionId;
        this.DBInstanceId = DBInstanceId;
        this.managerAccount = managerAccount;
        this.managerAccountPassword = managerAccountPassword;
        this.namespace = namespace;
        this.namespacePassword = namespacePassword;
        this.metrics = metrics;
        this.readTimeout = readTimeout;
        this.embeddingDimension = embeddingDimension;
    }


    public Map<String, Object> toAnalyticdbClientParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("accessKeyId", this.accessKeyId);
        params.put("accessKeySecret", this.accessKeySecret);
        params.put("regionId", this.regionId);
        params.put("readTimeout", this.readTimeout);
        return params;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public String getDBInstanceId() {
        return DBInstanceId;
    }

    public void setDBInstanceId(String DBInstanceId) {
        this.DBInstanceId = DBInstanceId;
    }

    public String getManagerAccount() {
        return managerAccount;
    }

    public void setManagerAccount(String managerAccount) {
        this.managerAccount = managerAccount;
    }

    public String getManagerAccountPassword() {
        return managerAccountPassword;
    }

    public void setManagerAccountPassword(String managerAccountPassword) {
        this.managerAccountPassword = managerAccountPassword;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getNamespacePassword() {
        return namespacePassword;
    }

    public void setNamespacePassword(String namespacePassword) {
        this.namespacePassword = namespacePassword;
    }

    public String getMetrics() {
        return metrics;
    }

    public void setMetrics(String metrics) {
        this.metrics = metrics;
    }

    public Integer getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Integer readTimeout) {
        this.readTimeout = readTimeout;
    }

    public Long getEmbeddingDimension() {
        return embeddingDimension;
    }

    public void setEmbeddingDimension(Long embeddingDimension) {
        this.embeddingDimension = embeddingDimension;
    }
}
