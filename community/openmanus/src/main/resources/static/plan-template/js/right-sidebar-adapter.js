/**
 * 计划模板页面的RightSidebar适配器
 * 使原有的RightSidebar组件能够与计划模板页面交互
 */

// 对RightSidebar的init方法进行覆盖，修复chatArea查找问题
(function() {
    // 检查RightSidebar是否定义
    if (typeof RightSidebar === 'undefined') {
        console.error('[RightSidebar适配器] RightSidebar未定义，无法覆盖init方法');
        return;
    }
    
    // 保存原始的init方法引用
    const originalInit = RightSidebar.init;
    
    // 覆盖init方法
    RightSidebar.init = function() {
        console.log('[RightSidebar适配器] 正在初始化右侧边栏');
        
        // 获取DOM元素
        const sidebarElement = document.getElementById('rightSidebar');
        if (!sidebarElement) {
            console.error('[RightSidebar适配器] 找不到#rightSidebar元素');
            return;
        }
        
        const sidebarContent = sidebarElement.querySelector('.right-sidebar-content');
        const executionStatusElement = document.getElementById('execution-status');
        const executionProgressElement = document.getElementById('execution-progress');
        
        // 将这些元素保存到RightSidebar对象中，以便其他方法可以访问
        RightSidebar.sidebarElement = sidebarElement;
        RightSidebar.sidebarContent = sidebarContent;
        RightSidebar.executionStatusElement = executionStatusElement;
        RightSidebar.executionProgressElement = executionProgressElement;
        
        // 使用统一的事件系统，确保事件能正确传递
        // 这里使用ManusUI.EventSystem，它已经在chat-handler-adapter.js中被定义为包装了PlanUIEvents.EventSystem
        if (typeof ManusUI !== 'undefined' && ManusUI.EventSystem) {
            ManusUI.EventSystem.on('plan-update', RightSidebar.handlePlanUpdate);
            console.log('[RightSidebar适配器] 已订阅plan-update事件');
        } else {
            console.error('[RightSidebar适配器] ManusUI.EventSystem未定义，无法订阅事件');
        }
        
        // 注意这里不再尝试添加.chat-area的事件监听器，
        // 事件监听会在adaptRightSidebar中添加到.simple-chat-area
        
        console.log('[RightSidebar适配器] 右侧边栏初始化完成');
        
        // 返回公共接口以保持兼容性
        return {
            handlePlanUpdate: RightSidebar.handlePlanUpdate,
            showStepDetails: RightSidebar.showStepDetails
        };
    };
    
    console.log('[RightSidebar适配器] 已覆盖RightSidebar.init方法');
})();

// 确保在document加载完成后执行
document.addEventListener('DOMContentLoaded', () => {
    console.log('[RightSidebar适配器] DOM已加载，准备初始化');
    
    // 确保所有需要的组件都已加载
    const checkComponents = () => {
        if (typeof PlanUIEvents !== 'undefined' && typeof RightSidebar !== 'undefined' && typeof ManusUI !== 'undefined') {
            console.log('[RightSidebar适配器] 所有组件已加载，初始化适配器');
            adaptRightSidebar();
            console.log('[RightSidebar适配器] 初始化完成');
        } else {
            console.warn('[RightSidebar适配器] 某些组件尚未加载，将在100ms后重试');
            setTimeout(checkComponents, 100);
        }
    };
    
    // 开始检查
    checkComponents();
});

/**
 * 适配RightSidebar组件
 */
function adaptRightSidebar() {
    // 直接订阅PlanUIEvents事件，避免循环触发
    // 统一使用PlanUIEvents作为事件源
    PlanUIEvents.EventSystem.on('plan-update', (planDetails) => {
        if (!planDetails || !planDetails.planId) return;
        
        console.log('[RightSidebar适配器] 收到plan-update事件');
        
        // 直接调用处理方法，不再转发到ManusUI以避免循环
        if (RightSidebar.handlePlanUpdate) {
            console.log('[RightSidebar适配器] 调用RightSidebar.handlePlanUpdate');
            RightSidebar.handlePlanUpdate(planDetails);
        }
    });
    
    // 从ManusUI到PlanUIEvents
    if (typeof ManusUI !== 'undefined' && ManusUI.EventSystem) {
        ManusUI.EventSystem.on('plan-update', (planDetails) => {
            if (!planDetails || !planDetails.planId) return;
            console.log('[RightSidebar适配器] ManusUI收到plan-update事件');
        });
    }
    
    // 增加聊天区域点击事件监听，以支持点击步骤查看详情
    const chatArea = document.querySelector('.simple-chat-area');
    if (chatArea) {
        console.log('[RightSidebar适配器] 为simple-chat-area添加点击事件');
        chatArea.addEventListener('click', handleChatAreaClick);
    } else {
        console.warn('[RightSidebar适配器] 找不到.simple-chat-area元素');
    }
    
    /**
     * 处理聊天区域点击事件
     */
    function handleChatAreaClick(event) {
        // 查找是否点击了步骤标题
        const sectionHeader = findParentWithClass(event.target, 'section-header');
        if (!sectionHeader) return;
        
        // 找到对应的对话轮次容器并获取planId
        const dialogRoundContainer = findParentWithClass(sectionHeader, 'dialog-round-container');
        if (!dialogRoundContainer) return;
        
        const planId = dialogRoundContainer.dataset.planId;
        if (!planId) return;
        
        // 找到点击的步骤索引
        const stepElement = findParentWithClass(sectionHeader, 'ai-section');
        if (!stepElement) return;
        
        const stepIndex = findStepIndex(stepElement);
        if (stepIndex === -1) return;
        
        // 如果RightSidebar提供了showStepDetails方法，调用它显示步骤详情
        if (RightSidebar.showStepDetails) {
            console.log(`[RightSidebar适配器] 显示步骤详情: planId=${planId}, stepIndex=${stepIndex}`);
            RightSidebar.showStepDetails(planId, stepIndex);
        }
    }
}

/**
 * 查找具有特定类名的父元素
 */
function findParentWithClass(element, className) {
    while (element && !element.classList.contains(className)) {
        element = element.parentElement;
    }
    return element;
}

/**
 * 查找步骤索引
 */
function findStepIndex(stepElement) {
    const parentContainer = stepElement.closest('.ai-steps-container');
    if (!parentContainer) return -1;
    
    const steps = parentContainer.querySelectorAll('.ai-section');
    return Array.from(steps).indexOf(stepElement);
}
