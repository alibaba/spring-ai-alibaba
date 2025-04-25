/**
 * 计划模板页面的ChatHandler适配器
 * 使原有的ChatHandler组件能够与计划模板页面交互
 */

// 在计划模板页面中创建完善的ManusUI对象，统一事件系统
(function() {
    // 只有当ManusUI未定义时才创建
    if (typeof window.ManusUI !== 'undefined') {
        console.log('ManusUI已存在，不再创建');
        return;
    }
    
    // 检查PlanUIEvents是否存在
    if (typeof PlanUIEvents === 'undefined') {
        console.error('PlanUIEvents未定义，无法创建ManusUI模拟对象');
        return;
    }
    
    // 创建完整的ManusUI模拟对象，增强防护措施
    window.ManusUI = {
        // 事件系统封装，增加防护措施
        EventSystem: {
            // 存储本地注册的事件处理器，避免重复注册
            _registeredHandlers: new Map(),
            
            on: function(eventName, callback) {
                // 验证回调是否为函数
                if (typeof callback !== 'function') {
                    console.error(`[ManusUI适配器] 订阅事件 ${eventName} 失败: 回调不是函数`, callback);
                    return;
                }
                
                // 检查是否重复注册
                if (!this._registeredHandlers.has(eventName)) {
                    this._registeredHandlers.set(eventName, new Set());
                }
                
                const handlers = this._registeredHandlers.get(eventName);
                if (handlers.has(callback)) {
                    console.warn(`[ManusUI适配器] 回调已注册到事件: ${eventName}`);
                    return;
                }
                
                // 添加到本地记录
                handlers.add(callback);
                
                console.log(`[ManusUI适配器] 订阅事件: ${eventName}`);
                // 注册到PlanUIEvents
                PlanUIEvents.EventSystem.on(eventName, callback);
            },
            
            emit: function(eventName, data) {
                console.log(`[ManusUI适配器] 发出事件: ${eventName}`);
                // 直接使用PlanUIEvents发送事件
                PlanUIEvents.EventSystem.emit(eventName, data);
            },
            
            off: function(eventName, callback) {
                // 移除本地记录
                if (this._registeredHandlers.has(eventName)) {
                    const handlers = this._registeredHandlers.get(eventName);
                    handlers.delete(callback);
                }
                
                // 从PlanUIEvents移除
                PlanUIEvents.EventSystem.off(eventName, callback);
            }
        },
        
        // 直接使用PlanUIEvents的UI事件常量
        UI_EVENTS: PlanUIEvents.UI_EVENTS,
        
        // 模拟其他需要的方法
        handleSendMessage: function() {
            console.log('[ManusUI适配器] 模拟handleSendMessage调用');
        }
    };
    
    console.log('已创建完善的ManusUI模拟对象，统一事件系统');
})();

// 自执行函数：修复ChatHandler初始化和事件处理
(function() {
    // 保存原始的ChatHandler方法
    const originalInit = ChatHandler.init;
    
    // 替换ChatHandler.init方法
    ChatHandler.init = function() {
        console.log('[ChatHandler适配器] 使用适配的init方法');
        
        // 在计划模板页面中，使用.simple-chat-area替代.chat-area
        ChatHandler.chatArea = document.querySelector('.simple-chat-area');
        
        // 订阅业务事件 - 使用统一的ManusUI.EventSystem
        ManusUI.EventSystem.on('plan-update', ChatHandler.handlePlanUpdate);
        ManusUI.EventSystem.on('plan-completed', ChatHandler.handlePlanComplete);
        ManusUI.EventSystem.on(ManusUI.UI_EVENTS.DIALOG_ROUND_START, ChatHandler.handleDialogRoundStart);
        
        console.log('[ChatHandler适配器] 初始化完成，chatArea =', ChatHandler.chatArea);
    };
    
    // 完全重写handleDialogRoundStart方法，适应计划模板页面
    ChatHandler.handleDialogRoundStart = function(eventData) {
        if (!eventData || !eventData.planId) {
            console.error('[ChatHandler适配器] 事件数据缺少planId');
            return;
        }
        
        const { planId, query } = eventData;
        
        try {
            // 创建新的对话轮次ID
            const dialogRoundId = Date.now().toString();
            
            // 检查聊天区域
            if (!ChatHandler.chatArea) {
                console.error('[ChatHandler适配器] ChatHandler.chatArea不存在');
                ChatHandler.chatArea = document.querySelector('.simple-chat-area');
                
                if (!ChatHandler.chatArea) {
                    console.error('[ChatHandler适配器] 无法找到.simple-chat-area元素');
                    return;
                }
            }
            
            // 查找或创建对话容器
            let dialogRoundContainer = ChatHandler.chatArea.querySelector('.dialog-round-container');
            if (!dialogRoundContainer) {
                dialogRoundContainer = document.createElement('div');
                dialogRoundContainer.className = 'dialog-round-container';
                dialogRoundContainer.dataset.dialogRoundId = dialogRoundId;
                dialogRoundContainer.dataset.planId = planId;
                ChatHandler.chatArea.appendChild(dialogRoundContainer);
                console.log('[ChatHandler适配器] 创建了新的对话容器');
            } else {
                // 如果容器已存在，更新planId
                dialogRoundContainer.dataset.planId = planId;
                console.log('[ChatHandler适配器] 使用现有对话容器，更新planId');
            }
            
            // 检查dialogRoundContainer是否已存在并已附加到DOM
            if (!dialogRoundContainer.isConnected) {
                console.error('[ChatHandler适配器] 对话容器未连接到DOM');
                return;
            }
            
            // 清空现有内容
            dialogRoundContainer.innerHTML = '';
            
            // 添加用户消息
            const userMessage = document.createElement('div');
            userMessage.className = 'message user-message';
            userMessage.innerHTML = `<p>${query || '执行计划'}</p>`;
            dialogRoundContainer.appendChild(userMessage);
            
            // 创建AI步骤容器
            const aiStepsContainer = document.createElement('div');
            aiStepsContainer.className = 'message ai-message ai-steps-container';
            dialogRoundContainer.appendChild(aiStepsContainer);
            
            // 添加AI头部
            const aiHeader = document.createElement('div');
            aiHeader.className = 'ai-header';
            aiHeader.innerHTML = '<span class="ai-logo">M</span> Manus AI';
            aiStepsContainer.appendChild(aiHeader);
            
            console.log('[ChatHandler适配器] 已成功创建对话轮次:', dialogRoundId, '对应计划:', planId);
            
            // 滚动到底部
            ChatHandler.chatArea.scrollTop = ChatHandler.chatArea.scrollHeight;
        } catch (error) {
            console.error('[ChatHandler适配器] 处理对话轮次开始事件失败:', error);
        }
    };
    
    console.log('[ChatHandler适配器] 已替换ChatHandler方法');
})();

// 确保在document加载完成后执行
document.addEventListener('DOMContentLoaded', () => {
    console.log('[ChatHandler适配器] DOM已加载，准备初始化');
    
    // 确保所有需要的组件都已加载
    const checkComponents = () => {
        if (typeof PlanUIEvents !== 'undefined' && typeof ChatHandler !== 'undefined') {
            console.log('[ChatHandler适配器] 所有组件已加载，初始化适配器');
            console.log('[ChatHandler适配器] 初始化完成');
        } else {
            console.warn('[ChatHandler适配器] 某些组件尚未加载，将在100ms后重试');
            setTimeout(checkComponents, 100);
        }
    };
    
    // 开始检查
    checkComponents();
});
