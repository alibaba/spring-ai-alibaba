/**
 * config-model.js - 配置数据模型
 * 负责管理、加载和保存配置数据
 */

// 默认配置
const DEFAULT_CONFIG = {
    // 基础配置
    basic: {
        systemName: "JTaskPilot",
        language: "zh-CN",
        performance: {
            maxThreads: 8,
            timeoutSeconds: 60
        }
    },
    
    // Agent配置
    agent: {
        defaultType: "assistant",
        timeout: 120,
        maxTurns: 10,
        systemPrompt: "您是一个智能助手，可以回答问题并执行任务。"
    },
    
    // Tool配置
    tool: {
        enabled: {
            search: true,
            calculator: true,
            weather: false
        },
        apiKeys: {
            weather: ""
        }
    },
    
    // MCP配置
    mcp: {
        endpoint: "http://localhost:8080/mcp",
        version: "1.2",
        defaultModel: "qwen-turbo",
        customModelUrl: ""
    }
};

// 配置管理器
class ConfigModel {
    constructor() {
        this.config = this._deepCopy(DEFAULT_CONFIG);
        this.loaded = false;
        
        // 存储按组加载的配置
        this.groupConfigs = {};
    }
    
    /**
     * 深拷贝对象
     */
    _deepCopy(obj) {
        return JSON.parse(JSON.stringify(obj));
    }
    
    /**
     * 根据配置组名加载配置
     * @param {string} groupName - 配置组名称 如 "manus"
     * @returns {Promise<Array>} - 配置项数组
     */
    async loadConfigByGroup(groupName) {
        try {
            const configs = await AdminAPI.getConfigsByGroup(groupName);
            // 缓存该组的配置
            this.groupConfigs[groupName] = configs;
            return configs;
        } catch (error) {
            console.error(`加载${groupName}组配置出错:`, error);
            return [];
        }
    }
    
    /**
     * 保存特定组的配置
     * @param {string} groupName - 配置组名称
     * @returns {Promise<Object>} - 保存结果
     */
    async saveGroupConfig(groupName) {
        if (!this.groupConfigs[groupName] || this.groupConfigs[groupName].length === 0) {
            return { success: false, message: `没有加载${groupName}组的配置` };
        }
        
        // 获取已修改的配置项
        const configsToUpdate = this.groupConfigs[groupName].filter(config => 
            config.configGroup === groupName && config._modified === true);
            
        if (configsToUpdate.length === 0) {
            return { success: true, message: '没有需要保存的修改' };
        }
        
        try {
            return await AdminAPI.batchUpdateConfigs(configsToUpdate, groupName);
        } catch (error) {
            console.error(`保存${groupName}组配置出错:`, error);
            return { success: false, message: `保存出错: ${error.message || '未知错误'}` };
        }
    }
    
    /**
     * 更新特定组中配置项的值
     * @param {string} groupName - 配置组名
     * @param {number} configId - 配置项ID
     * @param {string} newValue - 新值
     * @returns {boolean} - 更新是否成功
     */
    updateGroupConfigValue(groupName, configId, newValue) {
        if (!this.groupConfigs[groupName]) {
            return false;
        }
        
        const configIndex = this.groupConfigs[groupName].findIndex(item => item.id === configId);
        if (configIndex === -1) {
            return false;
        }
        
        // 检查值是否实际变化
        const config = this.groupConfigs[groupName][configIndex];
        if (config.configValue !== newValue) {
            config.configValue = newValue;
            // 标记为已修改
            config._modified = true;
        }
        
        return true;
    }
    
    /**
     * 合并配置，确保所有默认属性都存在
     */
    _mergeConfigs(defaultConfig, loadedConfig) {
        const merged = this._deepCopy(defaultConfig);
        
        for (const section in loadedConfig) {
            if (merged.hasOwnProperty(section)) {
                if (typeof loadedConfig[section] === 'object' && loadedConfig[section] !== null) {
                    merged[section] = this._mergeConfigSection(merged[section], loadedConfig[section]);
                } else {
                    merged[section] = loadedConfig[section];
                }
            }
        }
        
        return merged;
    }
    
    /**
     * 递归合并配置节
     */
    _mergeConfigSection(defaultSection, loadedSection) {
        const merged = this._deepCopy(defaultSection);
        
        for (const key in loadedSection) {
            if (merged.hasOwnProperty(key)) {
                if (typeof loadedSection[key] === 'object' && loadedSection[key] !== null) {
                    merged[key] = this._mergeConfigSection(merged[key], loadedSection[key]);
                } else {
                    merged[key] = loadedSection[key];
                }
            }
        }
        
        return merged;
    }
    
    /**
     * 重置为默认配置
     */
    resetToDefault() {
        this.config = this._deepCopy(DEFAULT_CONFIG);
        return this.config;
    }
}

// 创建全局配置实例
const configModel = new ConfigModel();
