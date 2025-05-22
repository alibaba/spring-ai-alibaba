/**
 * 聊天内容处理模块 , 对应聊天里面用户/Ai的对话显示部分
 */
const ChatHandler = (() => {
    let chatArea;
    let lastAgentExecutionId = null;
    let currentDialogRoundId = null;
    let dialogRoundPlans = new Map(); // 存储对话轮次和planId的映射关系
    let userInputFormContainer = null; // 从 ManusUI 移入
    
    /**
     * 初始化聊天处理器
     */
    const init = () => {
        chatArea = document.querySelector('.chat-area');
        
        // 订阅业务事件
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.PLAN_UPDATE, handlePlanUpdate);
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.PLAN_COMPLETED, handlePlanComplete);
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.DIALOG_ROUND_START, handleDialogRoundStart);
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.USER_INPUT_FORM_DISPLAY_REQUESTED, handleDisplayUserInputFormEvent); // 新增订阅
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.USER_INPUT_FORM_REMOVE_REQUESTED, removeUserInputForm); // 新增订阅
    };
    
    /**
     * 开始新的对话轮次
     */
    const startNewDialogRound = (planId) => {
        currentDialogRoundId = Date.now().toString();
        dialogRoundPlans.set(currentDialogRoundId, planId);
        return currentDialogRoundId;
    };

    /**
     * 处理对话轮次开始事件
     */
    const handleDialogRoundStart = (eventData) => {
        const { planId, query } = eventData;
        // 创建新的对话轮次
        const dialogRoundId = startNewDialogRound(planId);
        
        // 创建对话轮次容器
        const dialogRoundContainer = document.createElement('div');
        dialogRoundContainer.className = 'dialog-round-container';
        dialogRoundContainer.dataset.dialogRoundId = dialogRoundId;
        dialogRoundContainer.dataset.planId = planId;
        chatArea.appendChild(dialogRoundContainer);
        
        // 添加用户消息
        const messageElement = createMessageElement('user-message', query);
        dialogRoundContainer.appendChild(messageElement);
        scrollToBottom();
    };
    
    /**
     * 处理计划更新
     */
    const handlePlanUpdate = (planDetails) => {
        if (!planDetails.steps || !planDetails.steps.length) return;

        // 根据 planId 找到对应的对话轮次容器
        const dialogRoundContainer = findDialogRoundContainerByPlanId(planDetails.planId);
        if (!dialogRoundContainer) return;
        
        // 查找或创建步骤容器
        let stepsContainer = dialogRoundContainer.querySelector('.ai-steps-container');
        if (!stepsContainer) {
            stepsContainer = document.createElement('div');
            stepsContainer.className = 'message ai-message ai-steps-container';
            dialogRoundContainer.appendChild(stepsContainer);
        }
        
        // 更新步骤显示
        updateStepsDisplay(planDetails, stepsContainer);
    };

    /**
     * 根据 planId 查找对话轮次容器
     */
    const findDialogRoundContainerByPlanId = (planId) => {
        return document.querySelector(`.dialog-round-container[data-plan-id="${planId}"]`);
    };
    
    /**
     * 处理计划完成
     */
    const handlePlanComplete = (details) => {
        if (!details?.planId) return;
        
        // 根据 planId 找到对应的对话轮次容器
        const dialogRoundContainer = findDialogRoundContainerByPlanId(details.planId);
        if (!dialogRoundContainer || !details?.summary) return;
        
        // 创建AI消息元素
        const aiMessageElement = document.createElement('div');
        aiMessageElement.className = 'message ai-message';
        
        // 创建AI消息头部
        const headerDiv = document.createElement('div');
        headerDiv.className = 'ai-header';
        headerDiv.innerHTML = '<span class="ai-logo">M</span> Manus AI';
        
        // 创建消息内容区域
        const contentDiv = document.createElement('div');
        contentDiv.className = 'ai-content';
        contentDiv.innerHTML = `<p>${formatSummaryContent(details.summary)}</p>`;
        
        // 组装消息元素
        aiMessageElement.appendChild(headerDiv);
        aiMessageElement.appendChild(contentDiv);
        
        // 添加到对话轮次容器
        dialogRoundContainer.appendChild(aiMessageElement);
        scrollToBottom();
    };

    /**
     * 格式化总结内容
     * 处理可能包含的markdown、代码块等格式
     */
    const formatSummaryContent = (summary) => {
        if (!summary) return '';
        
        // 替换换行符为HTML换行
        let formattedText = summary.replace(/\n/g, '<br>');
        
        // 处理markdown格式的代码块
        formattedText = formattedText.replace(/```(\w*)\n([\s\S]*?)```/g, (match, language, code) => {
            return `<pre><code class="language-${language || 'text'}">${escapeHtml(code)}</code></pre>`;
        });
        
        // 处理粗体文本
        formattedText = formattedText.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
        
        // 处理斜体文本
        formattedText = formattedText.replace(/\*(.*?)\*/g, '<em>$1</em>');
        
        return formattedText;
    };

    /**
     * 更新步骤显示
     */
    const updateStepsDisplay = (planDetails, stepsContainer) => {
        if (!planDetails.steps || !planDetails.steps.length) return;
        
        // 初始化存储每个步骤的最后执行动作（现在是方法级变量）
        let lastStepActions = new Array(planDetails.steps.length).fill(null);
        
        // 遍历所有执行序列，匹配步骤并更新动作
        if (planDetails.agentExecutionSequence?.length > 0) {
            let index = 0;
            planDetails.agentExecutionSequence.forEach(execution => {
                // 使用stepIndex属性确定此执行记录属于哪个步骤
                if (execution?.thinkActSteps?.length > 0) {
                    const latestThinkAct = execution.thinkActSteps[execution.thinkActSteps.length - 1];
                    if (latestThinkAct?.actionDescription && latestThinkAct?.toolParameters) {
                        // 保存此步骤的最后执行动作
                        lastStepActions[index] = {
                            actionDescription: latestThinkAct.actionDescription,
                            toolParameters: latestThinkAct.toolParameters,
                            thinkInput: latestThinkAct.thinkInput || '',
                            thinkOutput: latestThinkAct.thinkOutput || ''
                        };
                    } else {
                        lastStepActions[index] = {
                            actionDescription: latestThinkAct.thinkOutput || '执行完成',
                            toolParameters: "无工具",
                            thinkInput: latestThinkAct.thinkInput || '',
                            thinkOutput: latestThinkAct.thinkOutput || ''
                        };
                    }
                }
                index++;
            });
        }
        
        // 渲染所有步骤
        const stepsContent = planDetails.steps.map((step, index) => {
            const stepDiv = document.createElement('div');
            stepDiv.className = `ai-section ${index === planDetails.currentStepIndex ? 'current' : ''}`;
            
            // 创建步骤标题
            stepDiv.innerHTML = `
                <div class="section-header">
                    <span class="icon">${index < planDetails.currentStepIndex ? '✓' : index === planDetails.currentStepIndex ? '▶' : '○'}</span>
                    <span>${escapeHtml(step)}</span>
                </div>
            `;

            // 获取该步骤的最后执行动作（现在使用局部变量）
            const lastAction = lastStepActions[index];
            
            // 简化逻辑：如果有lastAction就显示动作信息，不区分是否是当前步骤
            if (lastAction) {
                const actionInfoDiv = document.createElement('div');
                actionInfoDiv.className = 'action-info';
                actionInfoDiv.innerHTML = `
                    <div class="action-description">
                        <span class="icon">${index === planDetails.currentStepIndex ? '🔄' : '✓'}</span>
                        ${escapeHtml(lastAction.actionDescription)}
                    </div>
                    <div class="tool-params">
                        <span class="icon">⚙️</span>
                        参数: ${escapeHtml(lastAction.toolParameters)}
                    </div>
                    <div class="think-details" style="margin-top: 8px; border-top: 1px dashed #e8eaed; padding-top: 6px;">
                        <div class="think-output" style="font-size: 12px; color: #5f6368;">
                            <span style="font-weight: bold;">思考输出:</span> 
                            <span>${escapeHtml(lastAction.thinkOutput || '')}</span>
                        </div>
                    </div>
                `;
                stepDiv.appendChild(actionInfoDiv);
            }

            return stepDiv.outerHTML;
        }).join('');
        
        stepsContainer.innerHTML = stepsContent;
    };
    
    /**
     * 创建消息元素
     */
    const createMessageElement = (className, content) => {
        const div = document.createElement('div');
        div.className = `message ${className}`;
        div.innerHTML = `<p>${escapeHtml(content)}</p>`;
        return div;
    };
    
    /**
     * HTML转义
     */
    const escapeHtml = (text) => {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    };
    
    /**
     * 滚动到底部
     */
    const scrollToBottom = () => {
        if (chatArea && chatArea.scrollHeight !== undefined) {
            chatArea.scrollTop = chatArea.scrollHeight;
        }
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

    /**
     * 处理显示用户输入表单的事件 (新增)
     * @param {Object} eventData - 事件数据，包含 userInputState, planDetails, fallbackChatArea
     */
    const handleDisplayUserInputFormEvent = (eventData) => {
        const { userInputState, planDetails } = eventData; // Removed fallbackChatArea
        // 调用内部的 displayUserInputForm 方法
        displayUserInputFormInternal(userInputState, planDetails, chatArea); // Pass ChatHandler's own chatArea
    };
    
    /**
     * 显示用户输入表单 (内部实现，原 displayUserInputForm)
     * @param {Object} userInputState - 后端返回的等待输入状态
     * @param {Object} planDetails - 当前的计划详情，用于定位表单位置
     * @param {HTMLElement} currentChatAreaParam - The chat area to append to as a fallback
     */
    const displayUserInputFormInternal = (userInputState, planDetails, currentChatAreaParam) => {
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
        const dialogRoundContainer = findDialogRoundContainerByPlanId(planDetails.planId);
        // const currentChatArea = chatArea || fallbackChatArea; // 使用 ChatHandler 的 chatArea 或传入的 fallback

        if (dialogRoundContainer) {
            const stepsContainer = dialogRoundContainer.querySelector('.ai-steps-container');
            if (stepsContainer) {
                const allAiSections = stepsContainer.querySelectorAll('.ai-section');
                if (allAiSections && allAiSections.length > planDetails.currentStepIndex) {
                    const currentStepSection = allAiSections[planDetails.currentStepIndex];
                    currentStepSection.appendChild(userInputFormContainer);
                } else {
                    console.warn('无法找到当前步骤的ai-section来放置用户输入表单，将放置在聊天区域底部。');
                    if (currentChatAreaParam) currentChatAreaParam.appendChild(userInputFormContainer); // Fallback
                }
            } else {
                console.warn('无法找到ai-steps-container来放置用户输入表单，将放置在聊天区域底部。');
                if (currentChatAreaParam) currentChatAreaParam.appendChild(userInputFormContainer); // Fallback
            }
        } else {
            console.warn('无法找到dialogRoundContainer来放置用户输入表单，将放置在聊天区域底部。');
            if (currentChatAreaParam) currentChatAreaParam.appendChild(userInputFormContainer); // Fallback
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

                await ManusAPI.submitFormInput(PlanExecutionManager.activePlanId, inputs); 
                removeUserInputForm();
                
            } catch (error) {
                console.error('提交用户输入失败:', error);
                const errorMsg = document.createElement('p');
                errorMsg.className = 'error-message'; 
                errorMsg.textContent = `提交失败: ${error.message}`;
                const existingError = form.querySelector('.error-message');
                if (existingError) {
                    existingError.remove();
                }
                form.appendChild(errorMsg);
                form.querySelector('.submit-user-input-btn').disabled = false;
                form.querySelector('.submit-user-input-btn').textContent = '提交';
            }
        });
    };

    /**
     * 移除用户输入表单 (从 ManusUI 移入)
     */
    const removeUserInputForm = () => {
        if (userInputFormContainer) {
            userInputFormContainer.remove();
            userInputFormContainer = null;
        }
    };

    // /**
    //  * 将动态元素附加到指定的对话轮次，并滚动到该元素。
    //  * @param {string} planId - 计划ID，用于查找对应的对话轮次容器。
    //  * @param {HTMLElement} element - 要附加的HTML元素。
    //  */
    // const appendDynamicElementToDialogRound = (planId, element) => {
    //     const dialogRoundContainer = findDialogRoundContainerByPlanId(planId);
    //     if (dialogRoundContainer) {
    //         const stepsContainer = dialogRoundContainer.querySelector('.ai-steps-container');
    //         if (stepsContainer && stepsContainer.parentNode === dialogRoundContainer) {
    //             // 插入到步骤容器之后
    //             dialogRoundContainer.insertBefore(element, stepsContainer.nextSibling);
    //         } else {
    //             // 追加到对话轮次容器的末尾
    //             dialogRoundContainer.appendChild(element);
    //         }
    //         scrollToElement(element);
    //     } else {
    //         console.warn(`ChatHandler: Plan ID ${planId} 对应的对话轮次容器未找到。将元素附加到主聊天区域。`);
    //         chatArea.appendChild(element); // Fallback
    //         scrollToElement(element);
    //     }
    // };

    // /**
    //  * 从聊天区域或指定的对话轮次中移除元素。
    //  * @param {string} elementSelector - 要移除的元素的CSS选择器。
    //  * @param {string} [planIdContext] - 可选的计划ID，如果提供，则只在对应的对话轮次容器内查找并移除。
    //  */
    // const removeElementFromChat = (elementSelector, planIdContext) => {
    //     let scope = chatArea; // 默认搜索范围是整个聊天区域

    //     if (planIdContext) {
    //         const dialogRoundContainer = findDialogRoundContainerByPlanId(planIdContext);
    //         if (dialogRoundContainer) {
    //             scope = dialogRoundContainer; // 在特定的对话轮次容器内搜索
    //         } else {
    //             console.warn(`ChatHandler: Plan ID ${planIdContext} 对应的对话轮次容器未找到。无法移除元素。`);
    //             return; 
    //         }
    //     }

    //     const elementToRemove = scope.querySelector(elementSelector);
    //     if (elementToRemove) {
    //         elementToRemove.remove();
    //     } else {
    //         // console.warn(`ChatHandler: 选择器 "${elementSelector}" 对应的元素在指定范围内未找到。`);
    //     }
    // };
    
    // 返回公开方法
    return {
        init
    };
})();
