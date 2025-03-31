/**
 * i18n.js - 国际化支持
 * 处理多语言文本显示和切换
 */

// 默认语言
const DEFAULT_LANGUAGE = 'zh-CN';

// 支持的语言
const SUPPORTED_LANGUAGES = ['zh-CN', 'en-US'];

// 翻译文本
const translations = {
    'zh-CN': {
        // 通用
        'save': '保存',
        'cancel': '取消',
        'confirm': '确认',
        'back_to_main': '返回主页',
        'loading': '加载中...',
        'success': '成功',
        'error': '错误',
        'warning': '警告',
        
        // 页面标题
        'admin_console': 'JTaskPilot 管理控制台',
        'save_config': '保存配置',
        
        // 配置类别
        'basic_config': '基础配置',
        'agent_config': 'Agent配置',
        'tool_config': 'Tool配置',
        'mcp_config': 'MCP配置',
        
        // 基础配置
        'system_settings': '系统设置',
        'system_name': '系统名称',
        'default_language': '默认语言',
        'performance_settings': '性能设置',
        'max_threads': '最大线程数',
        'timeout_seconds': '请求超时时间(秒)',
        
        // Agent配置
        'agent_basic_settings': 'Agent基本设置',
        'default_agent_type': '默认Agent类型',
        'agent_timeout': 'Agent响应超时(秒)',
        'agent_advanced_settings': 'Agent高级设置',
        'max_turns': '最大对话轮次',
        'system_prompt': '系统提示词',
        
        // Tool配置
        'tool_enable_settings': '工具启用设置',
        'enable_search_tool': '启用搜索工具',
        'enable_calculator_tool': '启用计算器工具',
        'enable_weather_tool': '启用天气工具',
        'api_settings': 'API配置',
        'weather_api_key': '天气API密钥',
        'test_connection': '测试连接',
        
        // MCP配置
        'mcp_connection_settings': 'MCP连接设置',
        'mcp_endpoint': 'MCP服务端点',
        'mcp_version': 'MCP协议版本',
        'model_settings': '模型配置',
        'default_llm_model': '默认LLM模型',
        'custom_model_url': '自定义模型URL',
        
        // 选项值
        'general_assistant': '通用助手',
        'programmer': '编程专家',
        'custom': '自定义',
        
        // 消息
        'config_saved': '配置已成功保存',
        'config_save_failed': '配置保存失败',
        'reset_confirm': '确定要重置所有配置为默认值吗？',
        'changes_not_saved': '您有未保存的更改，确定要离开吗？'
    },
    
    'en-US': {
        // 通用
        'save': 'Save',
        'cancel': 'Cancel',
        'confirm': 'Confirm',
        'back_to_main': 'Back to Main',
        'loading': 'Loading...',
        'success': 'Success',
        'error': 'Error',
        'warning': 'Warning',
        
        // 页面标题
        'admin_console': 'JTaskPilot Admin Console',
        'save_config': 'Save Config',
        
        // 配置类别
        'basic_config': 'Basic Config',
        'agent_config': 'Agent Config',
        'tool_config': 'Tool Config',
        'mcp_config': 'MCP Config',
        
        // 基础配置
        'system_settings': 'System Settings',
        'system_name': 'System Name',
        'default_language': 'Default Language',
        'performance_settings': 'Performance Settings',
        'max_threads': 'Max Threads',
        'timeout_seconds': 'Request Timeout (sec)',
        
        // Agent配置
        'agent_basic_settings': 'Agent Basic Settings',
        'default_agent_type': 'Default Agent Type',
        'agent_timeout': 'Agent Response Timeout (sec)',
        'agent_advanced_settings': 'Agent Advanced Settings',
        'max_turns': 'Max Conversation Turns',
        'system_prompt': 'System Prompt',
        
        // Tool配置
        'tool_enable_settings': 'Tool Enable Settings',
        'enable_search_tool': 'Enable Search Tool',
        'enable_calculator_tool': 'Enable Calculator Tool',
        'enable_weather_tool': 'Enable Weather Tool',
        'api_settings': 'API Settings',
        'weather_api_key': 'Weather API Key',
        'test_connection': 'Test Connection',
        
        // MCP配置
        'mcp_connection_settings': 'MCP Connection Settings',
        'mcp_endpoint': 'MCP Service Endpoint',
        'mcp_version': 'MCP Protocol Version',
        'model_settings': 'Model Settings',
        'default_llm_model': 'Default LLM Model',
        'custom_model_url': 'Custom Model URL',
        
        // 选项值
        'general_assistant': 'General Assistant',
        'programmer': 'Programming Expert',
        'custom': 'Custom',
        
        // 消息
        'config_saved': 'Configuration saved successfully',
        'config_save_failed': 'Failed to save configuration',
        'reset_confirm': 'Are you sure you want to reset all configuration to default?',
        'changes_not_saved': 'You have unsaved changes. Are you sure you want to leave?'
    }
};

