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
        
        // 订阅事件
        ManusUI.EventSystem.on('plan-update', handlePlanUpdate);
        ManusUI.EventSystem.on('agent-execution', handleAgentExecution);
        ManusUI.EventSystem.on('plan-completed', handlePlanComplete);
    };
    
    /**
     * 处理用户输入消息
     * @param {string} message 用户输入的消息
     */
    const handleUserMessage = (message) => {
        const messageElement = createMessageElement('user-message', message);
        chatArea.appendChild(messageElement);
        scrollToBottom();
    };
    
    /**
     * 处理计划更新
     * @param {Object} planDetails 计划详情数据
     */
    const handlePlanUpdate = (planDetails) => {
        if (!planDetails.steps || !planDetails.steps.length) return;
        
        // 更新步骤展示
        updateStepsDisplay(planDetails.steps, planDetails.currentStepIndex);
    };
    
    /**
     * 处理智能体执行
     * @param {Object} agentExecution 智能体执行记录
     */
    const handleAgentExecution = (agentExecution) => {
        // 避免重复显示相同的执行记录
        if (lastAgentExecutionId === agentExecution.id) return;
        lastAgentExecutionId = agentExecution.id;
        
        const messageElement = createAgentExecutionElement(agentExecution);
        chatArea.appendChild(messageElement);
    };
    
    /**
     * 处理计划完成
     * @param {Object} planDetails 计划完成的详情
     */
    const handlePlanComplete = (planDetails) => {
        if (planDetails.summary) {
        }
    };
    
    /**
     * 创建消息元素
     * @param {string} className 消息类名
     * @param {string} content 消息内容
     */
    const createMessageElement = (className, content) => {
        const div = document.createElement('div');
        div.className = `message ${className}`;
        div.innerHTML = `<p>${escapeHtml(content)}</p>`;
        return div;
    };
    
    /**
     * 创建智能体执行展示元素
     * @param {Object} execution 执行记录
     */
    const createAgentExecutionElement = (execution) => {
        const div = document.createElement('div');
        div.className = 'message ai-message';
        
        let content = `
            <div class="ai-section">
                <div class="section-header ${execution.isCompleted ? 'checked' : ''}">
                    <span class="icon">${execution.isCompleted ? '✓' : '▶'}</span>
                    <span>${escapeHtml(execution.agentName)} - ${escapeHtml(execution.agentDescription || '')}</span>
                </div>
                <div class="section-content">
                    <div class="status-update">
                        <span class="icon">🔄</span>
                        执行请求: ${escapeHtml(execution.agentRequest)}
                    </div>
                    ${execution.result ? `
                        <div class="action-report">
                            <span class="icon">✓</span>
                            执行结果: ${escapeHtml(execution.result)}
                        </div>
                    ` : ''}
                </div>
            </div>
        `;
        
        div.innerHTML = content;
        return div;
    };
    
    /**
     * 创建总结元素
     * @param {string} summary 总结内容
     */
    const createSummaryElement = (summary) => {
        const div = document.createElement('div');
        div.className = 'message ai-message';
        div.innerHTML = `
            <div class="ai-section">
                <div class="section-header checked">
                    <span class="icon">✓</span>
                    <span>执行完成</span>
                </div>
                <div class="section-content">
                    ${escapeHtml(summary)}
                </div>
            </div>
        `;
        return div;
    };
    
    /**
     * 更新步骤显示
     * @param {Array} steps 步骤列表
     * @param {number} currentIndex 当前步骤索引
     */
    const updateStepsDisplay = (steps, currentIndex) => {
        // 查找或创建步骤容器
        let stepsContainer = document.querySelector('.ai-steps-container');
        if (!stepsContainer) {
            stepsContainer = document.createElement('div');
            stepsContainer.className = 'message ai-message ai-steps-container';
            chatArea.appendChild(stepsContainer);
        }
        
        const stepsContent = steps.map((step, index) => `
            <div class="ai-section ${index === currentIndex ? 'current' : ''}">
                <div class="section-header">
                    <span class="icon">${index < currentIndex ? '✓' : index === currentIndex ? '▶' : '○'}</span>
                    <span>${escapeHtml(step)}</span>
                </div>
            </div>
        `).join('');
        
        stepsContainer.innerHTML = stepsContent;
    };
    
    /**
     * HTML转义
     * @param {string} text 需要转义的文本
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
    
    return {
        init,
        handleUserMessage
    };
})();
