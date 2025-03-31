/**
 * UI 模块 - 处理用户界面交互
 */
const ManusUI = (() => {
    // 缓存DOM元素
    let chatArea;
    let inputField;
    let sendButton;
    
    // 当前活动的任务ID
    let activePlanId = null;
    
    // 记录上一次sequence的大小
    let lastSequenceSize = 0;
    
    // 轮询间隔（毫秒） - 从2秒增加到6秒
    const POLL_INTERVAL = 6000;
    
    // 轮询定时器
    let pollTimer = null;
    
    // 轮询并发控制标志
    let isPolling = false;
    
    // 事件监听器集合
    const eventListeners = {
        'plan-update': [],
        'agent-execution': [],
        'plan-completed': []
    };
    
    // UI更新相关的事件类型
    const UI_EVENTS = {
        MESSAGE_UPDATE: 'ui:message:update',
        MESSAGE_COMPLETE: 'ui:message:complete',
        SECTION_ADD: 'ui:section:add',
        DIALOG_ROUND_START: 'ui:dialog:round:start',  // 新增：对话轮次开始
        DIALOG_ROUND_UPDATE: 'ui:dialog:round:update' // 新增：对话轮次更新
    };

    // 事件发布订阅系统
    const EventSystem = {
        // 订阅事件
        on: (eventName, callback) => {
            if (!eventListeners[eventName]) {
                eventListeners[eventName] = [];
            }
            eventListeners[eventName].push(callback);
        },
        
        // 发布事件
        emit: (eventName, data) => {
            if (eventListeners[eventName]) {
                // 确保事件数据中包含planId
                const eventData = data ? { ...data, planId: activePlanId } : { planId: activePlanId };
                eventListeners[eventName].forEach(callback => callback(eventData));
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

    /**
     * 初始化UI组件
     */
    const init = () => {
        // 获取DOM元素
        chatArea = document.querySelector('.chat-area');
        inputField = document.querySelector('.input-area input');
        sendButton = document.querySelector('.send-btn');
        
        // 添加事件监听器
        sendButton.addEventListener('click', handleSendMessage);
        inputField.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                handleSendMessage();
            }
        });
        
        // 初始化事件监听
        initializeEventListeners();
        
        console.log('Manus UI 初始化完成');
    };
    
    /**
     * 初始化事件监听器
     */
    const initializeEventListeners = () => {
        // 计划相关事件
        EventSystem.on('plan-update', (details) => {
            if (!details) return;
            
            // 发出UI更新事件
            if (details.title) {
                EventSystem.emit(UI_EVENTS.MESSAGE_UPDATE, {
                    content: `正在执行: ${details.title}`,
                    type: 'status'
                });
            }
            
            if (details.steps && details.currentStepIndex !== null) {
                const currentStep = details.steps[details.currentStepIndex];
                if (currentStep) {
                    EventSystem.emit(UI_EVENTS.MESSAGE_UPDATE, {
                        content: `[${details.currentStepIndex + 1}/${details.steps.length}] ${currentStep}`,
                        type: 'step'
                    });
                }
            }
        });

        // 智能体执行事件
        EventSystem.on('agent-execution', (record) => {
            if (!record) return;
            
            // 发出添加section事件
            EventSystem.emit(UI_EVENTS.SECTION_ADD, {
                agentName: record.agentName,
                agentDescription: record.agentDescription,
                request: record.agentRequest,
                result: record.result
            });

            if (record.isCompleted) {
                EventSystem.emit(UI_EVENTS.MESSAGE_COMPLETE);
            }
        });

        // 计划完成事件
        EventSystem.on('plan-completed', (details) => {
            if (!details) return;
            EventSystem.emit(UI_EVENTS.MESSAGE_UPDATE, {
                content: details.summary ? `执行完成: ${details.summary}` : '执行完成',
                type: 'completion'
            });
            EventSystem.emit(UI_EVENTS.MESSAGE_COMPLETE);
            stopPolling();
            
            // 清空活动计划ID
            activePlanId = null;
            
            // 更新UI状态，启用发送按钮
            updateInputState(true);
        });
    };

    /**
     * 处理发送消息
     */
    const handleSendMessage = async () => {
        const query = inputField.value.trim();
        if (!query) return;
        
        // 如果当前有活动的计划正在执行，则不允许提交新任务
        if (activePlanId) {
            EventSystem.emit(UI_EVENTS.MESSAGE_UPDATE, {
                content: `当前有任务正在执行，请等待完成后再提交新任务`,
                type: 'error'
            });
            return;
        }
        
        // 清空输入框
        inputField.value = '';
        
        try {
            // 发送到API
            const response = await ManusAPI.sendMessage(query);
            
            // 更新任务ID并开始轮询
            activePlanId = response.planId;
            
            // 发出对话轮次开始事件
            EventSystem.emit(UI_EVENTS.DIALOG_ROUND_START, {
                planId: activePlanId,
                query: query
            });
            
            // 开始轮询
            startPolling();
            
        } catch (error) {
            EventSystem.emit(UI_EVENTS.MESSAGE_UPDATE, {
                content: `发送失败: ${error.message}`,
                type: 'error'
            });
            
            // 发生错误时，确保可以再次提交
            updateInputState(true);
        }
    };
    
    /**
     * 更新输入区域状态（启用/禁用）
     * @param {boolean} enabled - 是否启用输入
     */
    const updateInputState = (enabled) => {
        inputField.disabled = !enabled;
        sendButton.disabled = !enabled;
        
        if (!enabled) {
            sendButton.classList.add('disabled');
            inputField.setAttribute('placeholder', '正在处理中，请稍候...');
        } else {
            sendButton.classList.remove('disabled');
            inputField.setAttribute('placeholder', '向 Manus 发送消息');
        }
    };
    
    /**
     * 轮询计划执行状态
     */
    const pollPlanStatus = async () => {
        if (!activePlanId) return;
        
        // 如果已经在轮询中，跳过本次轮询
        if (isPolling) {
            console.log('上一次轮询尚未完成，跳过本次轮询');
            return;
        }
        
        try {
            isPolling = true; // 设置轮询状态为进行中
            
            const details = await ManusAPI.getDetails(activePlanId);
            
            // 发送计划更新事件
            EventSystem.emit('plan-update', details);
            
            // 如果有新的智能体执行记录，且sequence size增加了，才发送对应事件
            if (details.agentExecutionSequence) {
                const currentSize = details.agentExecutionSequence.length;
                if (currentSize > lastSequenceSize) {
                    // 只处理新增的记录
                    const newRecords = details.agentExecutionSequence.slice(lastSequenceSize);
                    newRecords.forEach(record => {
                        EventSystem.emit('agent-execution', record);
                    });
                    lastSequenceSize = currentSize;
                }
            }
            
            // 如果计划已完成，发送完成事件，重置sequence size并停止轮询
            if (details.completed) {
                EventSystem.emit('plan-completed', details);
                lastSequenceSize = 0; // 只在计划完成时重置
                stopPolling();
            }
            
        } catch (error) {
            console.error('轮询计划状态失败:', error);
        } finally {
            isPolling = false; // 无论成功或失败，都重置轮询状态
        }
    };
    
    /**
     * 开始轮询计划执行状态
     */
    const startPolling = () => {
        if (pollTimer) {
            clearInterval(pollTimer);
        }
        
        // 立即执行一次
        pollPlanStatus();
        
        // 设置定时轮询，间隔已增加到6秒
        pollTimer = setInterval(pollPlanStatus, POLL_INTERVAL);
    };
    
    /**
     * 停止轮询
     */
    const stopPolling = () => {
        if (pollTimer) {
            clearInterval(pollTimer);
            pollTimer = null;
        }
    };

    // 返回公开的方法和事件系统
    return {
        init,
        handleSendMessage,  // 确保导出 handleSendMessage
        EventSystem,
        UI_EVENTS
    };
})();
