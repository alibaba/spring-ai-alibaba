package com.alibaba.cloud.ai.example.manus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.alibaba.cloud.ai.example.manus.config.entity.ConfigInputType;

@Component
@ConfigurationProperties(prefix = "manus")
public class ManusProperties {


    @ConfigProperty(
        group = "browser",
        subGroup = "settings",
        key = "headless",
        path = "manus.browser.headless",
        description = "浏览器无头模式",
        defaultValue = "false",
        inputType = ConfigInputType.BOOLEAN
    )
    private Boolean browserHeadless = false;

    @ConfigProperty(
        group = "browser",
        subGroup = "settings",
        key = "openbrowser",
        path = "manus.openbrowser",
        description = "启动时自动打开浏览器",
        defaultValue = "true",
        inputType = ConfigInputType.BOOLEAN
    )
    private Boolean openBrowserAuto = true;

    @ConfigProperty(
        group = "browser",
        subGroup = "settings",
        key = "consolequery",
        path = "manus.consolequery",
        description = "启用控制台交互模式",
        defaultValue = "false",
        inputType = ConfigInputType.BOOLEAN
    )
    private Boolean consoleQuery = false;

    @ConfigProperty(
        group = "agent",
        subGroup = "settings",
        key = "maxSteps",
        path = "manus.agent.max-steps",
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
