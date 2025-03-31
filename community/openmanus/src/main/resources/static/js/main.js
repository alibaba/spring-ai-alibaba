/**
 * 主入口模块
 */
document.addEventListener('DOMContentLoaded', () => {
    const initializeApp = async () => {
        try {
            // 1. 初始化 UI 模块（包含基础事件系统）
            await ManusUI.init();
            console.log('UI 模块初始化完成');

            // 2. 初始化聊天处理器
            await ChatHandler.init();
            console.log('聊天处理器初始化完成');

            // 3. 初始化右侧边栏
            await RightSidebar.init();
            console.log('右侧边栏初始化完成');

            // 4. 初始化侧边栏切换功能
            initializeSidebars();
            console.log('侧边栏切换功能初始化完成');

            // 5. 绑定发送按钮事件（确保在所有模块初始化后进行）
            const sendButton = document.querySelector('.send-btn');
            const inputField = document.querySelector('.input-area input');
            
            sendButton.addEventListener('click', () => {
                if (inputField.value.trim()) {
                    ChatHandler.handleUserMessage(inputField.value.trim());
                }
            });

            inputField.addEventListener('keypress', (e) => {
                if (e.key === 'Enter' && inputField.value.trim()) {
                    ChatHandler.handleUserMessage(inputField.value.trim());
                }
            });

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
