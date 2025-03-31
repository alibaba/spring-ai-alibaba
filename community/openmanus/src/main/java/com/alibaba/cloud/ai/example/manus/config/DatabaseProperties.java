package com.alibaba.cloud.ai.example.manus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.alibaba.cloud.ai.example.manus.config.entity.ConfigInputType;

@Component
@ConfigurationProperties(prefix = "spring")
public class DatabaseProperties {

    @ConfigProperty(
        group = "database",
        subGroup = "datasource",
        key = "url",
        path = "spring.datasource.url",
        description = "数据库连接URL",
        defaultValue = "jdbc:h2:file:./h2-data/openmanus_db;MODE=MYSQL;DATABASE_TO_LOWER=TRUE",
        inputType = ConfigInputType.TEXT
    )
    private String url;

    @ConfigProperty(
        group = "database",
        subGroup = "datasource",
        key = "username",
        path = "spring.datasource.username",
        description = "数据库用户名",
        defaultValue = "sa",
        inputType = ConfigInputType.TEXT
    )
    private String username;

    @ConfigProperty(
        group = "database",
        subGroup = "datasource",
        key = "password",
        path = "spring.datasource.password",
        description = "数据库密码",
        defaultValue = "password",
        inputType = ConfigInputType.TEXT
    )
    private String password;

    @ConfigProperty(
        group = "database",
        subGroup = "jpa",
        key = "ddl_auto",
        path = "spring.jpa.hibernate.ddl-auto",
        description = "Hibernate DDL 自动更新策略",
        defaultValue = "update",
        inputType = ConfigInputType.SELECT,
        options = {
            @ConfigOption(value = "none", label = "禁用"),
            @ConfigOption(value = "validate", label = "仅验证"),
            @ConfigOption(value = "update", label = "自动更新"),
            @ConfigOption(value = "create", label = "每次创建"),
            @ConfigOption(value = "create-drop", label = "创建后删除")
        }
    )
    private String ddlAuto;

    @ConfigProperty(
        group = "database",
        subGroup = "h2",
        key = "console_enabled",
        path = "spring.h2.console.enabled",
        description = "启用H2控制台",
        defaultValue = "true",
        inputType = ConfigInputType.BOOLEAN
    )
    private Boolean h2ConsoleEnabled;

    @ConfigProperty(
        group = "database",
        subGroup = "h2",
        key = "console_path",
        path = "spring.h2.console.path",
        description = "H2控制台路径",
        defaultValue = "/h2-console",
        inputType = ConfigInputType.TEXT
    )
    private String h2ConsolePath;

    // Getters and Setters
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDdlAuto() {
        return ddlAuto;
    }

    public void setDdlAuto(String ddlAuto) {
        this.ddlAuto = ddlAuto;
    }

    public Boolean getH2ConsoleEnabled() {
        return h2ConsoleEnabled;
    }

    public void setH2ConsoleEnabled(Boolean h2ConsoleEnabled) {
        this.h2ConsoleEnabled = h2ConsoleEnabled;
    }

    public String getH2ConsolePath() {
        return h2ConsolePath;
    }

    public void setH2ConsolePath(String h2ConsolePath) {
        this.h2ConsolePath = h2ConsolePath;
    }
}
