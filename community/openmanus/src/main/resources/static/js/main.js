/**
 * 主入口模块
 */
document.addEventListener('DOMContentLoaded', () => {
    // 初始化 UI 模块
    ManusUI.init();
    
    // 初始化聊天处理器
    ChatHandler.init();
    
    // 初始化右侧边栏
    RightSidebar.init();
    
    // 初始化侧边栏切换
    initializeSidebars();
});

/**
 * 初始化侧边栏切换功能
 */
function initializeSidebars() {
    const leftSidebar = document.getElementById('leftSidebar');
    const rightSidebar = document.getElementById('rightSidebar');
    const mainContent = document.getElementById('mainContent');
    
    // 左侧边栏切换按钮事件
    /*document.getElementById('toggleLeftSidebarBtn').addEventListener('click', () => {
        leftSidebar.classList.toggle('collapsed');
        mainContent.classList.toggle('left-expanded');
    });*/
    
    // 右侧边栏切换按钮事件
    document.getElementById('toggleRightSidebarBtn').addEventListener('click', () => {
        rightSidebar.classList.toggle('collapsed');
        mainContent.classList.toggle('right-expanded');
    });
}
