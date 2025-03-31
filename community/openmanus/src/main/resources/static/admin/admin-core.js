/**
 * admin-core.js - 管理界面核心
 * 管理界面的主要入口点，负责初始化和协调功能
 */

/**
 * 管理界面核心
 */
class AdminCore {
    constructor() {
        this.initialized = false;
        this.init = this.init.bind(this);
    }
    
    /**
     * 初始化管理界面
     */
    async init() {
        if (this.initialized) return;
        
        try {
            // 显示加载状态
            document.body.classList.add('loading');
            document.body.insertAdjacentHTML('beforeend', '<div class="loading-overlay"><div class="loading-spinner"></div><div class="loading-text">加载中...</div></div>');
            
            // 1. 加载配置数据
            await configModel.loadConfig();
            
            // 2. 初始化UI
            adminUI.init();
            
            // 3. 初始化事件处理
            adminEvents.init();
            
            // 4. 更新表单值
            adminUI.updateFormValues(configModel.config);
            
            // 5. 保存原始配置用于检测变更
            configModel._originalConfig = configModel._deepCopy(configModel.config);
            
            console.log('管理界面初始化完成');
            this.initialized = true;
        } catch (error) {
            console.error('初始化管理界面出错:', error);
            adminUtils.showNotification('初始化界面失败，请刷新页面重试', 'error', 0);
        } finally {
            // 移除加载状态
            document.body.classList.remove('loading');
            const loadingOverlay = document.querySelector('.loading-overlay');
            if (loadingOverlay) {
                loadingOverlay.remove();
            }
        }
    }
}

// 创建核心实例
const adminCore = new AdminCore();

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', adminCore.init);
