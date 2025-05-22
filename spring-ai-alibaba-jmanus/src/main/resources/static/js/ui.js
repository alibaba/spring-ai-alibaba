/**
 * UI 模块 - 处理用户界面交互 ， 类似controller ，处理定期轮询，请求API数据等一系列动作，是基座。
 */
const PlanExecutionManager = (() => {
    
    // 当前活动的任务ID
    let activePlanId = null;
    
    // 记录上一次sequence的大小
    let lastSequenceSize = 0;
    
    // 轮询间隔（毫秒） - 从2秒增加到6秒
    const POLL_INTERVAL = 5000;
    
    // 轮询定时器
    let pollTimer = null;
    
    // 轮询并发控制标志
    let isPolling = false;

    /**
     * 初始化UI组件
     */
    const init = () => {
  
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
            TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.CHAT_INPUT_UPDATE_STATE, { enabled: true }); // CORRECTED
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
        TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.CHAT_INPUT_CLEAR); // CORRECTED
        // 禁用输入框 (通过事件通知 ChatInputHandler)
        TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.CHAT_INPUT_UPDATE_STATE, { enabled: false, placeholder: '处理中...' }); // CORRECTED

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
            TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.CHAT_INPUT_UPDATE_STATE, { enabled: true }); // CORRECTED
        }
    };

    /**
     * 处理计划完成的通用逻辑
     * @param {object} details - 计划详情
     */
    const _handlePlanCompletion = (details) => {
        TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.PLAN_COMPLETED, { ...details, planId: activePlanId });
        lastSequenceSize = 0; // 只在计划完成时重置
        stopPolling();

        // 计划完成后，删除后端执行详情记录释放资源
        try {
            // 延迟一段时间后删除，确保前端已完全处理完成事件
            setTimeout(async () => {
                if (!activePlanId) return; // 确保 activePlanId 仍然有效
                await fetch(`${ManusAPI.BASE_URL}/details/${activePlanId}`, {
                    method: 'DELETE'
                });
                console.log(`已删除已完成的计划执行记录: ${activePlanId}`);
                activePlanId = null; // 在成功删除后也清空
            }, 5000); // 5秒后删除
        } catch (error) {
            console.log(`删除计划执行记录失败: ${error.message}`);
        }
        // 如果不是通过正常轮询停止的，也需要确保 activePlanId 被清空
        if (details.completed) { // 确保只有在真正完成后才清空
             activePlanId = null;
             TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.CHAT_INPUT_UPDATE_STATE, { enabled: true });
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
                isPolling = false; // 重置轮询状态
                return;
            }
            
            // 如果没有获取到详情，或者详情中没有步骤，则不进行处理
            if (!details.steps || details.steps.length === 0) {
                console.log('轮询：未获取到有效详情或步骤为空', details);
                // 如果planId存在但获取不到details，可能是plan已结束或被删除
                // 或者如果 details.completed 为 true，也表示计划已结束
                if (details.completed) {
                    console.log(`轮询：Plan ${activePlanId} 已完成但无步骤，处理完成逻辑。`);
                    _handlePlanCompletion(details);
                } else if (activePlanId && !details.planId) { 
                    console.log(`轮询：Plan ${activePlanId} 可能已结束或被删除（无planId返回），停止轮询。`);
                    stopPolling(); // 停止轮询
                    activePlanId = null; // 清空活动ID
                    TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.CHAT_INPUT_UPDATE_STATE, { enabled: true });
                }
                isPolling = false; // 重置轮询状态
                return; 
            }

            // 首先，确保UI已更新以反映当前步骤，这样表单可以被放置在正确的位置
            TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.PLAN_UPDATE, { ...details, planId: activePlanId });

            // 然后，检查是否需要用户输入
            const userInputState = await ManusAPI.checkWaitForInput(activePlanId);
            if (userInputState && userInputState.waiting) {
                console.log('轮询：检测到需要用户输入', userInputState);
                // ChatHandler.displayUserInputForm(userInputState, details, chatArea); // 旧的直接调用方式
                TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.USER_INPUT_FORM_DISPLAY_REQUESTED, {
                    userInputState: userInputState,
                    planDetails: details
                    // fallbackChatArea: chatArea // REMOVED
                });
                TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.CHAT_INPUT_UPDATE_STATE, { enabled: false, placeholder: '等待用户在表单中输入...' });
                isPolling = false; // 重置轮询状态，因为等待用户输入，暂时不继续轮询
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
                _handlePlanCompletion(details); // 调用新的私有函数处理完成逻辑
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
        // console.log('轮询已停止'); // 可以取消注释以进行调试
    };

    // 公开UI模块的方法和事件系统
    return {
        init,
        // handleSendMessage, // 不再直接暴露，通过事件触发
        get activePlanId() { return activePlanId; }, // 通过 getter 暴露 activePlanId
      
    };
})();
