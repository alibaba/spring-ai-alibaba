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
    
    // 记录上一次sequence的大小
    let lastSequenceSize = 0;
    
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
    
    // UI更新相关的事件类型
    const UI_EVENTS = {
        MESSAGE_UPDATE: 'ui:message:update',
        MESSAGE_COMPLETE: 'ui:message:complete',
        SECTION_ADD: 'ui:section:add'
    };
    
    /**
     * 初始化事件监听器
     */
    const initializeEventListeners = () => {
        // 计划相关事件
        EventSystem.on('plan-update', (details) => {
            if (!details) return;
            
            // 发出UI更新事件
            if (details.title) {
                EventSystem.emit(UI_EVENTS.MESSAGE_UPDATE, {
                    content: `正在执行: ${details.title}`,
                    type: 'status'
                });
            }
            
            if (details.steps && details.currentStepIndex !== null) {
                const currentStep = details.steps[details.currentStepIndex];
                if (currentStep) {
                    EventSystem.emit(UI_EVENTS.MESSAGE_UPDATE, {
                        content: `[${details.currentStepIndex + 1}/${details.steps.length}] ${currentStep}`,
                        type: 'step'
                    });
                }
            }
        });

        // 智能体执行事件
        EventSystem.on('agent-execution', (record) => {
            if (!record) return;
            
            // 发出添加section事件
            EventSystem.emit(UI_EVENTS.SECTION_ADD, {
                agentName: record.agentName,
                agentDescription: record.agentDescription,
                request: record.agentRequest,
                result: record.result
            });

            if (record.isCompleted) {
                EventSystem.emit(UI_EVENTS.MESSAGE_COMPLETE);
            }
        });

        // 计划完成事件
        EventSystem.on('plan-completed', (details) => {
            if (!details) return;
            EventSystem.emit(UI_EVENTS.MESSAGE_UPDATE, {
                content: details.summary ? `执行完成: ${details.summary}` : '执行完成',
                type: 'completion'
            });
            EventSystem.emit(UI_EVENTS.MESSAGE_COMPLETE);
            stopPolling();
        });

        // 注册UI更新监听器
        EventSystem.on(UI_EVENTS.MESSAGE_UPDATE, ({content, type}) => {
            if (!content) return;
            
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
            paragraph.dataset.type = type;
        });

        // 注册section添加监听器
        EventSystem.on(UI_EVENTS.SECTION_ADD, ({agentName, agentDescription, request, result}) => {
            const aiMessages = document.querySelectorAll('.ai-message');
            if (aiMessages.length === 0) return;
            
            const latestMessage = aiMessages[aiMessages.length - 1];
            
            const section = document.createElement('div');
            section.className = 'ai-section';
            section.innerHTML = `
                <div class="section-header">
                    <span class="icon">▶</span>
                    <span>${agentName} - ${agentDescription}</span>
                </div>
                <div class="section-content">
                    <div class="status-update">
                        <span class="icon">🔄</span>
                        执行请求: ${request}
                    </div>
                    ${result ? `<div class="result">${result}</div>` : ''}
                </div>
            `;
            latestMessage.appendChild(section);
        });

        // 注册消息完成监听器
        EventSystem.on(UI_EVENTS.MESSAGE_COMPLETE, () => {
            const aiMessages = document.querySelectorAll('.ai-message');
            if (aiMessages.length === 0) return;
            
            const latestMessage = aiMessages[aiMessages.length - 1];
            latestMessage.classList.add('completed');
        });
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
     * 轮询计划执行状态
     */
    const pollPlanStatus = async () => {
        if (!activePlanId) return;
        
        try {
            const details = await ManusAPI.getDetails(activePlanId);
            
            // 发送计划更新事件
            EventSystem.emit('plan-update', details);
            
            // 如果有新的智能体执行记录，且sequence size增加了，才发送对应事件
            if (details.agentExecutionSequence) {
                const currentSize = details.agentExecutionSequence.length;
                if (currentSize > lastSequenceSize) {
                    // 只处理新增的记录
                    const newRecords = details.agentExecutionSequence.slice(lastSequenceSize);
                    newRecords.forEach(record => {
                        EventSystem.emit('agent-execution', record);
                    });
                    lastSequenceSize = currentSize;
                }
            }
            
            // 如果计划已完成，发送完成事件，重置sequence size并停止轮询
            if (details.completed) {
                EventSystem.emit('plan-completed', details);
                lastSequenceSize = 0; // 只在计划完成时重置
                stopPolling();
            }
            
        } catch (error) {
            console.error('轮询计划状态失败:', error);
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

    // 返回公开的方法和事件系统
    return {
        init,
        handleSendMessage,
        EventSystem
    };
})();
