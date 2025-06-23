/**
 * 右侧边栏 - 执行详情展示模块
 */
class RightSidebarController {
    constructor() {
        // 缓存DOM元素
        this.sidebarElement = null;
        this.sidebarContent = null;
        this.executionStatusElement = null;
        this.executionProgressElement = null;

        // 存储所有计划的数据
        this.planDataMap = new Map();
        // 当前显示的计划ID
        this.currentDisplayedPlanId = null;
    }

    /**
     * 初始化右侧边栏
     */
    init() {
        // 获取DOM元素
        this.sidebarElement = document.getElementById('rightSidebar');
        if (!this.sidebarElement) {
            console.error("RightSidebar: sidebarElement with ID 'rightSidebar' not found.");
            return;
        }
        this.sidebarContent = this.sidebarElement.querySelector('.right-sidebar-content');
        this.executionStatusElement = document.getElementById('execution-status');
        this.executionProgressElement = document.getElementById('execution-progress');

        if (!this.sidebarContent) console.error("RightSidebar: sidebarContent in .right-sidebar-content not found.");
        if (!this.executionStatusElement) console.error("RightSidebar: executionStatusElement with ID 'execution-status' not found.");
        if (!this.executionProgressElement) console.error("RightSidebar: executionProgressElement with ID 'execution-progress' not found.");
        
        // 订阅UI相关事件
        // 假设 TaskPilotUIEvent 已全局可用或已导入
        if (typeof TaskPilotUIEvent !== 'undefined' && TaskPilotUIEvent.EventSystem) {
            TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.PLAN_UPDATE, this.handlePlanUpdate.bind(this));
        } else {
            console.error("RightSidebar: TaskPilotUIEvent.EventSystem is not available.");
        }
        
        // 添加步骤点击事件委托到聊天区域
        const chatArea = document.querySelector('.chat-area');
        if (chatArea) {
            chatArea.addEventListener('click', this.handleChatAreaClick.bind(this));
        } else {
            console.error("RightSidebar: Chat area (.chat-area) not found.");
        }

