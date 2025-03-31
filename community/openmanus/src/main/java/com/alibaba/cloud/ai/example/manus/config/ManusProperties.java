package com.alibaba.cloud.ai.example.manus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.alibaba.cloud.ai.example.manus.config.entity.ConfigInputType;

/**
 * 注意 这里配置是依托 path 从yml里取的。
 * 
 * 不是我们传统意义上立即的 java spring 配置类的逻辑。
 * 
 * 因为我们要把内容放到数据库里。
 */
@Component
@ConfigurationProperties(prefix = "manus")
public class ManusProperties {


    @ConfigProperty(
        group = "manus",
        subGroup = "browser",
        key = "headless",
        path = "manus.browserHeadless",
        description = "浏览器无头模式",
        defaultValue = "false",
        inputType = ConfigInputType.BOOLEAN
    )
    private Boolean browserHeadless = false;

    @ConfigProperty(
        group = "manus",
        subGroup = "interaction",
        key = "openBrowser",
        path = "manus.openBrowserAuto",
        description = "启动时自动打开浏览器",
        defaultValue = "true",
        inputType = ConfigInputType.BOOLEAN
    )
    private Boolean openBrowserAuto = true;

    @ConfigProperty(
        group = "manus",
        subGroup = "interaction",
        key = "consoleQuery",
        path = "manus.consoleQuery",
        description = "启用控制台交互模式",
        defaultValue = "false",
        inputType = ConfigInputType.BOOLEAN
    )
    private Boolean consoleQuery = false;

    @ConfigProperty(
        group = "manus",
        subGroup = "agent",
        key = "maxSteps",
        path = "manus.maxSteps",
        description = "智能体执行最大步数",
        defaultValue = "20",
        inputType = ConfigInputType.NUMBER
    )
    private Integer maxSteps = 20;
    

    public Boolean getBrowserHeadless() {
        return browserHeadless;
    }

    public void setBrowserHeadless(Boolean browserHeadless) {
        this.browserHeadless = browserHeadless;
    }

    public Boolean getOpenBrowserAuto() {
        return openBrowserAuto;
    }

    public void setOpenBrowserAuto(Boolean openBrowserAuto) {
        this.openBrowserAuto = openBrowserAuto;
    }

    public Boolean getConsoleQuery() {
        return consoleQuery;
    }

    public void setConsoleQuery(Boolean consoleQuery) {
        this.consoleQuery = consoleQuery;
    }
    
    public Integer getMaxSteps() {
        return maxSteps;
    }
    
    public void setMaxSteps(Integer maxSteps) {
        this.maxSteps = maxSteps;
    }
}
