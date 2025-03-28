/**
 * 主 JavaScript 文件 - 初始化和协调所有模块
 */
document.addEventListener('DOMContentLoaded', () => {
    const leftSidebar = document.getElementById('leftSidebar');
    const rightSidebar = document.getElementById('rightSidebar');
    const toggleLeftBtn = document.getElementById('toggleLeftSidebarBtn');
    const toggleRightBtn = document.getElementById('toggleRightSidebarBtn');
    const mainContent = document.getElementById('mainContent');

    const leftIconSpan = toggleLeftBtn ? toggleLeftBtn.querySelector('span') : null;
    const rightIconSpan = toggleRightBtn ? toggleRightBtn.querySelector('span') : null;

    // --- 左侧边栏切换 ---
    if (toggleLeftBtn && leftSidebar && leftIconSpan) {
        toggleLeftBtn.addEventListener('click', () => {
            const isCollapsed = leftSidebar.classList.toggle('collapsed');
            // 更新图标和提示文字
            if (isCollapsed) {
                leftIconSpan.classList.remove('icon-collapse-left');
                leftIconSpan.classList.add('icon-expand-left');
                toggleLeftBtn.title = "展开左侧边栏";
            } else {
                leftIconSpan.classList.remove('icon-expand-left');
                leftIconSpan.classList.add('icon-collapse-left');
                toggleLeftBtn.title = "收起左侧边栏";
            }
        });

        // 根据类设置初始状态（如果预先收起）
        if (leftSidebar.classList.contains('collapsed')) {
            leftIconSpan.classList.remove('icon-collapse-left');
            leftIconSpan.classList.add('icon-expand-left');
            toggleLeftBtn.title = "展开左侧边栏";
        }
    }

    // --- 右侧边栏切换 ---
    if (toggleRightBtn && rightSidebar && rightIconSpan) {
        // 默认收起右侧边栏
        rightSidebar.classList.add('collapsed');
        rightIconSpan.classList.remove('icon-collapse-right');
        rightIconSpan.classList.add('icon-expand-right');
        toggleRightBtn.title = "展开右侧边栏";

        toggleRightBtn.addEventListener('click', () => {
            const isCollapsed = rightSidebar.classList.toggle('collapsed');
            // 更新图标和提示文字
            if (isCollapsed) {
                rightIconSpan.classList.remove('icon-collapse-right');
                rightIconSpan.classList.add('icon-expand-right');
                toggleRightBtn.title = "展开右侧边栏";
            } else {
                rightIconSpan.classList.remove('icon-expand-right');
                rightIconSpan.classList.add('icon-collapse-right');
                toggleRightBtn.title = "收起右侧边栏";
            }
        });
    }

    // 初始化API和UI模块（如果存在）
    if (typeof ManusUI !== 'undefined') {
        ManusUI.init();
    }

    console.log("UI已初始化，侧边栏可收起/弹出");
});
