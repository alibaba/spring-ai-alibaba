/**
 * 计划模板UI事件系统
 * 提供事件发布订阅机制，实现UI组件之间的通信
 */
const PlanUIEvents = (() => {
    // 事件监听器集合
    const eventListeners = {
        'plan-update': [],
        'agent-execution': [],
        'plan-completed': []
    };
    
    // UI更新相关的事件类型
    const UI_EVENTS = {
        DIALOG_ROUND_START: 'ui:dialog:round:start',  // 对话轮次开始
        DIALOG_ROUND_UPDATE: 'ui:dialog:round:update' // 对话轮次更新
    };

    // 事件发布订阅系统
    const EventSystem = {
        // 订阅事件
        on: (eventName, callback) => {
            // 验证回调是否为函数
            if (typeof callback !== 'function') {
                console.error(`订阅事件 ${eventName} 失败: 回调不是函数`, callback);
                return;
            }
            
            // 初始化事件数组（如果不存在）
            if (!eventListeners[eventName]) {
                eventListeners[eventName] = [];
            }
            
            // 检查是否已经注册了相同的回调
            if (eventListeners[eventName].indexOf(callback) !== -1) {
                console.warn(`回调已经注册到事件: ${eventName}`);
                return;
            }
            
            // 添加回调
            eventListeners[eventName].push(callback);
            console.log(`已注册事件处理器: ${eventName}`);
        },
        
        // 发布事件
        emit: (eventName, data) => {
            if (eventListeners[eventName]) {
                eventListeners[eventName].forEach(callback => {
                    // 检查callback是否为函数
                    if (typeof callback === 'function') {
                        try {
                            callback(data);
                        } catch (error) {
                            console.error(`执行事件 ${eventName} 的回调时发生错误:`, error);
                        }
                    } else {
                        console.warn(`事件 ${eventName} 的回调不是函数:`, callback);
                    }
                });
            }
        },
        
        // 取消订阅
        off: (eventName, callback) => {
            if (eventListeners[eventName]) {
                eventListeners[eventName] = eventListeners[eventName]
                    .filter(listener => listener !== callback);
            }
        }
    };

    // 返回公开的事件系统
    return {
        EventSystem,
        UI_EVENTS
    };
})();
