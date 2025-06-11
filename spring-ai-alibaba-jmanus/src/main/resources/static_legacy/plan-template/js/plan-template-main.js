

/**
 * 计划模板管理器 - 负责协调和初始化所有计划模板相关的组件
 */
class PlanTemplateManager {
    constructor() {
        this.planTemplateListUIHandler = null;
        this.planPromptGenerator = null;
        this.planTemplateHandler = null;
    }

    /**
     * 初始化 PlanTemplateManager 并设置所有组件
     */
    async init() {
        console.log('初始化');
        let chatAreaContainer = document.querySelector('.chat-area');

        if (!chatAreaContainer) {
            console.error('PlanTemplateManager: Main chat area container (.chat-area) not found. ChatHandler will not be initialized.');
            return;
        }
        try {
           
            // 1. 初始化 UI 模块（包含基础事件系统）
            const planExecutionManager = new PlanExecutionManagerController();
            planExecutionManager.init();
            console.log('UI 模块初始化完成');

            const chatHandler = new ChatHandler(planExecutionManager);
            console.log('PlanTemplateManager: ChatHandler initialized.');

            // 3. 初始化侧边栏管理器
            const sidebarManager = new SidebarManager();
            sidebarManager.init();
            console.log('边栏管理器初始化完成');

            // 4. 初始化右侧边栏
            const rightSidebar = new RightSidebarController();
            rightSidebar.init();
            console.log('右侧边栏初始化完成 (from PlanTemplateManager)');

            // 6. 初始化计划提示生成器
            this.planPromptGenerator = new PlanPromptGenerator();
            this.planPromptGenerator.init();
            console.log('PlanPromptGenerator 初始化完成');

            // 7. 初始化处理器
            this.planTemplateHandler = new PlanTemplateHandler();
            this.planTemplateHandler.init();
            console.log('PlanTemplateHandler 初始化完成');

            const planTemplateExecutionController = new PlanTemplateExecutionController();
            planTemplateExecutionController.init();
            console.log('PlanTemplateExecutionController 初始化完成');

            this.planTemplateListUIHandler = new PlanTemplateListUIHandler();
            this.planTemplateListUIHandler.init();
            console.log('PlanTemplateListUIHandler 初始化完成 from main');

        } catch (e) {
            console.error('PlanTemplateManager: Error during initialization:', e);
        }
    }

    /**
     * 获取计划提示生成器实例
     * @returns {PlanPromptGenerator} 计划提示生成器实例
     */
    getPlanPromptGenerator() {
        return this.planPromptGenerator;
    }

    /**
     * 获取计划模板处理器实例
     * @returns {PlanTemplateHandler} 计划模板处理器实例
     */
    getPlanTemplateHandler() {
        return this.planTemplateHandler;
    }

    /**
     * 获取计划模板列表UI处理器实例
     * @returns {PlanTemplateListUIHandler} 计划模板列表UI处理器实例
     */
    getPlanTemplateListUIHandler() {
        return this.planTemplateListUIHandler;
    }
}

/**
 * 边栏管理器 - 负责左右侧边栏的折叠/展开功能
 */
class SidebarManager {
    constructor() {
        this.toggleLeftSidebarBtn = null;
        this.leftSidebar = null;
    }

    /**
     * 初始化边栏管理器 - 只处理左侧边栏，右侧边栏由 RightSidebarController 处理
     */
    init() {
        // 获取左侧边栏切换按钮和侧边栏元素
        this.toggleLeftSidebarBtn = document.getElementById('toggleLeftSidebarBtn');
        this.leftSidebar = document.getElementById('leftSidebar');

        // 绑定左侧边栏切换按钮事件
        if (this.toggleLeftSidebarBtn && this.leftSidebar) {
            this.toggleLeftSidebarBtn.addEventListener('click', this.handleToggleLeftSidebar.bind(this));
            console.log('左侧边栏切换按钮事件绑定成功');
        } else {
            console.warn('SidebarManager: 左侧边栏或切换按钮未找到');
        }

        console.log('SidebarManager 初始化完成 (仅处理左侧边栏)');
    }

    /**
     * 处理左侧边栏折叠/展开
     */
    handleToggleLeftSidebar() {
        if (!this.leftSidebar) {
            console.warn('SidebarManager: 左侧边栏元素未找到');
            return;
        }
        
        this.leftSidebar.classList.toggle('collapsed');
        
        // 可以根据需要调整主内容区域的边距或宽度
        const mainContent = document.getElementById('mainContent') || document.querySelector('.main-content-wrapper');
        if (mainContent) {
            mainContent.classList.toggle('left-collapsed');
        }
    }
}
document.addEventListener('DOMContentLoaded', () => {
    // 创建 PlanTemplateManager 实例并初始化
    const planTemplateManager = new PlanTemplateManager();
    planTemplateManager.init();
    console.log('PlanTemplateManager 初始化完成');
    
    // 将实例暴露到全局作用域，以便其他脚本可以访问
    if (typeof window !== 'undefined') {
        window.planTemplateManager = planTemplateManager;
    }
});
