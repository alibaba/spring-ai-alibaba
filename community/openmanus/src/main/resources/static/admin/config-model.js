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
        try {
            const response = await fetch('/api/admin/config');
            if (response.ok) {
                const loadedConfig = await response.json();
                this.config = this._mergeConfigs(this._deepCopy(DEFAULT_CONFIG), loadedConfig);
            } else {
                console.warn('无法加载配置，使用默认配置');
            }
        } catch (error) {
            console.error('加载配置出错:', error);
        }
        
        this.loaded = true;
        return this.config;
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
     * 保存配置
     */
    async saveConfig() {
        try {
            const response = await fetch('/api/admin/config', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(this.config)
            });
            
            if (response.ok) {
                return { success: true, message: '配置保存成功' };
            } else {
                const error = await response.json();
                return { success: false, message: `保存失败: ${error.message || '未知错误'}` };
            }
        } catch (error) {
            console.error('保存配置出错:', error);
            return { success: false, message: `保存出错: ${error.message || '未知错误'}` };
        }
    }
    
    /**
     * 更新配置中的特定部分
     */
    updateConfig(section, key, value) {
        if (this.config.hasOwnProperty(section)) {
            if (key.includes('.')) {
                // 处理嵌套属性，如 performance.maxThreads
                const keys = key.split('.');
                let current = this.config[section];
                
                for (let i = 0; i < keys.length - 1; i++) {
                    current = current[keys[i]];
                }
                
                current[keys[keys.length - 1]] = value;
            } else {
                this.config[section][key] = value;
            }
            return true;
        }
        return false;
    }
    
    /**
     * 获取配置值
     */
    getConfig(section, key = null) {
        if (!this.config.hasOwnProperty(section)) {
            return null;
        }
        
        if (key === null) {
            return this.config[section];
        }
        
        if (key.includes('.')) {
            // 处理嵌套属性
            const keys = key.split('.');
            let current = this.config[section];
            
            for (const k of keys) {
                if (!current.hasOwnProperty(k)) {
                    return null;
                }
                current = current[k];
            }
            
            return current;
        }
        
        return this.config[section][key];
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
