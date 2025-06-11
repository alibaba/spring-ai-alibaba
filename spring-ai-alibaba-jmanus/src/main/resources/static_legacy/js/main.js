/**
 * 主入口模块
 */
document.addEventListener('DOMContentLoaded', () => {
    const initializeApp = async () => {
        try {
            // 1. 初始化 UI 模块（包含基础事件系统）
            // PlanExecutionManagerController is a class, we need to create an instance.
            const planExecutionManager = new PlanExecutionManagerController();
            planExecutionManager.init(); // init is synchronous
            console.log('UI 模块初始化完成');

            const chatInputHandler = new ChatInputHandler();

            // 2. 初始化聊天处理器
            const chatHandler = new ChatHandler(planExecutionManager); // Pass planExecutionManager instance
            console.log('聊天处理器初始化完成');

            // 3. 初始化右侧边栏
            // RightSidebar is now an instance of RightSidebarController, call init on it.

            const rightSidebar = new RightSidebarController();
            await rightSidebar.init();
            console.log('右侧边栏初始化完成');


            console.log('应用初始化完成');
        } catch (error) {
            console.error('应用初始化失败:', error);
        }
    };

    initializeApp();
});
