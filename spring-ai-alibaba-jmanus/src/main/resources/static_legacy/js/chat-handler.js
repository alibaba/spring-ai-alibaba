/**
 * 聊天内容处理模块 , 对应聊天里面用户/Ai的对话显示部分
 */
class ChatHandler {
    #chatArea;
    #lastAgentExecutionId = null; // Kept as per original, though not used
    #currentDialogRoundId = null;
    #dialogRoundPlans = new Map(); // 存储对话轮次和planId的映射关系
    #userInputFormContainer = null; // 从 ManusUI 移入
    #planExecutionManager; // Store the PlanExecutionManager instance

    /**
     * 初始化聊天处理器
     */
    constructor(planExecutionManagerInstance) { // Accept PlanExecutionManager instance
        this.#planExecutionManager = planExecutionManagerInstance; // Store the instance
        this.#chatArea = document.querySelector('.chat-area');

        // 订阅业务事件
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.PLAN_UPDATE, this.#handlePlanUpdate.bind(this));
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.PLAN_COMPLETED, this.#handlePlanComplete.bind(this));
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.DIALOG_ROUND_START, this.#handleDialogRoundStart.bind(this));
       }

    /**
     * 开始新的对话轮次
     */
    #startNewDialogRound(planId) {
        this.#currentDialogRoundId = Date.now().toString();
        this.#dialogRoundPlans.set(this.#currentDialogRoundId, planId);
        return this.#currentDialogRoundId;
    }

    /**
     * 处理对话轮次开始事件
     */
    #handleDialogRoundStart(eventData) {
        const { planId, query } = eventData;
        // 创建新的对话轮次
        const dialogRoundId = this.#startNewDialogRound(planId);

        // 创建对话轮次容器
        const dialogRoundContainer = document.createElement('div');
        dialogRoundContainer.className = 'dialog-round-container';
        dialogRoundContainer.dataset.dialogRoundId = dialogRoundId;
        dialogRoundContainer.dataset.planId = planId;
        this.#chatArea.appendChild(dialogRoundContainer);

        // 添加用户消息
        const messageElement = ChatHandler.#createMessageElement('user-message', query);
        dialogRoundContainer.appendChild(messageElement);
        this.#scrollToBottom();
    }

    /**
     * 处理计划更新
     */
    #handlePlanUpdate(planDetails) {
        if (!planDetails.steps || !planDetails.steps.length) return;

        // 根据 planId 找到对应的对话轮次容器
        const dialogRoundContainer = ChatHandler.#findDialogRoundContainerByPlanId(planDetails.planId);
        if (!dialogRoundContainer) return;

        // 查找或创建步骤容器
        let stepsContainer = dialogRoundContainer.querySelector('.ai-steps-container');
        if (!stepsContainer) {
            stepsContainer = document.createElement('div');
            stepsContainer.className = 'message ai-message ai-steps-container';
            dialogRoundContainer.appendChild(stepsContainer);
        }

        // 更新步骤显示
        this.#updateStepsDisplay(planDetails, stepsContainer);

        // 如果需要用户输入，则显示表单
        if (planDetails.userInputWaitState) {
            this.#displayUserInputFormInternal(planDetails.userInputWaitState, planDetails, this.#chatArea);
        } else {
            this.#removeUserInputForm(); // 如果 userInputWaitState 为空，则移除表单
        }
    }

    /**
     * 根据 planId 查找对话轮次容器 (Static private as it doesn't use 'this')
     */
    static #findDialogRoundContainerByPlanId(planId) {
        return document.querySelector(`.dialog-round-container[data-plan-id="${planId}"]`);
    }

    /**
     * 处理计划完成
     */
    #handlePlanComplete(details) {
        if (!details?.planId) return;

        // 根据 planId 找到对应的对话轮次容器
        const dialogRoundContainer = ChatHandler.#findDialogRoundContainerByPlanId(details.planId);
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
        contentDiv.innerHTML = `<p>${ChatHandler.#formatSummaryContent(details.summary)}</p>`;

        // 组装消息元素
        aiMessageElement.appendChild(headerDiv);
        aiMessageElement.appendChild(contentDiv);

        // 添加到对话轮次容器
        dialogRoundContainer.appendChild(aiMessageElement);
        this.#scrollToBottom();
    }

    /**
     * 格式化总结内容 (Static private as it doesn't use 'this' directly, uses another static private)
     * 处理可能包含的markdown、代码块等格式
     */
    static #formatSummaryContent(summary) {
        if (!summary) return '';

        // 替换换行符为HTML换行
        let formattedText = summary.replace(/\n/g, '<br>');

        // 处理markdown格式的代码块
        formattedText = formattedText.replace(/```(\w*)\n([\s\S]*?)```/g, (match, language, code) => {
            return `<pre><code class="language-${language || 'text'}">${ChatHandler.#escapeHtml(code)}</code></pre>`;
        });

        // 处理粗体文本
        formattedText = formattedText.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');

        // 处理斜体文本
        formattedText = formattedText.replace(/\*(.*?)\*/g, '<em>$1</em>');

        return formattedText;
    }

    /**
     * 更新步骤显示
     */
    #updateStepsDisplay(planDetails, stepsContainer) {
        if (this.#userInputFormContainer) return; // 如果用户输入表单已显示，则跳过刷新
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
                    } else if (latestThinkAct) { // 当 latestThinkAct 不为 null
                        lastStepActions[index] = {
                            actionDescription: '思考中', // actionDescription 为 "思考中"
                            toolParameters: "等待决策中", // toolParameters 保持 "无工具" 或根据需要调整
                            thinkInput: latestThinkAct.thinkInput || '', // thinkInput 使用 latestThinkAct.thinkInput
                            thinkOutput: latestThinkAct.thinkOutput || '' // thinkOutput 保持不变
                        };
                    } else {
                        lastStepActions[index] = {
                            actionDescription: '执行完成',
                            toolParameters: "无工具",
                            thinkInput: '',
                            thinkOutput: ''
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
                    <span>${ChatHandler.#escapeHtml(step)}</span>
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
                        ${ChatHandler.#escapeHtml(lastAction.actionDescription)}
                    </div>
                    <div class="tool-params">
                        <span class="icon">⚙️</span>
                        参数: ${ChatHandler.#escapeHtml(lastAction.toolParameters)}
                    </div>
                    <div class="think-details" style="margin-top: 8px; border-top: 1px dashed #e8eaed; padding-top: 6px;">
                        <div class="think-output" style="font-size: 12px; color: #5f6368;">
                            <span style="font-weight: bold;">思考输出:</span>
                            <span>${ChatHandler.#escapeHtml(lastAction.thinkOutput || '')}</span>
                        </div>
                    </div>
                `;
                stepDiv.appendChild(actionInfoDiv);
            }

            return stepDiv.outerHTML;
        }).join('');

        stepsContainer.innerHTML = stepsContent;
    }

    /**
     * 创建消息元素 (Static private as it doesn't use 'this' directly, uses another static private)
     */
    static #createMessageElement(className, content) {
        const div = document.createElement('div');
        div.className = `message ${className}`;
        div.innerHTML = `<p>${ChatHandler.#escapeHtml(content)}</p>`;
        return div;
    }

    /**
     * HTML转义 (Static private as it doesn't use 'this')
     */
    static #escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    /**
     * 滚动到底部
     */
    #scrollToBottom() {
        if (this.#chatArea && this.#chatArea.scrollHeight !== undefined) {
            this.#chatArea.scrollTop = this.#chatArea.scrollHeight;
        }
    }

    /**
     * 滚动到指定元素
     * @param {HTMLElement} element
     */
    #scrollToElement(element) {
        if (element && typeof element.scrollIntoView === 'function') {
            element.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        } else {
            this.#scrollToBottom(); // Fallback
        }
    }


    /**
     * 显示用户输入表单 (内部实现，原 displayUserInputForm)
     * @param {Object} userInputState - 后端返回的等待输入状态
     * @param {Object} planDetails - 当前的计划详情，用于定位表单位置
     * @param {HTMLElement} currentChatAreaParam - The chat area to append to as a fallback
     */
    #displayUserInputFormInternal(userInputState, planDetails, currentChatAreaParam) {
        // 1) If a form is already displayed by this instance, do not create a new one. Scroll to it.
        if (this.#userInputFormContainer) {
            console.log('ChatHandler: User input form is already displayed. Skipping new form request.');
            return;
        }

        // No form currently managed by this instance, proceed to create a new one.

        this.#userInputFormContainer = document.createElement('div');
        this.#userInputFormContainer.className = 'user-input-form-container';

        let formHTML = `<p class="user-input-message">${userInputState.message || '请输入所需信息:'}</p>`;
        if (userInputState.formDescription) {
            formHTML += `<p class="form-description">${userInputState.formDescription}</p>`;
        }

        formHTML += '<form id="userInputForm">';
        if (userInputState.formInputs && userInputState.formInputs.length > 0) {
            userInputState.formInputs.forEach(input => {
                const safeId = input.label.replace(/\\W+/g, '_');
                formHTML += `
                    <div class="form-group">
                        <label for="form-input-${safeId}">${input.label}:</label>
                        <input type="text" id="form-input-${safeId}" name="${input.label}" value="${input.value || ''}">
                    </div>
                `;
            });
        } else {
            formHTML += `
                <div class="form-group">
                    <label for="form-input-genericInput">输入:</label>
                    <input type="text" id="form-input-genericInput" name="genericInput">
                </div>
            `;
        }
        formHTML += '<button type="submit" class="submit-user-input-btn">提交</button>';
        formHTML += '</form>';

        this.#userInputFormContainer.innerHTML = formHTML;

        // 定位表单的插入位置
        const dialogRoundContainer = ChatHandler.#findDialogRoundContainerByPlanId(planDetails.planId);

        if (dialogRoundContainer) {
            const stepsContainer = dialogRoundContainer.querySelector('.ai-steps-container');
            if (stepsContainer) {
                const allAiSections = stepsContainer.querySelectorAll('.ai-section');
                if (allAiSections && allAiSections.length > planDetails.currentStepIndex) {
                    const currentStepSection = allAiSections[planDetails.currentStepIndex];
                    if (currentStepSection) {
                        currentStepSection.appendChild(this.#userInputFormContainer);
                    } else {
                        console.log('currentStepSection is Null skip');
                         if (currentChatAreaParam) currentChatAreaParam.appendChild(this.#userInputFormContainer); // Fallback
                    }
                } else {
                    console.warn('无法找到当前步骤的ai-section来放置用户输入表单，将放置在聊天区域底部。');
                    if (currentChatAreaParam) currentChatAreaParam.appendChild(this.#userInputFormContainer); // Fallback
                }
            } else {
                console.warn('无法找到ai-steps-container来放置用户输入表单，将放置在聊天区域底部。');
                if (currentChatAreaParam) currentChatAreaParam.appendChild(this.#userInputFormContainer); // Fallback
            }
        } else {
            console.warn('无法找到dialogRoundContainer来放置用户输入表单，将放置在聊天区域底部。');
            if (currentChatAreaParam) currentChatAreaParam.appendChild(this.#userInputFormContainer); // Fallback
        }

        this.#scrollToElement(this.#userInputFormContainer); // Scroll to the newly created form

        const form = this.#userInputFormContainer.querySelector('#userInputForm');
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const formData = new FormData(form);
            const inputs = {};
            formData.forEach((value, key) => {
                inputs[key] = value;
            });

            try {
                form.querySelector('.submit-user-input-btn').disabled = true;
                form.querySelector('.submit-user-input-btn').textContent = '提交中...';

                await ManusAPI.submitFormInput(this.#planExecutionManager.getActivePlanId(), inputs);
                // 3) Remove user input form when submit button clicked (already implemented)
                this.#removeUserInputForm();

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
    }

    /**
     * 移除用户输入表单 (从 ManusUI 移入)
     */
    #removeUserInputForm() {
        if (this.#userInputFormContainer) {
            this.#userInputFormContainer.remove();
            this.#userInputFormContainer = null;
        }
    }
}
