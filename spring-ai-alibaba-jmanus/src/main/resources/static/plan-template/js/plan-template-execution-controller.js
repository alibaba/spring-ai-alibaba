class PlanTemplateExecutionController {
    constructor() {
        this.runPlanBtn = null;
        this.isExecutingInternal = false; // Internal state for the button's own execution process
        
        // 缓存的状态数据
        this.cachedGeneratingState = false;
        this.cachedExecutionState = false;
        this.currentPlanTemplateId = null;
        this.currentPlanParams = null;

        this.paramsInput = null; // Element for extra parameters
        this.clearParamBtn = null; // Element for clearing parameters
        this.apiUrlElement = null; // Element displaying API URL
    }

    init() {
        this.runPlanBtn = document.getElementById('runPlanBtn');

        if (!this.runPlanBtn) {
            console.error('[PlanTemplateExecutionController] init: 未找到运行计划按钮!');
            return; 
        }
        console.log('[PlanTemplateExecutionController] init: runPlanBtn 元素已找到:', this.runPlanBtn);

        // Ensure 'this' context is correct for the event handler
        this.runPlanBtn.addEventListener('click', this.handleRunPlanClickInternal.bind(this));
        console.log('[PlanTemplateExecutionController] init: 事件监听器已附加。');
        console.log('[PlanTemplateExecutionController] init: 附加监听器后按钮的 disabled 状态:', this.runPlanBtn.disabled);

        // 绑定UI事件
        this.bindUIEvents();

        // 获取额外参数输入框和API URL元素
        this.paramsInput = document.getElementById('plan-params');
        this.apiUrlElement = document.querySelector('.api-url');

        // 在 init() 方法中，添加监听额外参数输入变化，并立即刷新 API URL
        this.paramsInput.addEventListener('input', (e) => {
            const extraArg = e.target.value.trim();
            this.refreshApiUrl(extraArg);
        });

        // 获取清空参数按钮并添加事件监听器
        this.clearParamBtn = document.getElementById('clearParamBtn');
        if (this.clearParamBtn) {
            this.clearParamBtn.addEventListener('click', this.handleClearParamsClick.bind(this));
            console.log('[PlanTemplateExecutionController] init: clearParamBtn 事件监听器已附加');
        } else {
            console.warn('[PlanTemplateExecutionController] init: 未找到 clearParamBtn 元素');
        }

        this.updateButtonStateInternal(); // Set initial state
        console.log('[PlanTemplateExecutionController] init: 调用 updateButtonStateInternal 后按钮的 disabled 状态:', this.runPlanBtn.disabled);
        console.log('PlanTemplateExecutionController 初始化完成');
    }

    /**
     * 绑定UI事件监听器
     */
    bindUIEvents() {
        // 监听计划模板选择事件
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.PLAN_TEMPLATE_SELECTED, (data) => {
            console.log('[PlanTemplateExecutionController] Received PLAN_TEMPLATE_SELECTED event:', data);
            this.currentPlanTemplateId = data.templateId;
            this.updateButtonStateInternal();
            // 刷新 API URL 显示
            const extraArg = this.paramsInput ? this.paramsInput.value.trim() : null;
            this.refreshApiUrl(extraArg);
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
            this.currentPlanTemplateId = data.templateId ; // 兼容处理
            this.updateButtonStateInternal();
            // 刷新 API URL 显示
            const extraArg = this.paramsInput ? this.paramsInput.value.trim() : null;
            this.refreshApiUrl(extraArg);
        });

        // 监听计划参数变化
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.PLAN_PARAMS_CHANGED, (data) => {
            this.currentPlanParams = data.params;
        });


        console.log('[PlanTemplateExecutionController] Subscribed to UI events.');
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
            // 从输入框读取额外参数
            const extraArg = this.paramsInput ? this.paramsInput.value.trim() : null;
            const planParamsValue = extraArg ? extraArg : null;

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
    
    // 新增方法：刷新 API URL 显示
    refreshApiUrl(extraArg) {
        if (!this.apiUrlElement) return;
        // 如果未选择模板，则不更新URL，避免显示 null
        if (!this.currentPlanTemplateId) return;
        // 获取当前基础 URL（去掉任何旧参数）
        const baseUrl = this.apiUrlElement.textContent.split('?')[0].replace(/execute\/.*$/, `execute/${this.currentPlanTemplateId}`);
        const newUrl = extraArg ? `${baseUrl}?${encodeURIComponent(extraArg)}` : baseUrl;
        this.apiUrlElement.textContent = newUrl;
    }

    updateButtonStateInternal() {
        if (this.runPlanBtn) {
            console.log('[PlanTemplateExecutionController] updateButtonStateInternal: isExecutingInternal:', this.isExecutingInternal);
            console.log('[PlanTemplateExecutionController] updateButtonStateInternal: cachedGeneratingState:', this.cachedGeneratingState);
            console.log('[PlanTemplateExecutionController] updateButtonStateInternal: cachedExecutionState:', this.cachedExecutionState);
            console.log('[PlanTemplateExecutionController] updateButtonStateInternal: currentPlanTemplateId:', this.currentPlanTemplateId);

            // Button is disabled if its own operation is running, or main is generating,
            // or the global execution (polling) is active, or no template is selected.
            const isDisabled = this.isExecutingInternal || this.cachedGeneratingState || this.cachedExecutionState || !this.currentPlanTemplateId;
            this.runPlanBtn.disabled = isDisabled;
            console.log('[PlanTemplateExecutionController] updateButtonStateInternal: calculated isDisabled:', isDisabled, 'Button disabled state set to:', this.runPlanBtn.disabled);

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

    /**
     * 处理清空参数按钮点击事件
     */
    handleClearParamsClick() {
        if (this.paramsInput) {
            this.paramsInput.value = '';
            console.log('[PlanTemplateExecutionController] 参数输入框已清空');
            // 清空参数后刷新 API URL
            this.refreshApiUrl('');
        } else {
            console.warn('[PlanTemplateExecutionController] paramsInput 元素未找到，无法清空');
        }
    }
}
