/**
 * admin-events.js - 管理界面事件处理
 * 负责处理管理界面的各种事件
 */

// 事件处理器
class AdminEvents {
    constructor() {
        // 绑定事件处理方法
        this.handleSaveConfig = this.handleSaveConfig.bind(this);
        this.handleBackToMain = this.handleBackToMain.bind(this);
        this.handleTestWeatherApi = this.handleTestWeatherApi.bind(this);
        this.handleCustomModelVisibility = this.handleCustomModelVisibility.bind(this);
    }
    
    /**
     * 初始化事件
     */
    init() {
        // 保存配置按钮
        document.getElementById('saveConfigBtn').addEventListener('click', this.handleSaveConfig);
        
        // 返回主页按钮
        document.getElementById('backToMainBtn').addEventListener('click', this.handleBackToMain);
        
        // 测试天气API按钮
        const testWeatherApiBtn = document.getElementById('test-weather-api');
        if (testWeatherApiBtn) {
            testWeatherApiBtn.addEventListener('click', this.handleTestWeatherApi);
        }
        
        // 默认模型选择变更事件
        const defaultModelSelect = document.getElementById('default-model');
        if (defaultModelSelect) {
            defaultModelSelect.addEventListener('change', this.handleCustomModelVisibility);
            // 初始触发一次
            this.handleCustomModelVisibility({ target: defaultModelSelect });
        }
        
        // 页面离开确认
        window.addEventListener('beforeunload', this.handleBeforeUnload);
    }
    
    /**
     * 处理保存配置事件
     */
    async handleSaveConfig() {
        try {
            // 设置加载状态
            adminUI.setLoading(true);
            
            // 保存配置
            const result = await configModel.saveConfig();
            
            if (result.success) {
                adminUtils.showNotification('配置保存成功', 'success');
            } else {
                adminUtils.showNotification(`保存失败: ${result.message}`, 'error');
            }
        } catch (error) {
            console.error('保存配置出错:', error);
            adminUtils.showNotification(`保存出错: ${error.message || '未知错误'}`, 'error');
        } finally {
            // 取消加载状态
            adminUI.setLoading(false);
        }
    }
    
    /**
     * 处理返回主页事件
     * @param {Event} e - 点击事件
     */
    async handleBackToMain(e) {
        // 检查是否有未保存的更改
        const originalConfig = await configModel.loadConfig();
        const currentConfig = configModel.config;
        
        if (adminUtils.hasUnsavedChanges(originalConfig, currentConfig)) {
            const confirmed = await adminUtils.confirmDialog('您有未保存的更改，确定要离开吗？');
            if (!confirmed) {
                e.preventDefault();
                return;
            }
        }
        
        // 返回主页
        window.location.href = '/';
    }
    
    /**
     * 处理测试天气API事件
     */
    async handleTestWeatherApi() {
        const apiKey = document.getElementById('weather-api-key').value.trim();
        
        if (!apiKey) {
            adminUtils.showNotification('请输入API密钥', 'warning');
            return;
        }
        
        try {
            // 设置测试按钮状态
            const testBtn = document.getElementById('test-weather-api');
            const originalText = testBtn.textContent;
            testBtn.disabled = true;
            testBtn.textContent = '测试中...';
            
            // 发送测试请求
            const response = await fetch('/api/admin/test-weather-api', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ apiKey })
            });
            
            const result = await response.json();
            
            if (result.success) {
                adminUtils.showNotification('API连接测试成功', 'success');
            } else {
                adminUtils.showNotification(`测试失败: ${result.message}`, 'error');
            }
        } catch (error) {
            console.error('测试API出错:', error);
            adminUtils.showNotification(`测试出错: ${error.message || '未知错误'}`, 'error');
        } finally {
            // 恢复按钮状态
            const testBtn = document.getElementById('test-weather-api');
            testBtn.disabled = false;
            testBtn.textContent = '测试连接';
        }
    }
    
    /**
     * 处理自定义模型输入框可见性
     * @param {Event} e - 变更事件
     */
    handleCustomModelVisibility(e) {
        const value = e.target.value;
        const customModelUrlInput = document.getElementById('custom-model-url');
        const customModelUrlContainer = customModelUrlInput.closest('.config-item');
        
        if (value === 'custom') {
            customModelUrlContainer.style.display = 'block';
        } else {
            customModelUrlContainer.style.display = 'none';
        }
    }
    
    /**
     * 处理页面离开前确认
     * @param {BeforeUnloadEvent} e - 页面卸载前事件
     */
    handleBeforeUnload(e) {
        // 如果有未保存的更改，提示用户
        if (configModel.loaded && adminUtils.hasUnsavedChanges(configModel._originalConfig, configModel.config)) {
            const message = '您有未保存的更改，确定要离开吗？';
            e.returnValue = message;
            return message;
        }
    }
}

// 创建全局事件处理实例
const adminEvents = new AdminEvents();