        this.initializeSidebars(); // Call the new method
        console.log('右侧边栏初始化完成');
    }
    
    /**
     * 初始化侧边栏切换功能
     */
    initializeSidebars() {
        // Ensure sidebarElement is available
        if (!this.sidebarElement) {
            console.error("RightSidebar: sidebarElement is not initialized. Cannot initialize sidebars.");
            return;
        }
        const mainContent = document.getElementById('mainContent');
        const toggleButton = document.getElementById('toggleRightSidebarBtn');

        if (!mainContent) {
            console.error("RightSidebar: mainContent element not found.");
            return;
        }
        if (!toggleButton) {
            console.error("RightSidebar: toggleRightSidebarBtn element not found.");
            return;
        }

        toggleButton.addEventListener('click', () => {
            this.sidebarElement.classList.toggle('collapsed');
            mainContent.classList.toggle('right-expanded');
        });
        console.log('侧边栏切换功能初始化完成 from RightSidebarController');
    }
    
    /**
     * 处理计划更新
     */
    handlePlanUpdate(planData) {
        if (!planData?.planId) return;
        
        this.planDataMap.set(planData.planId, planData);
        
        if (planData.planId === this.currentDisplayedPlanId) {
            this.updateDisplayedPlanProgress(planData);
        }
    }
    
    /**
     * 更新显示的计划进度
     */
    updateDisplayedPlanProgress(planData) {
        if (planData.steps && planData.steps.length > 0) {
            const totalSteps = planData.steps.length;
            const currentStep = planData.currentStepIndex + 1;
            const progressElement = this.executionProgressElement; // 使用实例属性
            if (progressElement) {
                progressElement.innerHTML = `${currentStep} / ${totalSteps} <span class="icon-up-arrow"></span>`;
            }
        }
    }
    
    /**
     * 处理聊天区域点击事件
     */
    handleChatAreaClick(event) {
        const sectionHeader = this._findParentWithClass(event.target, 'section-header');
        if (!sectionHeader) return;
        
        const stepElement = this._findParentWithClass(sectionHeader, 'ai-section');
        if (!stepElement) return;
        
        const dialogRoundContainer = this._findParentWithClass(stepElement, 'dialog-round-container');
        if (!dialogRoundContainer) return;
        
        const planId = dialogRoundContainer.dataset.planId;
        if (!planId) return;
        
        const stepIndex = this._findStepIndex(stepElement);
        if (stepIndex === -1) return;
        
        this.showStepDetails(planId, stepIndex);
    }
    
    /**
     * 查找具有特定类名的父元素 (内部辅助方法)
     */
    _findParentWithClass(element, className) {
        while (element && (!element.classList || !element.classList.contains(className))) {
            element = element.parentElement;
        }
        return element;
    }
    
    /**
     * 查找步骤索引 (内部辅助方法)
     */
    _findStepIndex(stepElement) {
        const parentContainer = stepElement.closest('.ai-steps-container');
        if (!parentContainer) return -1;
        
        const steps = Array.from(parentContainer.querySelectorAll('.ai-section'));
        return steps.indexOf(stepElement);
    }
    
    /**
     * 显示步骤详情
     */
    showStepDetails(planId, stepIndex) {
        const planData = this.planDataMap.get(planId);

        if (!planData || !planData.steps || stepIndex >= planData.steps.length) {
            const contentElement = this.sidebarContent; // 使用实例属性
            if (contentElement) {
                contentElement.innerHTML = '<div class="no-selection-message"><p>无法获取步骤详情</p></div>';
            }
            
            const statusElement = this.executionStatusElement; // 使用实例属性
            if (statusElement) {
                statusElement.textContent = '步骤数据不可用';
            }
            return;
        }

        this.currentDisplayedPlanId = planId;
        const step = planData.steps[stepIndex];
        const agentExecution = planData.agentExecutionSequence && planData.agentExecutionSequence[stepIndex];

        this.updateDisplayedPlanProgress(planData);

        if (!agentExecution) {
            const contentElement = this.sidebarContent; // 使用实例属性
            if (contentElement) {
                contentElement.innerHTML = `
                    <div class="step-info">
                        <h3>${this._escapeHtml(step)}</h3>
                        <div class="status-detail">等待执行...</div>
                    </div>
                `;
            }
            const statusElement = this.executionStatusElement; // 使用实例属性
            if (statusElement) {
                statusElement.textContent = '等待执行';
            }
            return;
        }
        
        const statusElement = this.executionStatusElement; // 使用实例属性
        if (statusElement) {
            statusElement.textContent = agentExecution.status;
        }
        
        let detailsHTML = `
            <div class="step-info">
                <h3>${this._escapeHtml(step)}</h3>
                <div class="agent-info">
                    <span class="label">执行智能体:</span>
                    <span class="value">${this._escapeHtml(agentExecution.agentName)}</span>
                </div>
                <div class="agent-info">
                    <span class="label">描述:</span>
                    <span class="value">${this._escapeHtml(agentExecution.agentDescription || '')}</span>
                </div>
                <div class="agent-info">
                    <span class="label">请求:</span>
                    <span class="value">${this._escapeHtml(agentExecution.agentRequest || '')}</span>
                </div>
                <div class="agent-info">
                    <span class="label">执行结果:</span>
                    <span class="value ${agentExecution.isCompleted ? 'success' : ''}">${this._escapeHtml(agentExecution.result || '执行中...')}</span>
                </div>
            </div>
            <div class="think-act-steps">
                <h4>思考与行动步骤</h4>
        `;
        
        if (agentExecution.thinkActSteps && agentExecution.thinkActSteps.length > 0) {
            detailsHTML += '<div class="steps-container">';
            
            agentExecution.thinkActSteps.forEach((tas, index) => {
                let stepHTML = `
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
                                    <pre>${this._escapeHtml(this._formatJson(tas.thinkInput))}</pre>
                                </div>
                                <div class="output">
                                    <span class="label">输出:</span>
                                    <pre>${this._escapeHtml(this._formatJson(tas.thinkOutput))}</pre>
                                </div>
                            </div>
                        </div>
                `; 
                if (tas.actionNeeded) {
                    stepHTML += `
                        <div class="action-section">
                            <h5>行动</h5>
                            <div class="action-content">
                                <div class="tool-info">
                                    <span class="label">工具:</span>
                                    <span class="value">${this._escapeHtml(tas.toolName || '')}</span>
                                </div>
                                <div class="input">
                                    <span class="label">工具参数:</span>
                                    <pre>${this._escapeHtml(this._formatJson(tas.toolParameters))}</pre>
                                </div>
                                <div class="output">
                                    <span class="label">执行结果:</span>
                                    <pre>${this._escapeHtml(this._formatJson(tas.actionResult))}</pre>
                                </div>
                            </div>
                        </div>
                    `;
                }
                stepHTML += '</div>'; // Closes think-act-step
                detailsHTML += stepHTML;
            });
            detailsHTML += '</div>'; // Closes steps-container
        } else {
            detailsHTML += '<p class="no-steps-message">暂无详细步骤信息</p>';
        }
        detailsHTML += '</div>'; // Closes think-act-steps
        
        const contentElement = this.sidebarContent; // 使用实例属性
        if (contentElement) {
            contentElement.innerHTML = detailsHTML;
        } else {
            console.error('无法更新右侧边栏内容：this.sidebarContent 未定义');
        }
        
        if (this.sidebarElement && this.sidebarElement.classList.contains('collapsed')) {
            const toggleButton = document.getElementById('toggleRightSidebarBtn');
            if (toggleButton) {
                toggleButton.click();
            }
        }
    }

    /**
     * 格式化 JSON 字符串 (内部辅助方法)
     */
    _formatJson(jsonString) {
        if (jsonString === null || typeof jsonString === 'undefined' || jsonString === '') {
            return 'N/A';
        }
        try {
            const jsonObj = typeof jsonString === 'object' ? jsonString : JSON.parse(jsonString);
            return JSON.stringify(jsonObj, null, 2);
        } catch (e) {
            return this._escapeHtml(String(jsonString)); 
        }
    }

    /**
     * 转义 HTML 字符 (内部辅助方法)
     */
    _escapeHtml(text) {
        if (text === null || typeof text === 'undefined') return '';
        const div = document.createElement('div');
        div.textContent = String(text); 
        return div.innerHTML;
    }
}

