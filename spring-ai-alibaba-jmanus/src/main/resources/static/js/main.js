/**
 * 主入口模块
 */
document.addEventListener('DOMContentLoaded', () => {
    const initializeApp = async () => {
        try {
            // 1. 初始化 UI 模块（包含基础事件系统）
            await PlanExecutionManager.init();
            console.log('UI 模块初始化完成');

            const chatInputHandler = new ChatInputHandler();

            // 2. 初始化聊天处理器
            const chatHandler = new ChatHandler();
            console.log('聊天处理器初始化完成');

            // 3. 初始化右侧边栏
            await RightSidebar.init();
            console.log('右侧边栏初始化完成');

            // 4. 初始化侧边栏切换功能
            initializeSidebars();
            console.log('侧边栏切换功能初始化完成');


            console.log('应用初始化完成');
        } catch (error) {
            console.error('应用初始化失败:', error);
        }
    };

    initializeApp();
});

/**
 * 初始化侧边栏切换功能
 */
function initializeSidebars() {
    const rightSidebar = document.getElementById('rightSidebar');
    const mainContent = document.getElementById('mainContent');

    // 右侧边栏切换按钮事件
    document.getElementById('toggleRightSidebarBtn').addEventListener('click', () => {
        rightSidebar.classList.toggle('collapsed');
        mainContent.classList.toggle('right-expanded');
    });
}
