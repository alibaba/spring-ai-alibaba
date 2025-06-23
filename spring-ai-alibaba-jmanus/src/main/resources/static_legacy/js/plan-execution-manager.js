/**
 * 计划执行管理控制器
 * 负责与后端API通信并管理计划执行状态
 */
// 引入DirectApiService
const DirectApiService = {
    BASE_URL: '/api/executor',
    
    async sendMessage(query) {
        try {
            const response = await fetch(`${this.BASE_URL}/execute`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ query })
            });
            
            if (!response.ok) {
                throw new Error(`API请求失败: ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error("[DirectApiService] sendMessage error:", error);
            throw error;
        }
    },
    
    async getDetails(planId) {
        try {
            const response = await fetch(`${this.BASE_URL}/details/${planId}`);
            
            if (!response.ok) {
                throw new Error(`获取计划详情失败: ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error("[DirectApiService] getDetails error:", error);
            throw error;
        }
    }
};

class PlanExecutionManagerController {
    constructor() {
        this.activePlanId = null;
        this.lastSequenceSize = 0;
        this.POLL_INTERVAL = 5000;
        this.pollTimer = null;
        this.isPolling = false;
    }

    getActivePlanId() {
        return this.activePlanId;
    }

    /**
     * 初始化UI组件
     */
    init() {
        this.initializeEventListeners();
        console.log('Manus UI 初始化完成');
    }

