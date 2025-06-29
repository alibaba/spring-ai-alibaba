/**
 * admin-core.js - Admin interface core functionality coordination
 */
class AdminCore {
    constructor() {
        this.initializePanels();
        this.bindGlobalEvents();
    }

    /**
     * Initialize panel display
     */
    initializePanels() {
        // Get all configuration panels and category items
        const panels = document.querySelectorAll('.config-panel');
        const categories = document.querySelectorAll('.category-item');

        // Set initial active state
        this.setActivePanel('basic');

        // Bind category switching events
        categories.forEach(category => {
            category.addEventListener('click', () => {
                const categoryId = category.dataset.category;
                this.setActivePanel(categoryId);
            });
        });
    }

    /**
     * Set currently active panel
     */
    setActivePanel(categoryId) {
        // 更新类别项的激活状态
        document.querySelectorAll('.category-item').forEach(item => {
            item.classList.toggle('active', item.dataset.category === categoryId);
        });

        // 更新面板的显示状态
        document.querySelectorAll('.config-panel').forEach(panel => {
            panel.classList.toggle('active', panel.id === `${categoryId}-config`);
        });
    }

    /**
     * 绑定全局事件
     */
    bindGlobalEvents() {
        // 返回主页按钮
        const backBtn = document.getElementById('backToMainBtn');
        if (backBtn) {
            backBtn.addEventListener('click', () => {
                window.location.href = '/index.html';
            });
        }

        // 监听页面离开事件
        window.addEventListener('beforeunload', (event) => {
            // 如果有未保存的更改，提示用户
            if (this.hasUnsavedChanges()) {
                event.preventDefault();
                event.returnValue = '您有未保存的更改，确定要离开吗？';
            }
        });
    }

    /**
     * 检查是否有未保存的更改
     */
    hasUnsavedChanges() {
        // TODO: 实现未保存更改检查逻辑
        return false;
    }
}

// 创建全局管理界面实例
window.adminCore = new AdminCore();
