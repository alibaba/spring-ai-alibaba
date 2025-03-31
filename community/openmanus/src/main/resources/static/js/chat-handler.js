/**
 * 聊天内容处理模块
 */
const ChatHandler = (() => {
    let chatArea;
    let lastAgentExecutionId = null;
    let currentDialogRoundId = null;
    let dialogRoundPlans = new Map(); // 存储对话轮次和planId的映射关系
    
    /**
     * 初始化聊天处理器
     */
    const init = () => {
        chatArea = document.querySelector('.chat-area');
        
        // 订阅业务事件
        ManusUI.EventSystem.on('plan-update', handlePlanUpdate);
        ManusUI.EventSystem.on('plan-completed', handlePlanComplete);
        ManusUI.EventSystem.on(ManusUI.UI_EVENTS.DIALOG_ROUND_START, handleDialogRoundStart);
    };

    /**
     * 处理用户消息
     */
    const handleUserMessage = (message) => {
        // 交给 UI 模块处理发送消息
        ManusUI.handleSendMessage();
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
                            toolParameters: latestThinkAct.toolParameters
                        };
                    }else{
                        lastStepActions[index] = {
                            actionDescription: latestThinkAct.thinkOutput,
                            toolParameters: "无工具"
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
        chatArea.scrollTop = chatArea.scrollHeight;
    };
    
    // 返回公开方法
    return {
        init,
        handleUserMessage,  // 确保导出 handleUserMessage
        // 其他需要公开的方法...
    };
})();
