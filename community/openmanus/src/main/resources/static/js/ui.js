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
    
    // 轮询间隔（毫秒）
    const POLL_INTERVAL = 2000;
    
    // 轮询定时器
    let pollTimer = null;
    
    // 事件监听器集合
    const eventListeners = {
        'plan-update': [],
        'agent-execution': [],
        'plan-completed': []
    };
    
    /**
     * 事件发布订阅系统
     */
    const EventSystem = {
        // 订阅事件
        on: (eventName, callback) => {
            if (!eventListeners[eventName]) {
                eventListeners[eventName] = [];
            }
            eventListeners[eventName].push(callback);
        },
        
        // 发布事件
        emit: (eventName, data) => {
            if (eventListeners[eventName]) {
                eventListeners[eventName].forEach(callback => callback(data));
            }
        },
        
        // 取消订阅
        off: (eventName, callback) => {
            if (eventListeners[eventName]) {
                eventListeners[eventName] = eventListeners[eventName]
                    .filter(listener => listener !== callback);
            }
        }
    };

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
        // 监听计划更新
        EventSystem.on('plan-update', handlePlanUpdate);
        // 监听智能体执行
        EventSystem.on('agent-execution', handleAgentExecution);
        // 监听计划完成
        EventSystem.on('plan-completed', handlePlanCompleted);
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
            
            // 更新任务ID并开始轮询
            activePlanId = response.planId;
            startPolling();
            
        } catch (error) {
            updateLatestAIMessage(`发送失败: ${error.message}`);
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
        
        // 设置定时轮询
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
     * 轮询计划执行状态
     */
    const pollPlanStatus = async () => {
        if (!activePlanId) return;
        
        try {
            const details = await ManusAPI.getDetails(activePlanId);
            
            // 发送计划更新事件
            EventSystem.emit('plan-update', details);
            
            // 如果有新的智能体执行记录，发送对应事件
            if (details.agentExecutionSequence) {
                details.agentExecutionSequence.forEach(record => {
                    EventSystem.emit('agent-execution', record);
                });
            }
            
            // 如果计划已完成，发送完成事件并停止轮询
            if (details.completed) {
                EventSystem.emit('plan-completed', details);
                stopPolling();
            }
            
        } catch (error) {
            console.error('轮询计划状态失败:', error);
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
    
    /**
     * 处理计划更新事件
     */
    const handlePlanUpdate = (details) => {
        if (!details) return;
        
        // 更新执行状态
        updateLatestAIMessage(`正在执行: ${details.title || ''}`, false);
        
        // 显示当前步骤
        if (details.steps && details.currentStepIndex !== null) {
            const currentStep = details.steps[details.currentStepIndex];
            if (currentStep) {
                updateLatestAIMessage(`[${details.currentStepIndex + 1}/${details.steps.length}] ${currentStep}`, false);
            }
        }
    };
    
    /**
     * 处理智能体执行事件
     */
    const handleAgentExecution = (record) => {
        if (!record) return;
        
        let message = `${record.agentName}: ${record.agentRequest}`;
        if (record.result) {
            message += `\n结果: ${record.result}`;
        }
        updateLatestAIMessage(message, record.isCompleted);
    };
    
    /**
     * 处理计划完成事件
     */
    const handlePlanCompleted = (details) => {
        if (!details) return;
        if (details.summary) {
            updateLatestAIMessage(`执行完成: ${details.summary}`, true);
        } else {
            updateLatestAIMessage('执行完成', true);
        }
        stopPolling();
    };

    // 返回公开的方法和事件系统
    return {
        init,
        handleSendMessage,
        EventSystem
    };
})();
