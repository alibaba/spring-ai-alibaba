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
    // 轮询计时器
    let statusPollingTimer = null;
    
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
        
        console.log('Manus UI 初始化完成');
    };
    
    /**
     * 处理发送消息
     */
    const handleSendMessage = async () => {
        const query = inputField.value.trim();
        if (!query) return;
        
        // 清空输入框
        inputField.value = '';
        
        // 显示用户消息
        appendUserMessage(query);
        
        try {
            // 显示初始AI响应（等待状态）
            appendInitialAIMessage();
            
            // 发送到API
            const response = await ManusAPI.sendMessage(query);
            
            // 更新任务ID
            activePlanId = response.planId;
            
            // 开始轮询状态
            startStatusPolling();
        } catch (error) {
            updateLatestAIMessage(`发送失败: ${error.message}`);
        }
    };
    
    /**
     * 开始轮询任务状态
     */
    const startStatusPolling = () => {
        // 清除任何现有的轮询
        if (statusPollingTimer) {
            clearInterval(statusPollingTimer);
        }
        
        // 设置轮询间隔 (2秒)
        statusPollingTimer = setInterval(pollTaskStatus, 2000);
    };
    
    /**
     * 轮询任务状态
     */
    const pollTaskStatus = async () => {
        if (!activePlanId) return;
        
        try {
            const status = await ManusAPI.getStatus(activePlanId);
            
            // 更新消息状态
            updateExecutionStatus(status);
            
            // 如果任务完成，停止轮询
            if (status.completed || status.status === 'completed' || status.status === 'error') {
                clearInterval(statusPollingTimer);
                statusPollingTimer = null;
                
                // 更新最终结果
                if (status.result) {
                    updateLatestAIMessage(status.result, true);
                }
            }
        } catch (error) {
            console.error('获取状态失败:', error);
        }
    };
    
    /**
     * 添加用户消息到聊天区域
     */
    const appendUserMessage = (message) => {
        const messageDiv = document.createElement('div');
        messageDiv.className = 'message user-message';
        messageDiv.innerHTML = `<p>${escapeHTML(message)}</p>`;
        chatArea.appendChild(messageDiv);
        scrollToBottom();
    };
    
    /**
     * 添加初始AI响应消息
     */
    const appendInitialAIMessage = () => {
        const messageDiv = document.createElement('div');
        messageDiv.className = 'message ai-message';
        messageDiv.innerHTML = `
            <div class="ai-header">
                <span class="ai-logo">[M]</span> Manus
            </div>
            <p>正在思考中...</p>
        `;
        chatArea.appendChild(messageDiv);
        scrollToBottom();
    };
    
    /**
     * 更新最新的AI消息内容
     */
    const updateLatestAIMessage = (content, isComplete = false) => {
        const aiMessages = document.querySelectorAll('.ai-message');
        if (aiMessages.length === 0) return;
        
        const latestMessage = aiMessages[aiMessages.length - 1];
        
        // 获取或创建消息段落
        let paragraph = latestMessage.querySelector('p:not(.ai-header)');
        if (!paragraph) {
            paragraph = document.createElement('p');
            latestMessage.appendChild(paragraph);
        }
        
        paragraph.innerHTML = escapeHTML(content);
        
        if (isComplete) {
            // 添加已完成的标记
            latestMessage.classList.add('completed');
        }
        
        scrollToBottom();
    };
    
    /**
     * 更新执行状态
     */
    const updateExecutionStatus = (status) => {
        const aiMessages = document.querySelectorAll('.ai-message');
        if (aiMessages.length === 0) return;
        
        const latestMessage = aiMessages[aiMessages.length - 1];
        
        // 检查是否已有状态部分，如果没有则创建
        let statusSection = latestMessage.querySelector('.ai-section');
        if (!statusSection) {
            statusSection = document.createElement('div');
            statusSection.className = 'ai-section';
            latestMessage.appendChild(statusSection);
        }
        
        // 更新状态部分内容
        let statusHTML = '';
        
        // 添加标题和进度
        statusHTML += `<div class="section-header">
            <span class="icon">[${status.completed ? '✔' : '⏳'}]</span> 
            ${status.title || '任务执行中'} 
            <span class="progress">(${Math.round(status.progress)}%)</span>
            <span class="toggle-arrow">^</span>
        </div>`;
        
        // 添加步骤信息
        if (status.steps && status.steps.length > 0) {
            statusHTML += '<div class="section-content">';
            
            status.steps.forEach((step, index) => {
                const stepStatus = status.stepStatuses && status.stepStatuses[index] ? status.stepStatuses[index] : 'not_started';
                const statusIcon = getStatusIcon(stepStatus);
                
                statusHTML += `<div class="step ${stepStatus}">
                    <span class="icon">[${statusIcon}]</span> ${escapeHTML(step)}
                </div>`;
                
                // 如果是当前步骤，显示执行中状态
                if (status.currentStepIndex === index) {
                    statusHTML += '<div class="status-update searching">';
                    statusHTML += '<span class="icon">[🔍]</span> 正在执行此步骤...';
                    statusHTML += '</div>';
                }
            });
            
            statusHTML += '</div>';
        }
        
        // 更新状态部分
        statusSection.innerHTML = statusHTML;
        
        scrollToBottom();
    };
    
    /**
     * 获取步骤状态对应的图标
     */
    const getStatusIcon = (status) => {
        switch (status) {
            case 'completed': return '✔';
            case 'in_progress': return '→';
            case 'blocked': return '!';
            default: return ' ';
        }
    };
    
    /**
     * 将聊天区域滚动到底部
     */
    const scrollToBottom = () => {
        chatArea.scrollTop = chatArea.scrollHeight;
    };
    
    /**
     * HTML转义，防止XSS攻击
     */
    const escapeHTML = (str) => {
        if (!str) return '';
        return str
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;')
            .replace(/\n/g, '<br>');
    };
    
    // 返回公开的方法
    return {
        init,
        handleSendMessage
    };
})();
