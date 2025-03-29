/**
 * 聊天内容处理模块
 */
const ChatHandler = (() => {
    let chatArea;
    let lastAgentExecutionId = null;
    
    /**
     * 初始化聊天处理器
     */
    const init = () => {
        chatArea = document.querySelector('.chat-area');
        
        // // 订阅UI相关事件
        
        // 订阅业务事件
        ManusUI.EventSystem.on('plan-update', handlePlanUpdate);
        // ManusUI.EventSystem.on('agent-execution', handleAgentExecution);
        ManusUI.EventSystem.on('plan-completed', handlePlanComplete);
    };
    
    
    /**
     * 处理用户输入消息
     */
    const handleUserMessage = (message) => {
        const messageElement = createMessageElement('user-message', message);
        chatArea.appendChild(messageElement);
        scrollToBottom();
    };
    
    /**
     * 处理计划更新
     */
    const handlePlanUpdate = (planDetails) => {
        if (!planDetails.steps || !planDetails.steps.length) return;
        updateStepsDisplay(planDetails);
    };

    /**
     * 更新步骤显示
     */
    const updateStepsDisplay = (planDetails) => {
        if (!planDetails.steps || !planDetails.steps.length) return;
        
        let stepsContainer = document.querySelector('.ai-steps-container');
        if (!stepsContainer) {
            stepsContainer = document.createElement('div');
            stepsContainer.className = 'message ai-message ai-steps-container';
            chatArea.appendChild(stepsContainer);
        }

        // 初始化存储每个步骤的最后执行动作
        if (!window.lastStepActions) {
            window.lastStepActions = new Array(planDetails.steps.length).fill(null);
        }
        
        // 遍历所有执行序列，匹配步骤并更新动作
        if (planDetails.agentExecutionSequence?.length > 0) {
            let index = 0;
            planDetails.agentExecutionSequence.forEach(execution => {
                // 使用stepIndex属性确定此执行记录属于哪个步骤
                if (execution?.thinkActSteps?.length > 0) {
                    const latestThinkAct = execution.thinkActSteps[execution.thinkActSteps.length - 1];
                    if (latestThinkAct?.actionDescription && latestThinkAct?.toolParameters) {
                        // 保存此步骤的最后执行动作
                        window.lastStepActions[index] = {
                            actionDescription: latestThinkAct.actionDescription,
                            toolParameters: latestThinkAct.toolParameters
                        };
                    }else{
                        window.lastStepActions[index] = {
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

            // 获取该步骤的最后执行动作
            const lastAction = window.lastStepActions[index];
            
            // 如果是当前步骤且有执行动作，显示动作信息
            if (index === planDetails.currentStepIndex && planDetails.agentExecutionSequence?.length > 0) {
                const latestExecution = planDetails.agentExecutionSequence.find(e => e.stepIndex === index);
                if (latestExecution?.thinkActSteps?.length > 0) {
                    const latestThinkAct = latestExecution.thinkActSteps[latestExecution.thinkActSteps.length - 1];
                    if (latestThinkAct?.actionDescription && latestThinkAct?.toolParameters) {
                        const actionInfoDiv = document.createElement('div');
                        actionInfoDiv.className = 'action-info';
                        actionInfoDiv.innerHTML = `
                            <div class="action-description">
                                <span class="icon">🔄</span>
                                ${escapeHtml(latestThinkAct.actionDescription)}
                            </div>
                            <div class="tool-params">
                                <span class="icon">⚙️</span>
                                参数: ${escapeHtml(latestThinkAct.toolParameters)}
                            </div>
                        `;
                        stepDiv.appendChild(actionInfoDiv);
                    }
                }
            } 
            // 如果是已完成的步骤且有保存的最后执行动作，显示该动作
            else if (index < planDetails.currentStepIndex && lastAction) {
                const actionInfoDiv = document.createElement('div');
                actionInfoDiv.className = 'action-info';
                actionInfoDiv.innerHTML = `
                    <div class="action-description">
                        <span class="icon">✓</span>
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
        handleUserMessage
    };
})();
