/**
 * admin-ui.js - 管理界面UI交互
 * 处理管理界面的用户界面交互和展示逻辑
 */

// UI管理器
class AdminUI {
    constructor() {
        // 缓存DOM元素
        this.elements = {
            // 类别选项
            categoryItems: document.querySelectorAll('.category-item'),
            // 配置面板
            configPanels: document.querySelectorAll('.config-panel'),
            // 按钮
            saveConfigBtn: document.getElementById('saveConfigBtn'),
            backToMainBtn: document.getElementById('backToMainBtn')
        };
        
        // 绑定this
        this.switchCategory = this.switchCategory.bind(this);
        this.bindEvents = this.bindEvents.bind(this);
        this.updateFormValues = this.updateFormValues.bind(this);
        this.getFormValues = this.getFormValues.bind(this);
    }
    
    /**
     * 初始化UI
     */
    init() {
        this.bindEvents();
    }
    
    /**
     * 绑定事件处理程序
     */
    bindEvents() {
        // 类别切换事件
        this.elements.categoryItems.forEach(item => {
            item.addEventListener('click', () => {
                const category = item.getAttribute('data-category');
                this.switchCategory(category);
            });
        });
        
        // 表单输入变化监听
        const formInputs = document.querySelectorAll('input, select, textarea');
        formInputs.forEach(input => {
            input.addEventListener('change', (e) => {
                this.handleInputChange(e);
            });
            
            if (input.type === 'text' || input.tagName === 'TEXTAREA') {
                input.addEventListener('input', adminUtils.debounce((e) => {
                    this.handleInputChange(e);
                }, 500));
            }
        });
    }
    
    /**
     * 处理表单输入变化
     * @param {Event} e - 输入事件
     */
    handleInputChange(e) {
        const input = e.target;
        const id = input.id;
        
        // 根据输入ID获取相应的配置节和键
        const configMapping = this.getConfigMappingFromId(id);
        if (!configMapping) return;
        
        const { section, key } = configMapping;
        let value;
        
        // 根据输入类型获取值
        switch (input.type) {
            case 'checkbox':
                value = input.checked;
                break;
            case 'number':
                value = parseInt(input.value, 10);
                break;
            default:
                value = input.value;
        }
        
        // 更新配置
        configModel.updateConfig(section, key, value);
    }
    
    /**
     * 根据输入ID获取配置映射
     * @param {string} id - 输入元素ID
     * @returns {Object|null} 配置映射对象 {section, key}
     */
    getConfigMappingFromId(id) {
        // 配置ID映射表
        const mappings = {
            // 基础配置
            'system-name': { section: 'basic', key: 'systemName' },
            'system-language': { section: 'basic', key: 'language' },
            'max-threads': { section: 'basic', key: 'performance.maxThreads' },
            'timeout-seconds': { section: 'basic', key: 'performance.timeoutSeconds' },
            
            // Agent配置
            'default-agent': { section: 'agent', key: 'defaultType' },
            'agent-timeout': { section: 'agent', key: 'timeout' },
            'max-turns': { section: 'agent', key: 'maxTurns' },
            'system-prompt': { section: 'agent', key: 'systemPrompt' },
            
            // Tool配置
            'enable-search-tool': { section: 'tool', key: 'enabled.search' },
            'enable-calculator-tool': { section: 'tool', key: 'enabled.calculator' },
            'enable-weather-tool': { section: 'tool', key: 'enabled.weather' },
            'weather-api-key': { section: 'tool', key: 'apiKeys.weather' },
            
            // MCP配置
            'mcp-endpoint': { section: 'mcp', key: 'endpoint' },
            'mcp-version': { section: 'mcp', key: 'version' },
            'default-model': { section: 'mcp', key: 'defaultModel' },
            'custom-model-url': { section: 'mcp', key: 'customModelUrl' }
        };
        
        return mappings[id] || null;
    }
    
