/**
 * config-model.js - 配置数据模型管理
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

class ConfigModel {
    constructor() {
        this.config = this._deepCopy(DEFAULT_CONFIG);
        this.loaded = false;
        this.originalConfig = null;
        
        // 存储按组加载的配置
        this.groupConfigs = {};
        
        // 缓存配置变更
        this.pendingChanges = new Set();
    }

    /**
     * 深拷贝对象
     */
    _deepCopy(obj) {
        return JSON.parse(JSON.stringify(obj));
    }

    /**
     * 加载配置
     */
    async loadConfig() {
        if (this.loaded) return this.config;

        try {
            // 加载配置
            const loadedBasicConfig = await AdminAPI.getConfigsByGroup('basic');
            const loadedAgentConfig = await AdminAPI.getConfigsByGroup('agent');
            
            // 合并配置
            this._mergeLoadedConfig('basic', loadedBasicConfig);
            this._mergeLoadedConfig('agent', loadedAgentConfig);
            
            // 保存原始配置用于对比变更
            this.originalConfig = this._deepCopy(this.config);
            this.loaded = true;
            
            return this.config;
        } catch (error) {
            console.error('加载配置失败:', error);
            return this.config;
        }
    }

    /**
     * 合并加载的配置到当前配置
     */
    _mergeLoadedConfig(section, loadedConfigs) {
        if (!loadedConfigs || loadedConfigs.length === 0) return;

        loadedConfigs.forEach(config => {
            const path = config.configKey.split('.');
            let current = this.config[section];
            
            // 遍历配置路径
            for (let i = 0; i < path.length - 1; i++) {
                if (!current[path[i]]) {
                    current[path[i]] = {};
                }
                current = current[path[i]];
            }
            
            // 设置最终值
            const lastKey = path[path.length - 1];
            current[lastKey] = this._parseConfigValue(config.configValue, config.inputType);
        });
    }

    /**
     * 解析配置值
     */
    _parseConfigValue(value, type) {
        switch (type) {
            case 'BOOLEAN':
                return value === 'true';
            case 'NUMBER':
                return parseFloat(value);
            default:
                return value;
        }
    }

    /**
     * 更新配置值
     */
    updateValue(section, path, value) {
        const keys = path.split('.');
        let current = this.config[section];
        
        // 遍历到最后一个键之前
        for (let i = 0; i < keys.length - 1; i++) {
            if (!current[keys[i]]) {
                current[keys[i]] = {};
            }
            current = current[keys[i]];
        }
        
        // 设置最终值
        const lastKey = keys[keys.length - 1];
        if (current[lastKey] !== value) {
            current[lastKey] = value;
            this.pendingChanges.add(`${section}.${path}`);
        }
    }

    /**
     * 获取已修改的配置项
     */
    getModifiedConfigs() {
        const modified = [];
        this.pendingChanges.forEach(path => {
            const [section, ...rest] = path.split('.');
            const configPath = rest.join('.');
            const value = this._getValueByPath(this.config[section], configPath);
            
            modified.push({
                configGroup: section,
                configKey: configPath,
                configValue: String(value)
            });
        });
        return modified;
    }

    /**
     * 根据路径获取值
     */
    _getValueByPath(obj, path) {
        return path.split('.').reduce((current, key) => current[key], obj);
    }

    /**
     * 保存配置
     */
    async saveConfig() {
        if (this.pendingChanges.size === 0) {
            return { success: true, message: '没有需要保存的修改' };
        }

        try {
            const modifiedConfigs = this.getModifiedConfigs();
            const result = await AdminAPI.batchUpdateConfigs(modifiedConfigs);
            
            if (result.success) {
                // 更新原始配置
                this.originalConfig = this._deepCopy(this.config);
                this.pendingChanges.clear();
            }
            
            return result;
        } catch (error) {
            console.error('保存配置失败:', error);
            return { success: false, message: error.message || '保存失败' };
        }
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
        this.loaded = false;
        this.originalConfig = null;
        this.pendingChanges.clear();
        // 清空组配置
        this.groupConfigs = {};
        return this.config;
    }

    /**
     * 检查是否有未保存的更改
     */
    hasUnsavedChanges() {
        // 检查对象模式的配置变更
        if (this.pendingChanges.size > 0) {
            return true;
        }
        
        // 检查按组模式的配置变更
        for (const groupName in this.groupConfigs) {
            const configs = this.groupConfigs[groupName];
            if (configs.some(config => config._modified === true)) {
                return true;
            }
        }
        
        return false;
    }
}

// 创建全局配置实例
window.configModel = new ConfigModel();

// Agent 配置模型
class AgentConfigModel {
    constructor() {
        this.agents = [];
        this.currentAgent = null;
        this.availableTools = [];
    }

    async loadAgents() {
        try {
            this.agents = await AdminAPI.getAllAgents();
            return this.agents;
        } catch (error) {
            console.error('加载Agent列表失败:', error);
            throw error;
        }
    }

    async loadAgentDetails(id) {
        try {
            this.currentAgent = await AdminAPI.getAgentById(id);
            return this.currentAgent;
        } catch (error) {
            console.error('加载Agent详情失败:', error);
            throw error;
        }
    }

    async loadAvailableTools() {
        try {
            this.availableTools = await AdminAPI.getAvailableTools();
            return this.availableTools;
        } catch (error) {
            console.error('加载可用工具列表失败:', error);
            throw error;
        }
    }

    async saveAgent(agentData, isImport = false) {
        try {
            // 如果是导入操作，移除ID以强制创建新Agent
            if (isImport) {
                const importData = { ...agentData };
                delete importData.id;
                return await AdminAPI.createAgent(importData);
            }
            
            // 正常保存逻辑
            if (agentData.id) {
                return await AdminAPI.updateAgent(agentData.id, agentData);
            } else {
                return await AdminAPI.createAgent(agentData);
            }
        } catch (error) {
            console.error('保存Agent失败:', error);
            throw error;
        }
    }

    async deleteAgent(id) {
        try {
            await AdminAPI.deleteAgent(id);
            this.agents = this.agents.filter(agent => agent.id !== id);
        } catch (error) {
            console.error('删除Agent失败:', error);
            throw error;
        }
    }
}

// 创建全局Agent配置模型实例
window.agentConfigModel = new AgentConfigModel();
