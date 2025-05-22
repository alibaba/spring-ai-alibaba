/**
 * UI 模块 - 处理用户界面交互 ， 类似controller ，处理定期轮询，请求API数据等一系列动作，是基座。
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

    // 用户输入表单的容器
    let userInputFormContainer = null;
    
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
            TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.MESSAGE_UPDATE, {
                content: `当前有任务正在执行，请等待完成后再提交新任务`,
                type: 'error',
                planId: activePlanId
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
            TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.DIALOG_ROUND_START, {
                planId: activePlanId, 
                query: query
            });
            
            // 开始轮询
            startPolling();
            
        } catch (error) {
            TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.MESSAGE_UPDATE, {
                content: `发送失败: ${error.message}`,
                type: 'error',
                planId: activePlanId
            });
            
            // 发生错误时，确保可以再次提交
            updateInputState(true);
        }
    };

    /**
     * 更新输入区域的状态（启用/禁用）
     */
    const updateInputState = (enabled) => {
        inputField.disabled = !enabled;
        sendButton.disabled = !enabled;
        // 可选：更改样式以反映禁用状态
        if (!enabled) {
            inputField.placeholder = '等待用户输入...';
            inputField.classList.add('disabled');
            sendButton.classList.add('disabled');
        } else {
            inputField.placeholder = '向 JTaskPilot 发送消息';
            inputField.classList.remove('disabled');
            sendButton.classList.remove('disabled');
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
                    updateInputState(true); // 恢复输入框
                }
                return; 
            }

            // 首先，确保UI已更新以反映当前步骤，这样表单可以被放置在正确的位置
            TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.PLAN_UPDATE, { ...details, planId: activePlanId });

            // 然后，检查是否需要用户输入
            const userInputState = await ManusAPI.checkWaitForInput(activePlanId);
            if (userInputState && userInputState.waiting) {
                console.log('轮询：检测到需要用户输入', userInputState);
                displayUserInputForm(userInputState, details); // 传递details给表单显示函数
                updateInputState(false); // 禁用主输入框
                // 注意：这里我们不停止轮询 (stopPolling())，因为用户提交后，我们希望继续当前计划的轮询
                // 但是，我们需要从pollPlanStatus返回，以避免在等待用户输入时处理计划完成等逻辑
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
     * 显示用户输入表单
     * @param {Object} userInputState - 后端返回的等待输入状态
     * @param {Object} planDetails - 当前的计划详情，用于定位表单位置
     */
    const displayUserInputForm = (userInputState, planDetails) => {
        removeUserInputForm(); // 移除已有的表单

        userInputFormContainer = document.createElement('div');
        userInputFormContainer.className = 'user-input-form-container'; // 样式类名保持不变

        let formHTML = `<p class="user-input-message">${userInputState.message || '请输入所需信息:'}</p>`;
        if (userInputState.formDescription) {
            formHTML += `<p class="form-description">${userInputState.formDescription}</p>`;
        }

        formHTML += '<form id="userInputForm">';
        if (userInputState.formInputs && userInputState.formInputs.length > 0) {
            userInputState.formInputs.forEach(input => {
                // 为input的id和name创建一个更安全的版本，例如替换空格和特殊字符
                const safeId = input.label.replace(/\W+/g, '_');
                formHTML += `
                    <div class="form-group">
                        <label for="form-input-${safeId}">${input.label}:</label>
                        <input type="text" id="form-input-${safeId}" name="${input.label}" value="${input.value || ''}" required>
                    </div>
                `;
            });
        } else {
            formHTML += `
                <div class="form-group">
                    <label for="form-input-genericInput">输入:</label>
                    <input type="text" id="form-input-genericInput" name="genericInput" required>
                </div>
            `;
        }
        formHTML += '<button type="submit" class="submit-user-input-btn">提交</button>';
        formHTML += '</form>';

        userInputFormContainer.innerHTML = formHTML;

        // 定位表单的插入位置
        const dialogRoundContainer = ChatHandler.findDialogRoundContainerByPlanId(planDetails.planId);
        if (dialogRoundContainer) {
            const stepsContainer = dialogRoundContainer.querySelector('.ai-steps-container');
            if (stepsContainer) {
                const allAiSections = stepsContainer.querySelectorAll('.ai-section');
                if (allAiSections && allAiSections.length > planDetails.currentStepIndex) {
                    const currentStepSection = allAiSections[planDetails.currentStepIndex];
                    currentStepSection.appendChild(userInputFormContainer);
                } else {
                    console.warn('无法找到当前步骤的ai-section来放置用户输入表单，将放置在聊天区域底部。');
                    chatArea.appendChild(userInputFormContainer); // Fallback
                }
            } else {
                console.warn('无法找到ai-steps-container来放置用户输入表单，将放置在聊天区域底部。');
                chatArea.appendChild(userInputFormContainer); // Fallback
            }
        } else {
            console.warn('无法找到dialogRoundContainer来放置用户输入表单，将放置在聊天区域底部。');
            chatArea.appendChild(userInputFormContainer); // Fallback
        }
        
        scrollToElement(userInputFormContainer); // 滚动到表单

        const form = userInputFormContainer.querySelector('#userInputForm');
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const formData = new FormData(form);
            const inputs = {};
            formData.forEach((value, key) => {
                inputs[key] = value;
            });

            try {
                // 在提交前禁用表单，防止重复提交
                form.querySelector('.submit-user-input-btn').disabled = true;
                form.querySelector('.submit-user-input-btn').textContent = '提交中...';

                await ManusAPI.submitFormInput(activePlanId, inputs); // 使用 submitFormInput
                removeUserInputForm();
                updateInputState(true); // 重新启用主输入框
                
                // 用户提交后，立即再次轮询以获取更新的状态
                // 不再需要手动调用 startPolling()，因为 pollPlanStatus 的 return 被移除了
                // 并且外层轮询定时器仍在运行。我们只需确保下一次轮询会发生。
                // 为了立即反馈，可以手动触发一次 pollPlanStatus
                console.log('用户输入已提交，将立即轮询最新状态。');
                pollPlanStatus(); 

            } catch (error) {
                console.error('提交用户输入失败:', error);
                const errorMsg = document.createElement('p');
                errorMsg.className = 'error-message'; // 确保这个类有样式
                errorMsg.textContent = `提交失败: ${error.message}`;
                // 在表单内部显示错误，而不是替换整个表单
                const existingError = form.querySelector('.error-message');
                if (existingError) {
                    existingError.remove();
                }
                form.appendChild(errorMsg);
                // 重新启用提交按钮
                form.querySelector('.submit-user-input-btn').disabled = false;
                form.querySelector('.submit-user-input-btn').textContent = '提交';
            }
        });
    };

    /**
     * 移除用户输入表单
     */
    const removeUserInputForm = () => {
        if (userInputFormContainer) {
            userInputFormContainer.remove();
            userInputFormContainer = null;
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
        handleSendMessage,
        activePlanId, // 暴露 activePlanId 以便其他模块可能需要访问
        updateInputState // 暴露以便外部可以控制输入状态
    };
})();
