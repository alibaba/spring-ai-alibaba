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
     * 处理计划完成
     */
    const handlePlanComplete = (details) => {
        // 首先调用handlePlanUpdate进行最后的执行状态更新
        handlePlanUpdate(details);
        
        // 如果有总结内容，显示为系统反馈消息
        if (details && details.summary) {
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
            
            // 添加到聊天区域
            chatArea.appendChild(aiMessageElement);
            scrollToBottom();
        }
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
    const updateStepsDisplay = (planDetails) => {
        if (!planDetails.steps || !planDetails.steps.length) return;
        
        let stepsContainer = document.querySelector('.ai-steps-container');
        if (!stepsContainer) {
            stepsContainer = document.createElement('div');
            stepsContainer.className = 'message ai-message ai-steps-container';
            chatArea.appendChild(stepsContainer);
        }

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
        handleUserMessage
    };
})();
