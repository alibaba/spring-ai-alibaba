/**
 * UI 模块 - 处理用户界面交互 ， 类似controller ，处理定期轮询，请求API数据等一系列动作，是基座。
 */
const ManusUI = (() => {
    // 缓存DOM元素
    let chatArea;
    // let inputField;
    // let sendButton;
    
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

    /**
     * 初始化UI组件
     */
    const init = () => {
        // 获取DOM元素
        chatArea = document.querySelector('.chat-area');
        // inputField = document.querySelector('.input-area input'); // 由 ChatInputHandler 管理
        // sendButton = document.querySelector('.send-btn'); // 由 ChatInputHandler 管理
        
        // ChatInputHandler.init(handleSendMessage); // 旧的初始化方式
        ChatInputHandler.init(); // ChatInputHandler 现在通过事件通信

        // 添加事件监听器
        // sendButton.addEventListener('click', handleSendMessage); // 由 ChatInputHandler 管理
        // inputField.addEventListener('keypress', (e) => { // 由 ChatInputHandler 管理
        //     if (e.key === 'Enter') {
        //         handleSendMessage();
        //     }
        // });
        
        // 初始化事件监听
        initializeEventListeners();
        
        console.log('Manus UI 初始化完成');
    };
    
    /**
     * 初始化事件监听器
     */
    const initializeEventListeners = () => {
        // 计划相关事件
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.PLAN_UPDATE, (details) => {
            if (!details) return;
            
            // 发出UI更新事件
            if (details.title) {
                TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.MESSAGE_UPDATE, {
                    content: `正在执行: ${details.title}`,
                    type: 'status',
                    planId: activePlanId
                });
            }
            
            if (details.steps && details.currentStepIndex !== null) {
                const currentStep = details.steps[details.currentStepIndex];
                if (currentStep) {
                    TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.MESSAGE_UPDATE, {
                        content: `[${details.currentStepIndex + 1}/${details.steps.length}] ${currentStep}`,
                        type: 'step',
                        planId: activePlanId
                    });
                }
            }
        });

        // 智能体执行事件
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.AGENT_EXECUTION, (record) => {
            if (!record) return;
            
            // 发出添加section事件
            TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.SECTION_ADD, {
                agentName: record.agentName,
                agentDescription: record.agentDescription,
                request: record.agentRequest,
                result: record.result,
                planId: activePlanId
            });

            if (record.isCompleted) {
                TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.MESSAGE_COMPLETE, { planId: activePlanId });
            }
        });

        // 计划完成事件
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.PLAN_COMPLETED, (details) => {
            if (!details) return;
            TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.MESSAGE_UPDATE, {
                content: details.summary ? `执行完成: ${details.summary}` : '执行完成',
                type: 'completion',
                planId: activePlanId
            });
            TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.MESSAGE_COMPLETE, { planId: activePlanId });
            stopPolling();
            
            // 清空活动计划ID
            activePlanId = null;
            
            // 更新UI状态，启用发送按钮
            // updateInputState(true); // 改为通过事件通知 ChatInputHandler
            TaskPilotUIEvent.EventSystem.emit('chatinput:updatestate', { enabled: true });
        });

        // 新增：监听用户请求发送消息的事件
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.USER_MESSAGE_SEND_REQUESTED, handleUserMessageSendRequested);
    };

    /**
     * 处理用户请求发送消息的事件
     * @param {object} eventData - 包含 query 的事件数据
     */
    const handleUserMessageSendRequested = async (eventData) => {
        const { query } = eventData;
        if (!query) return;

        // 如果当前有活动的计划正在执行，则不允许提交新任务
        if (activePlanId) {
            TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.MESSAGE_UPDATE, {
                content: `当前有任务正在执行，请等待完成后再提交新任务`,
                type: 'error',
                planId: activePlanId
            });
            return;
        }

        // 清空输入框 (通过事件通知 ChatInputHandler)
        TaskPilotUIEvent.EventSystem.emit('chatinput:clear');
        // 禁用输入框 (通过事件通知 ChatInputHandler)
        TaskPilotUIEvent.EventSystem.emit('chatinput:updatestate', { enabled: false, placeholder: '处理中...' });

        try {
            // 发送到API
            const response = await ManusAPI.sendMessage(query);
            
            // 更新任务ID并开始轮询
            activePlanId = response.planId;
            
            // 发出对话轮次开始事件
            TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.DIALOG_ROUND_START, {
                planId: activePlanId, 
                query: query
            });
            
            // 开始轮询 (轮询内部会在适当时机通过事件恢复输入框状态)
            startPolling();
            
        } catch (error) {
            TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.MESSAGE_UPDATE, {
                content: `发送失败: ${error.message}`,
                type: 'error',
                planId: activePlanId
            });
            
            // 发生错误时，确保可以再次提交 (通过事件通知 ChatInputHandler)
            // updateInputState(true);
            TaskPilotUIEvent.EventSystem.emit('chatinput:updatestate', { enabled: true });
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
            
            // 如果details为null（例如404错误），则跳过处理
            if (!details) {
                console.log(`无法获取计划 ${activePlanId} 的详情`);
                return;
            }
            
            // 如果没有获取到详情，或者详情中没有步骤，则不进行处理
            if (!details || !details.steps || details.steps.length === 0) {
                console.log('轮询：未获取到有效详情或步骤为空', details);
                // 如果planId存在但获取不到details，可能是plan已结束或被删除
                if (activePlanId && !details) {
                    console.log(`轮询：Plan ${activePlanId} 可能已结束或被删除，停止轮询。`);
                    stopPolling(); // 停止轮询
                    activePlanId = null; // 清空活动ID
                    // updateInputState(true); // 恢复输入框 (通过事件通知 ChatInputHandler)
                    TaskPilotUIEvent.EventSystem.emit('chatinput:updatestate', { enabled: true });
                }
                return; 
            }

            // 首先，确保UI已更新以反映当前步骤，这样表单可以被放置在正确的位置
            TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.PLAN_UPDATE, { ...details, planId: activePlanId });

            // 然后，检查是否需要用户输入
            const userInputState = await ManusAPI.checkWaitForInput(activePlanId);
            if (userInputState && userInputState.waiting) {
                console.log('轮询：检测到需要用户输入', userInputState);
                ChatHandler.displayUserInputForm(userInputState, details, chatArea); 
                // updateInputState(false); // 禁用主输入框 (通过事件通知 ChatInputHandler)
                TaskPilotUIEvent.EventSystem.emit('chatinput:updatestate', { enabled: false, placeholder: '等待用户在表单中输入...' });
                return; 
            }

            
            // 如果有新的智能体执行记录，且sequence size增加了，才发送对应事件
            if (details.agentExecutionSequence) {
                const currentSize = details.agentExecutionSequence.length;
                if (currentSize > lastSequenceSize) {
                    // 只处理新增的记录
                    const newRecords = details.agentExecutionSequence.slice(lastSequenceSize);
                    newRecords.forEach(record => {
                        TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.AGENT_EXECUTION, { ...record, planId: activePlanId }); // MODIFIED & Ensured planId
                    });
                    lastSequenceSize = currentSize;
                }
            }
            
            // 如果计划已完成，发送完成事件，重置sequence size并停止轮询
            if (details.completed) {
                TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.PLAN_COMPLETED, { ...details, planId: activePlanId }); // MODIFIED & Ensured planId
                lastSequenceSize = 0; // 只在计划完成时重置
                stopPolling();
                
                // 计划完成后，删除后端执行详情记录释放资源
                try {
                    // 延迟一段时间后删除，确保前端已完全处理完成事件
                    setTimeout(async () => {
                        await fetch(`${ManusAPI.BASE_URL}/details/${activePlanId}`, {
                            method: 'DELETE'
                        });
                        console.log(`已删除已完成的计划执行记录: ${activePlanId}`);
                    }, 5000); // 5秒后删除
                } catch (error) {
                    console.log(`删除计划执行记录失败: ${error.message}`);
                }
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

    /**
     * 滚动到聊天区域底部
     */
    const scrollToBottom = () => {
        chatArea.scrollTop = chatArea.scrollHeight;
    };

    /**
     * 滚动到指定元素
     * @param {HTMLElement} element 
     */
    const scrollToElement = (element) => {
        if (element && typeof element.scrollIntoView === 'function') {
            element.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        } else {
            scrollToBottom(); // Fallback
        }
    };

    // 公开UI模块的方法和事件系统
    return {
        init,
        // handleSendMessage, // 不再直接暴露，通过事件触发
        activePlanId, 
        // updateInputState // 不再直接暴露，通过事件控制
        pollPlanStatus // 暴露 pollPlanStatus 以便 ChatHandler 中的 displayUserInputForm 可以调用
    };
})();
