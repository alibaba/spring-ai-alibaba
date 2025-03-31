/**
 * 右侧边栏 - 执行详情展示模块
 */
const RightSidebar = (() => {
    // 缓存DOM元素
    let sidebarElement;
    let sidebarContent;
    let executionStatusElement;
    let executionProgressElement;

    // 存储所有计划的数据
    let planDataMap = new Map();
    // 当前显示的计划ID
    let currentDisplayedPlanId = null;
    
    /**
     * 初始化右侧边栏
     */
    const init = () => {
        // 获取DOM元素
        sidebarElement = document.getElementById('rightSidebar');
        sidebarContent = sidebarElement.querySelector('.right-sidebar-content');
        executionStatusElement = document.getElementById('execution-status');
        executionProgressElement = document.getElementById('execution-progress');
        
        // 订阅UI相关事件
        ManusUI.EventSystem.on('plan-update', handlePlanUpdate);
        
        // 添加步骤点击事件委托到聊天区域
        const chatArea = document.querySelector('.chat-area');
        chatArea.addEventListener('click', handleChatAreaClick);

        console.log('右侧边栏初始化完成');
    };
    
    /**
     * 处理计划更新
     */
    const handlePlanUpdate = (planData) => {
        if (!planData?.planId) return;
        
        // 保存/更新计划数据
        planDataMap.set(planData.planId, planData);
        
        // 更新进度显示（如果是当前显示的计划）
        if (planData.planId === currentDisplayedPlanId) {
            updateDisplayedPlanProgress(planData);
        }
    };
    
    /**
     * 更新显示的计划进度
     */
    const updateDisplayedPlanProgress = (planData) => {
        if (planData.steps && planData.steps.length > 0) {
            const totalSteps = planData.steps.length;
            const currentStep = planData.currentStepIndex + 1;
            executionProgressElement.innerHTML = `${currentStep} / ${totalSteps} <span class="icon-up-arrow"></span>`;
        }
    };
    
    /**
     * 处理聊天区域点击事件
     */
    const handleChatAreaClick = (event) => {
        // 查找是否点击了步骤标题
        const sectionHeader = findParentWithClass(event.target, 'section-header');
        if (!sectionHeader) return;
        
        // 找到对应的步骤元素
        const stepElement = findParentWithClass(sectionHeader, 'ai-section');
        if (!stepElement) return;
        
        // 找到对话轮次容器并获取planId
        const dialogRoundContainer = findParentWithClass(stepElement, 'dialog-round-container');
        if (!dialogRoundContainer) return;
        
        const planId = dialogRoundContainer.dataset.planId;
        if (!planId) return;
        
        // 获取步骤索引
        const stepIndex = findStepIndex(stepElement);
        if (stepIndex === -1) return;
        
        // 显示该步骤的详细信息
        showStepDetails(planId, stepIndex);
    };
    
    /**
     * 查找具有特定类名的父元素
     */
    const findParentWithClass = (element, className) => {
        while (element && !element.classList.contains(className)) {
            element = element.parentElement;
        }
        return element;
    };
    
    /**
     * 查找步骤索引
     */
    const findStepIndex = (stepElement) => {
        const parentContainer = stepElement.closest('.ai-steps-container');
        if (!parentContainer) return -1;
        
        const steps = parentContainer.querySelectorAll('.ai-section');
        return Array.from(steps).indexOf(stepElement);
    };
    
    /**
     * 显示步骤详情
     */
    const showStepDetails = (planId, stepIndex) => {
        // 获取计划数据
        const planData = planDataMap.get(planId);
        
        // 如果没有计划数据或步骤不存在，则显示错误信息
        if (!planData || !planData.steps || stepIndex >= planData.steps.length) {
            sidebarContent.innerHTML = '<div class="no-selection-message"><p>无法获取步骤详情</p></div>';
            executionStatusElement.textContent = '步骤数据不可用';
            return;
        }
        
        // 更新当前显示的计划ID
        currentDisplayedPlanId = planId;
        
        // 获取步骤信息
        const step = planData.steps[stepIndex];
        const agentExecution = planData.agentExecutionSequence && planData.agentExecutionSequence[stepIndex];
        
        // 更新状态和进度显示
        updateDisplayedPlanProgress(planData);
        
        // 如果没有执行数据，显示等待信息
        if (!agentExecution) {
            sidebarContent.innerHTML = `
                <div class="step-info">
                    <h3>${escapeHtml(step)}</h3>
                    <div class="status-detail">等待执行...</div>
                </div>
            `;
            executionStatusElement.textContent = '等待执行';
            return;
        }
        
        // 更新状态栏
        executionStatusElement.textContent = agentExecution.status;
        
        // 构建步骤详情HTML
        let detailsHTML = `
            <div class="step-info">
                <h3>${escapeHtml(step)}</h3>
                <div class="agent-info">
                    <span class="label">执行智能体:</span>
                    <span class="value">${escapeHtml(agentExecution.agentName)}</span>
                </div>
                <div class="agent-info">
                    <span class="label">描述:</span>
                    <span class="value">${escapeHtml(agentExecution.agentDescription || '')}</span>
                </div>
                <div class="agent-info">
                    <span class="label">请求:</span>
                    <span class="value">${escapeHtml(agentExecution.agentRequest || '')}</span>
                </div>
                <div class="agent-info">
                    <span class="label">执行结果:</span>
                    <span class="value ${agentExecution.isCompleted ? 'success' : ''}">${escapeHtml(agentExecution.result || '执行中...')}</span>
                </div>
            </div>
            <div class="think-act-steps">
                <h4>思考与行动步骤</h4>
        `;
        
        // 如果有思考和行动步骤，显示每个步骤的详细信息
        if (agentExecution.thinkActSteps && agentExecution.thinkActSteps.length > 0) {
            detailsHTML += '<div class="steps-container">';
            
            agentExecution.thinkActSteps.forEach((tas, index) => {
                detailsHTML += `
                    <div class="think-act-step">
                        <div class="step-header">
                            <span class="step-number">#${index + 1}</span>
                            <span class="step-status ${tas.status}">${tas.status}</span>
                        </div>
                        
                        <div class="think-section">
                            <h5>思考</h5>
                            <div class="think-content">
                                <div class="input">
                                    <span class="label">输入:</span>
                                    <pre>${escapeHtml(tas.thinkInput || '')}</pre>
                                </div>
                                <div class="output">
                                    <span class="label">输出:</span>
                                    <pre>${escapeHtml(tas.thinkOutput || '')}</pre>
                                </div>
                            </div>
                        </div>
                `;
                
                // 如果需要执行操作，显示操作详情
                if (tas.actionNeeded) {
                    detailsHTML += `
                        <div class="action-section">
                            <h5>行动</h5>
                            <div class="action-content">
                                <div class="tool-info">
                                    <span class="label">工具:</span>
                                    <code class="code-inline">${escapeHtml(tas.toolName || 'N/A')}</code>
                                </div>
                                <div class="tool-params">
                                    <span class="label">参数:</span>
                                    <pre class="params">${formatJson(tas.toolParameters)}</pre>
                                </div>
                                <div class="action-desc">
                                    <span class="label">行动描述:</span>
                                    <div>${escapeHtml(tas.actionDescription || '')}</div>
                                </div>
                                <div class="action-result">
                                    <span class="label">执行结果:</span>
                                    <pre>${escapeHtml(tas.actionResult || '执行中...')}</pre>
                                </div>
                            </div>
                        </div>
                    `;
                }
                
                detailsHTML += '</div>'; // 结束 think-act-step
            });
            
            detailsHTML += '</div>'; // 结束 steps-container
        } else {
            detailsHTML += '<p class="no-steps-message">暂无详细步骤信息</p>';
        }
        
        detailsHTML += '</div>'; // 结束 think-act-steps
        
        // 更新右侧边栏内容
        sidebarContent.innerHTML = detailsHTML;
        
        // 如果边栏是收起状态，展开边栏
        if (sidebarElement.classList.contains('collapsed')) {
            document.getElementById('toggleRightSidebarBtn').click();
        }
    };
    
    /**
     * 格式化JSON字符串
     */
    const formatJson = (jsonString) => {
        if (!jsonString) return 'N/A';
        
        try {
            // 尝试解析JSON字符串
            const jsonObj = typeof jsonString === 'object' ? jsonString : JSON.parse(jsonString);
            return JSON.stringify(jsonObj, null, 2);
        } catch (e) {
            // 如果解析失败，直接返回原始字符串
            return escapeHtml(jsonString);
        }
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
    
    // 返回公开方法
    return {
        init
    };
})();
