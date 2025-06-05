class RunPlanButtonHandler {
    constructor() {
        this.runPlanBtn = null;
        this.isExecutingInternal = false; // Internal state for the button's own execution process
        
        // 缓存的状态数据
        this.cachedGeneratingState = false;
        this.cachedExecutionState = false;
        this.currentPlanTemplateId = null;
        this.currentPlanParams = null;
    }

    init() {
        this.runPlanBtn = document.getElementById('runPlanBtn');

        if (!this.runPlanBtn) {
            console.error('[RunPlanButtonHandler] init: 未找到运行计划按钮!');
            return; 
        }
        console.log('[RunPlanButtonHandler] init: runPlanBtn 元素已找到:', this.runPlanBtn);

        // Ensure 'this' context is correct for the event handler
        this.runPlanBtn.addEventListener('click', this.handleRunPlanClickInternal.bind(this));
        console.log('[RunPlanButtonHandler] init: 事件监听器已附加。');
        console.log('[RunPlanButtonHandler] init: 附加监听器后按钮的 disabled 状态:', this.runPlanBtn.disabled);

        // 绑定UI事件
        this.bindUIEvents();

        this.updateButtonStateInternal(); // Set initial state
        console.log('[RunPlanButtonHandler] init: 调用 updateButtonStateInternal 后按钮的 disabled 状态:', this.runPlanBtn.disabled);
        console.log('RunPlanButtonHandler 初始化完成');
    }

    /**
     * 绑定UI事件监听器
     */
    bindUIEvents() {
        // 监听计划模板选择事件
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.PLAN_TEMPLATE_SELECTED, (data) => {
            console.log('[RunPlanButtonHandler] Received PLAN_TEMPLATE_SELECTED event:', data);
            this.updateButtonStateInternal();
        });

        // 监听生成状态变化
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.GENERATION_STATE_CHANGED, (data) => {
            this.cachedGeneratingState = data.isGenerating;
            this.updateButtonStateInternal();
        });

        // 监听执行状态变化
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.EXECUTION_STATE_CHANGED, (data) => {
            this.cachedExecutionState = data.isExecuting;
            this.updateButtonStateInternal();
        });

        // 监听当前计划模板变化
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.CURRENT_PLAN_TEMPLATE_CHANGED, (data) => {
            this.currentPlanTemplateId = data.templateId || data.planTemplateId; // 兼容处理
            this.updateButtonStateInternal();
        });

        // 监听计划参数变化
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.PLAN_PARAMS_CHANGED, (data) => {
            this.currentPlanParams = data.params;
        });


        console.log('[RunPlanButtonHandler] Subscribed to UI events.');
    }

    handleRunPlanClickInternal() {
        if (this.isExecutingInternal) {
            return;
        }

        if (!this.currentPlanTemplateId) {
            alert('没有可执行的计划模板');
            return;
        }

        try {
            this.executePlanInternal(this.currentPlanTemplateId);
        } catch (e) {
            console.error('执行计划时出错', e);
            alert('执行计划失败: ' + e.message);
            // Ensure button state is reset on error
            this.isExecutingInternal = false;
            this.updateButtonStateInternal();
        }
    }

    async executePlanInternal(planTemplateId) {
        this.isExecutingInternal = true;
        this.updateButtonStateInternal();

        // 发布执行状态变化事件
        TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.EXECUTION_STATE_CHANGED, {
            isExecuting: true
        });

        try {
            let response;
            const planParamsValue = null;

            if (planParamsValue) {
                response = await ManusAPI.executePlan(planTemplateId, planParamsValue);
            } else {
                response = await ManusAPI.executePlan(planTemplateId);
            }

            const eventQuery = `Executing plan template: ${planTemplateId}`;
            TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.PLAN_EXECUTION_REQUESTED, {
                    planId: response.planId,
                    query: eventQuery // Or a more specific query/description if available
                });
            
            console.log('计划模板执行请求成功:', response);
        } catch (error) {
            console.error('执行计划出错:', error);
            alert('执行计划失败: ' + error.message);
            // Ensure main state is also reset in case of error before finally
            this.isExecutingInternal = false;
            
            // 发布执行状态变化事件
            TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.EXECUTION_STATE_CHANGED, {
                isExecuting: false
            });
            
            this.updateButtonStateInternal(); // Update button state after error
        } finally {
            // The button's own direct action is complete or errored out.
            // Global execution state (polling) is managed elsewhere.
            if (this.isExecutingInternal) { // if not already set to false by an error path
                this.isExecutingInternal = false;
            }
            this.updateButtonStateInternal();
        }
    }
    

    updateButtonStateInternal() {
        if (this.runPlanBtn) {
            console.log('[RunPlanButtonHandler] updateButtonStateInternal: isExecutingInternal:', this.isExecutingInternal);
            console.log('[RunPlanButtonHandler] updateButtonStateInternal: cachedGeneratingState:', this.cachedGeneratingState);
            console.log('[RunPlanButtonHandler] updateButtonStateInternal: cachedExecutionState:', this.cachedExecutionState);
            console.log('[RunPlanButtonHandler] updateButtonStateInternal: currentPlanTemplateId:', this.currentPlanTemplateId);

            // Button is disabled if its own operation is running, or main is generating,
            // or the global execution (polling) is active, or no template is selected.
            const isDisabled = this.isExecutingInternal || this.cachedGeneratingState || this.cachedExecutionState || !this.currentPlanTemplateId;
            this.runPlanBtn.disabled = isDisabled;
            console.log('[RunPlanButtonHandler] updateButtonStateInternal: calculated isDisabled:', isDisabled, 'Button disabled state set to:', this.runPlanBtn.disabled);

            if (this.isExecutingInternal || this.cachedExecutionState) {
                this.runPlanBtn.textContent = '执行中...';
            } else {
                this.runPlanBtn.textContent = '执行计划';
            }
        }
    }

    // Public interface method, consistent with the original IIFE's returned object
    updateButtonState() {
        this.updateButtonStateInternal();
    }
}