// i18n类
class I18n {
    constructor() {
        this.currentLanguage = DEFAULT_LANGUAGE;
    }
    
    /**
     * 初始化i18n，根据用户设置或浏览器语言设置当前语言
     */
    async init() {
        try {
            // 尝试从localStorage获取语言设置
            const savedLanguage = localStorage.getItem('admin_language');
            if (savedLanguage && SUPPORTED_LANGUAGES.includes(savedLanguage)) {
                this.currentLanguage = savedLanguage;
                return;
            }
            
            // 尝试从服务器配置获取语言设置
            if (configModel && configModel.loaded) {
                const configLanguage = configModel.getConfig('basic', 'language');
                if (configLanguage && SUPPORTED_LANGUAGES.includes(configLanguage)) {
                    this.currentLanguage = configLanguage;
                    return;
                }
            }
            
            // 尝试匹配浏览器语言
            const browserLang = navigator.language;
            for (const lang of SUPPORTED_LANGUAGES) {
                if (browserLang.startsWith(lang.split('-')[0])) {
                    this.currentLanguage = lang;
                    return;
                }
            }
            
            // 默认使用DEFAULT_LANGUAGE
            this.currentLanguage = DEFAULT_LANGUAGE;
        } catch (error) {
            console.error('初始化i18n出错:', error);
            this.currentLanguage = DEFAULT_LANGUAGE;
        }
    }
    
    /**
     * 获取翻译文本
     * @param {string} key - 文本key
     * @param {object} params - 替换参数
     * @returns {string} 翻译后的文本
     */
    t(key, params = {}) {
        const language = this.currentLanguage;
        
        if (!translations[language] || !translations[language][key]) {
            // 如果找不到翻译，尝试使用默认语言
            if (language !== DEFAULT_LANGUAGE && translations[DEFAULT_LANGUAGE] && translations[DEFAULT_LANGUAGE][key]) {
                return this._replaceParams(translations[DEFAULT_LANGUAGE][key], params);
            }
            // 如果默认语言也没有，返回key
            return key;
        }
        
        return this._replaceParams(translations[language][key], params);
    }
    
    /**
     * 替换文本中的参数占位符
     * @param {string} text - 原始文本，包含 {paramName} 格式的参数
     * @param {object} params - 参数对象
     * @returns {string} 替换后的文本
     */
    _replaceParams(text, params) {
        let result = text;
        
        for (const key in params) {
            const regex = new RegExp(`{${key}}`, 'g');
            result = result.replace(regex, params[key]);
        }
        
        return result;
    }
    
    /**
     * 设置当前语言
     * @param {string} language - 语言代码
     */
    setLanguage(language) {
        if (SUPPORTED_LANGUAGES.includes(language)) {
            this.currentLanguage = language;
            localStorage.setItem('admin_language', language);
            return true;
        }
        return false;
    }
    
    /**
     * 获取当前语言
     * @returns {string} 当前语言代码
     */
    getLanguage() {
        return this.currentLanguage;
    }
    
    /**
     * 获取支持的语言列表
     * @returns {Array} 支持的语言代码数组
     */
    getSupportedLanguages() {
        return [...SUPPORTED_LANGUAGES];
    }
}

// 创建全局i18n实例
const i18n = new I18n();