    /**
     * 切换配置类别
     * @param {string} category - 类别名称
     */
    switchCategory(category) {
        // 移除所有类别的活动状态
        this.elements.categoryItems.forEach(item => {
            item.classList.remove('active');
        });
        
        // 设置当前类别为活动状态
        const activeItem = document.querySelector(`.category-item[data-category="${category}"]`);
        if (activeItem) {
            activeItem.classList.add('active');
        }
        
        // 隐藏所有配置面板
        this.elements.configPanels.forEach(panel => {
            panel.classList.remove('active');
        });
        
        // 显示当前配置面板
        const activePanel = document.getElementById(`${category}-config`);
        if (activePanel) {
            activePanel.classList.add('active');
        }
    }
    
    /**
     * 更新表单值
     * @param {Object} config - 配置对象
     */
    updateFormValues(config) {
        // 基础配置
        document.getElementById('system-name').value = config.basic.systemName;
        document.getElementById('system-language').value = config.basic.language;
        document.getElementById('max-threads').value = config.basic.performance.maxThreads;
        document.getElementById('timeout-seconds').value = config.basic.performance.timeoutSeconds;
        
        // Agent配置
        document.getElementById('default-agent').value = config.agent.defaultType;
        document.getElementById('agent-timeout').value = config.agent.timeout;
        document.getElementById('max-turns').value = config.agent.maxTurns;
        document.getElementById('system-prompt').value = config.agent.systemPrompt;
        
        // Tool配置
        document.getElementById('enable-search-tool').checked = config.tool.enabled.search;
        document.getElementById('enable-calculator-tool').checked = config.tool.enabled.calculator;
        document.getElementById('enable-weather-tool').checked = config.tool.enabled.weather;
        document.getElementById('weather-api-key').value = config.tool.apiKeys.weather || '';
        
        // MCP配置
        document.getElementById('mcp-endpoint').value = config.mcp.endpoint;
        document.getElementById('mcp-version').value = config.mcp.version;
        document.getElementById('default-model').value = config.mcp.defaultModel;
        document.getElementById('custom-model-url').value = config.mcp.customModelUrl || '';
    }
    
    /**
     * 获取表单值
     * @returns {Object} 表单配置对象
     */
    getFormValues() {
        const config = {
            basic: {
                systemName: document.getElementById('system-name').value,
                language: document.getElementById('system-language').value,
                performance: {
                    maxThreads: parseInt(document.getElementById('max-threads').value, 10),
                    timeoutSeconds: parseInt(document.getElementById('timeout-seconds').value, 10)
                }
            },
            agent: {
                defaultType: document.getElementById('default-agent').value,
                timeout: parseInt(document.getElementById('agent-timeout').value, 10),
                maxTurns: parseInt(document.getElementById('max-turns').value, 10),
                systemPrompt: document.getElementById('system-prompt').value
            },
            tool: {
                enabled: {
                    search: document.getElementById('enable-search-tool').checked,
                    calculator: document.getElementById('enable-calculator-tool').checked,
                    weather: document.getElementById('enable-weather-tool').checked
                },
                apiKeys: {
                    weather: document.getElementById('weather-api-key').value
                }
            },
            mcp: {
                endpoint: document.getElementById('mcp-endpoint').value,
                version: document.getElementById('mcp-version').value,
                defaultModel: document.getElementById('default-model').value,
                customModelUrl: document.getElementById('custom-model-url').value
            }
        };
        
        return config;
    }
    
    /**
     * 显示加载状态
     * @param {boolean} isLoading - 是否处于加载状态
     */
    setLoading(isLoading) {
        if (isLoading) {
            document.body.classList.add('loading');
            this.elements.saveConfigBtn.disabled = true;
            this.elements.saveConfigBtn.textContent = '保存中...';
        } else {
            document.body.classList.remove('loading');
            this.elements.saveConfigBtn.disabled = false;
            this.elements.saveConfigBtn.textContent = '保存配置';
        }
    }
}

// 创建全局UI实例
const adminUI = new AdminUI();
