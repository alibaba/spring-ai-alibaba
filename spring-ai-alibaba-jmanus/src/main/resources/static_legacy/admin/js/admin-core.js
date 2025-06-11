/**
 * admin-core.js - 管理界面核心功能协调
 */
class AdminCore {
    constructor() {
        this.initializePanels();
        this.bindGlobalEvents();
    }

    /**
     * 初始化面板显示
     */
    initializePanels() {
        // 获取所有配置面板和类别项
        const panels = document.querySelectorAll('.config-panel');
        const categories = document.querySelectorAll('.category-item');

        // 设置初始激活状态
        this.setActivePanel('basic');

        // 绑定类别切换事件
        categories.forEach(category => {
            category.addEventListener('click', () => {
                const categoryId = category.dataset.category;
                this.setActivePanel(categoryId);
            });
        });
    }

    /**
     * 设置当前激活的面板
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