    /**
     * 初始化事件监听器
     */
    initializeEventListeners() {
        // 计划相关事件
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.PLAN_UPDATE, (details) => {
            if (!details) return;

            // 发出UI更新事件
            if (details.title) {
                TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.MESSAGE_UPDATE, {
                    content: `正在执行: ${details.title}`,
                    type: 'status',
                    planId: this.activePlanId
                });
            }

            if (details.steps && details.currentStepIndex !== null) {
                const currentStep = details.steps[details.currentStepIndex];
                if (currentStep) {
                    TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.MESSAGE_UPDATE, {
                        content: `[${details.currentStepIndex + 1}/${details.steps.length}] ${currentStep}`,
                        type: 'step',
                        planId: this.activePlanId
                    });
                }
            }
        });

        // 智能体执行事件
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.AGENT_EXECUTION, (record) => {
            if (!record) return;

            // 发出添加section事件
            TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.SECTION_ADD, {
                agentName: record.agentName,
                agentDescription: record.agentDescription,
                request: record.agentRequest,
                result: record.result,
                planId: this.activePlanId
            });

            if (record.isCompleted) {
                TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.MESSAGE_COMPLETE, { planId: this.activePlanId });
            }
        });

        // 计划完成事件
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.PLAN_COMPLETED, (details) => {
            if (!details) return;
            TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.MESSAGE_UPDATE, {
                content: details.summary ? `执行完成: ${details.summary}` : '执行完成',
                type: 'completion',
                planId: this.activePlanId
            });
            TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.MESSAGE_COMPLETE, { planId: this.activePlanId });
            this.stopPolling();

            // 清空活动计划ID
            this.activePlanId = null;

            // 更新UI状态，启用发送按钮
            TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.CHAT_INPUT_UPDATE_STATE, { enabled: true }); // CORRECTED
        });

        // 新增：监听用户请求发送消息的事件
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.USER_MESSAGE_SEND_REQUESTED, this.handleUserMessageSendRequested.bind(this));

        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.PLAN_EXECUTION_REQUESTED, (data) => {
            console.log('[PlanExecutionManager] Received PLAN_EXECUTION_REQUESTED event:', data);
            if (data && data.planId) {
                this.activePlanId = data.planId; // Set activePlanId from the event data
                // Prepare UI for execution (e.g., disable chat input if not already)
                this.initiatePlanExecutionSequence(data.query || `Executing plan: ${data.planId}`, data.planId);
            } else {
                console.error('[PlanExecutionManager] PLAN_EXECUTION_REQUESTED event missing planId:', data);
            }
        });
    }

    /**
     * Validates the request and prepares the UI for a new message.
     * @param {string} query - The user's query.
     * @returns {boolean} - True if validation passes and UI is prepared, false otherwise.
     */
    _validateAndPrepareUIForNewRequest(query) {
        if (!query) {
            console.warn("[PlanExecutionManager] _validateAndPrepareUIForNewRequest: Query is empty.");
            return false;
        }

        if (this.activePlanId) {
            TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.MESSAGE_UPDATE, {
                content: `当前有任务正在执行，请等待完成后再提交新任务`,
                type: 'error',
                planId: this.activePlanId
            });
            return false;
        }

        TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.CHAT_INPUT_CLEAR);

        TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.CHAT_INPUT_UPDATE_STATE, { enabled: false, placeholder: '处理中...' });
        return true;
    }

    /**
     * Sends the user's message to the API and sets the active plan ID.
     * @param {string} query - The user's query.
     * @returns {Promise<object|null>} - The API response containing planId, or null if an error occurs or planId is missing.
     */
    async _sendUserMessageAndSetPlanId(query) {
        // 使用DirectApiService而不是ManusAPI
        const response = await DirectApiService.sendMessage(query);
        if (response && response.planId) {
            this.activePlanId = response.planId;
            return response; // Return the full response as it might be useful
        } else if (response && response.planTemplateId) {
            // 如果响应中有planTemplateId而不是planId
            this.activePlanId = response.planTemplateId;
            return { ...response, planId: response.planTemplateId };
        }
        console.error("[PlanExecutionManager] _sendUserMessageAndSetPlanId: Failed to get planId from response.", response);
        throw new Error("未能从API响应中获取有效的 planId");
    }

    /**
     * Emits the dialog round start event and begins polling for plan status.
     * Public method to allow initiation from direct plan execution requests.
     * @param {string} query - The user's query or a description of the execution.
     * @param {string} planId - The active plan ID.
     */
    initiatePlanExecutionSequence(query, planId) {
        TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.DIALOG_ROUND_START, {
            planId: planId,
            query: query
        });
        this.startPolling();
    }

    /**
     * 处理用户请求发送消息的事件
     * @param {object} eventData - 包含 query 的事件数据
     */
    async handleUserMessageSendRequested(eventData) {
        const { query } = eventData;

        if (!this._validateAndPrepareUIForNewRequest(query)) {
            return; 
        }

        try {
            await this._sendUserMessageAndSetPlanId(query);
            if (this.activePlanId) {
                this.initiatePlanExecutionSequence(query, this.activePlanId);
            } else {
                console.error("[PlanExecutionManager] handleUserMessageSendRequested: activePlanId is not set after sending message.");
                throw new Error("未能设置有效的 activePlanId。");
            }
        } catch (error) {
            TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.MESSAGE_UPDATE, {
                content: `发送失败: ${error.message}`,
                type: 'error',
                planId: this.activePlanId 
            });
            TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.CHAT_INPUT_UPDATE_STATE, { enabled: true });
            this.activePlanId = null; 
        }
    }

    /**
     * 处理计划完成的通用逻辑
     * @param {object} details - 计划详情
     */
    _handlePlanCompletion(details) {
        TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.PLAN_COMPLETED, { ...details, planId: this.activePlanId });
        this.lastSequenceSize = 0; 
        this.stopPolling();

        try {
            setTimeout(async () => {
                if (!this.activePlanId) return; 
                await fetch(`${DirectApiService.BASE_URL}/details/${this.activePlanId}`, {
                    method: 'DELETE'
                });
                console.log(`已删除已完成的计划执行记录: ${this.activePlanId}`);
                this.activePlanId = null; 
            }, 5000); 
        } catch (error) {
            console.log(`删除计划执行记录失败: ${error.message}`);
        }
        if (details.completed) { 
            this.activePlanId = null;
            TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.CHAT_INPUT_UPDATE_STATE, { enabled: true });
        }
    }

    /**
     * 轮询计划执行状态
     */
    async pollPlanStatus() {
        if (!this.activePlanId) return;
        if (this.isPolling) {
            console.log('上一次轮询尚未完成，跳过本次轮询');
            return;
        }
        try {
            this.isPolling = true;
            const details = await DirectApiService.getDetails(this.activePlanId);
            if (!details) {
                console.log(`无法获取计划 ${this.activePlanId} 的详情`);
                this.isPolling = false;
                return;
            }
            if (!details.steps || details.steps.length === 0) {
                console.log('轮询：未获取到有效详情或步骤为空', details);
                if (details.completed) {
                    console.log(`轮询：Plan ${this.activePlanId} 已完成但无步骤，处理完成逻辑。`);
                    this._handlePlanCompletion(details);
                } else if (this.activePlanId && !details.planId) {
                    console.log(`轮询：Plan ${this.activePlanId} 可能已结束或被删除（无planId返回），停止轮询。`);
                    this.stopPolling(); 
                    this.activePlanId = null; 
                    TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.CHAT_INPUT_UPDATE_STATE, { enabled: true });
                }
                this.isPolling = false; 
                return;
            }

            TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.PLAN_UPDATE, { ...details, planId: this.activePlanId });

            if (details.completed) {
                this._handlePlanCompletion(details); 
            }
        } catch (error) {
            console.error('轮询计划状态失败:', error);
        } finally {
            this.isPolling = false; 
        }
    }

    /**
     * 开始轮询计划执行状态
     */
    startPolling() {
        if (this.pollTimer) { // Added parentheses
            clearInterval(this.pollTimer);
        }
        this.pollTimer = setInterval(this.pollPlanStatus.bind(this), this.POLL_INTERVAL);
    }

    /**
     * 停止轮询
     */
    stopPolling() {
        if (this.pollTimer) {
            clearInterval(this.pollTimer);
            this.pollTimer = null;
        }
    }

    get ActivePlanId() { 
        return this.activePlanId; 
    }
}

