/**
 *  TaskPilotUIEvent   System - 提供事件发布订阅机制
 */
const TaskPilotUIEvent = (() => {
    // 事件监听器集合
    const eventListeners = {};

    // UI更新相关的事件类型
    const UI_EVENTS = {
        MESSAGE_UPDATE: 'ui:message:update',
        MESSAGE_COMPLETE: 'ui:message:complete',
        
        SECTION_ADD: 'ui:section:add',
        DIALOG_ROUND_START: 'ui:dialog:round:start',
        
        PLAN_UPDATE: 'ui:plan:update',
        PLAN_COMPLETED: 'ui:plan:completed',
        
        USER_MESSAGE_SEND_REQUESTED: 'ui:user:message:send_requested', // 新增事件
        
        CHAT_INPUT_CLEAR: 'ui:chatinput:clear', // 新增事件：清空聊天输入框
        CHAT_INPUT_UPDATE_STATE: 'ui:chatinput:updatestate', // 新增事件：更新聊天输入框状态
        
        PLAN_TEMPLATE_SELECTED: 'ui:plan_template:selected', // Event for when a plan template is selected or selection changes
        PLAN_EXECUTION_REQUESTED: 'ui:plan:execution_requested', // Event for when a plan execution is requested directly (e.g., via "Run Plan" button)
        
        
        GENERATION_STATE_CHANGED: 'ui:generation:state_changed', // 生成状态变化
        EXECUTION_STATE_CHANGED: 'ui:execution:state_changed', // 执行状态变化
        CURRENT_PLAN_TEMPLATE_CHANGED: 'ui:current_plan_template:changed', // 当前计划模板变化
        PLAN_PARAMS_CHANGED: 'ui:plan_params:changed', // 计划参数变化
        
        // JSON编辑器相关事件
        JSON_CONTENT_SET: 'ui:json:content_set', // 设置JSON内容
        JSON_CONTENT_CLEAR: 'ui:json:content_clear', // 清空JSON内容
        VERSION_HISTORY_SET: 'ui:version_history:set', // 设置版本历史
        
    };

    // 事件发布订阅系统
    const EventSystem = {
        // 订阅事件 ，一般都是界面层，去响应事件
        on: (eventName, callback) => {
            if (typeof callback !== 'function') {
                console.error(`Error subscribing to event ${eventName}: Callback is not a function.`, callback);
                return;
            }
            if (!eventListeners[eventName]) {
                eventListeners[eventName] = [];
            }
            if (eventListeners[eventName].indexOf(callback) !== -1) {
                console.warn(`Callback already registered for event: ${eventName}`);
                return;
            }
            eventListeners[eventName].push(callback);
            // console.log(`Event handler registered: ${eventName}`);
        },

        // 发布事件 ，一般是由底层ui + api 等触发 。然后界面层去响应
        emit: (eventName, data) => {
            if (eventListeners[eventName]) {
                eventListeners[eventName].forEach(callback => {
                    if (typeof callback === 'function') {
                        try {
                            callback(data); // 直接传递 data
                        } catch (error) {
                            console.error(`Error executing callback for event ${eventName}:`, error);
                        }
                    } else {
                        console.warn(`Callback for event ${eventName} is not a function:`, callback);
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

    // 返回公开的事件系统和事件类型
    return {
        EventSystem,
        UI_EVENTS
    };
})();
