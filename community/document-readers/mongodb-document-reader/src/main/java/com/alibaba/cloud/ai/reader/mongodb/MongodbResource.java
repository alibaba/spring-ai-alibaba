package com.alibaba.cloud.ai.reader.mongodb;

import lombok.Builder;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;


/**
 * MongoDB文档读取器配置属性类
 * 用于配置MongoDB连接、文档处理和性能相关的参数
 *
 * @author 驰恩
 * @version 1.0.0
 */
@Builder
public class MongodbResource implements Resource {

    /**
     * MongoDB连接URI
     * 格式: mongodb://[username:password@]host1[:port1][,host2[:port2],...][/database][?options]
     * 默认值: mongodb://localhost:27017
     */
    private String uri;


    /**
     * MongoDB用户名
     * 可选项，用于身份验证
     */
    private String username;

    /**
     * MongoDB密码
     * 可选项，用于身份验证
     */
    private String password;

    /**
     * MongoDB数据库名称
     * 必填项，指定要连接的数据库
     */
    private String database;

    /**
     * MongoDB集合名称
     * 必填项，指定要读取的集合
     */
    private String collection;

    /**
     * MongoDB查询条件（JSON格式）
     * 可选项，用于过滤要读取的文档
     * 示例: {"status": "active", "type": "article"}
     */
    private String query;

    /**
     * 文档分块大小（字符数）
     * 用于将大文档分割成小块进行处理
     * 默认值: 1000字符
     */
    @Builder.Default
    private int chunkSize = 1000;

    /**
     * 分块重叠大小（字符数）
     * 相邻分块之间的重叠字符数，用于保持上下文连贯性
     * 默认值: 200字符
     */
    @Builder.Default
    private int overlap = 200;

    /**
     * 是否启用向量化处理
     * 如果为true，将处理文档中的向量字段
     * 默认值: false
     */
    @Builder.Default
    private boolean enableVectorization = false;

    /**
     * 向量维度
     * 文档向量化时的维度大小
     * 默认值: 1536 (适用于OpenAI的text-embedding-ada-002模型)
     */
    @Builder.Default
    private int vectorDimensions = 1536;

    /**
     * 向量字段名
     * MongoDB文档中存储向量的字段名
     * 默认值: "vector"
     */
    @Builder.Default
    private String vectorField = "vector";

    /**
     * 批处理大小
     * 每次从MongoDB批量读取的文档数量
     * 默认值: 100
     */
    @Builder.Default
    private int batchSize = 100;

    /**
     * MongoDB连接池大小
     * 默认值: 10
     */
    @Builder.Default
    private int poolSize = 10;

    /**
     * MongoDB连接超时时间（毫秒）
     * 默认值: 5000ms (5秒)
     */
    @Builder.Default
    private int connectTimeout = 5000;

    // Getters and Setters

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getOverlap() {
        return overlap;
    }

    public void setOverlap(int overlap) {
        this.overlap = overlap;
    }

    public boolean isEnableVectorization() {
        return enableVectorization;
    }

    public void setEnableVectorization(boolean enableVectorization) {
        this.enableVectorization = enableVectorization;
    }

    public int getVectorDimensions() {
        return vectorDimensions;
    }

    public void setVectorDimensions(int vectorDimensions) {
        this.vectorDimensions = vectorDimensions;
    }

    public String getVectorField() {
        return vectorField;
    }

    public void setVectorField(String vectorField) {
        this.vectorField = vectorField;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public URL getURL() throws IOException {
        return null;
    }

    @Override
    public URI getURI() throws IOException {
        return null;
    }

    @Override
    public File getFile() throws IOException {
        return null;
    }

    @Override
    public long contentLength() throws IOException {
        return 0;
    }

    @Override
    public long lastModified() throws IOException {
        return 0;
    }

    @Override
    public Resource createRelative(String relativePath) throws IOException {
        return null;
    }

    @Override
    public String getFilename() {
        return "";
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return null;
    }
}