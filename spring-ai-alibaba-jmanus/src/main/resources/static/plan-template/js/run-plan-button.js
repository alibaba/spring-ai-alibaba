class RunPlanButtonHandler {
    constructor() {
        this.runPlanBtn = null;
        this.isExecutingInternal = false; // Internal state for the button's own execution process
        this.planTemplateManagerOldInstance = null; // Will hold the dependency object
    }

    init(planTemplateManagerOldInstance) {
        this.planTemplateManagerOldInstance = planTemplateManagerOldInstance;

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

        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.PLAN_TEMPLATE_SELECTED, (data) => {
                console.log('[RunPlanButtonHandler] Received PLAN_TEMPLATE_SELECTED event:', data);
                this.updateButtonStateInternal();
            });
            console.log('[RunPlanButtonHandler] Subscribed to PLAN_TEMPLATE_SELECTED event.');

        this.updateButtonStateInternal(); // Set initial state
        console.log('[RunPlanButtonHandler] init: 调用 updateButtonStateInternal 后按钮的 disabled 状态:', this.runPlanBtn.disabled);
        console.log('RunPlanButtonHandler 初始化完成');
    }

    handleRunPlanClickInternal() {
        if (this.isExecutingInternal) {
            return;
        }

        const planTemplateId = this.planTemplateManagerOldInstance.getCurrentPlanTemplateId();
        if (!planTemplateId) {
            alert('没有可执行的计划模板');
            return;
        }

        try {
            this.executePlanInternal(planTemplateId);
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

        try {
            let response;
            const planParamsValue = this.planTemplateManagerOldInstance.getPlanParams ? this.planTemplateManagerOldInstance.getPlanParams() : null;

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
            // Assuming setMainIsExecuting is a method on planTemplateManagerOldInstance
            this.planTemplateManagerOldInstance.setMainIsExecuting(false);
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
        if (this.runPlanBtn && this.planTemplateManagerOldInstance) {
            const mainIsGenerating = this.planTemplateManagerOldInstance.getIsGenerating();
            const mainIsExecutingGlobal = this.planTemplateManagerOldInstance.getMainIsExecuting(); // Reflects the broader execution state
            const currentPlanTplId = this.planTemplateManagerOldInstance.getCurrentPlanTemplateId();

            console.log('[RunPlanButtonHandler] updateButtonStateInternal: isExecutingInternal:', this.isExecutingInternal);
            console.log('[RunPlanButtonHandler] updateButtonStateInternal: mainIsGenerating from planTemplateManagerOldInstance:', mainIsGenerating);
            console.log('[RunPlanButtonHandler] updateButtonStateInternal: mainIsExecutingGlobal from planTemplateManagerOldInstance:', mainIsExecutingGlobal);
            console.log('[RunPlanButtonHandler] updateButtonStateInternal: currentPlanTplId from planTemplateManagerOldInstance:', currentPlanTplId);

            // Button is disabled if its own operation is running, or main is generating,
            // or the global execution (polling) is active, or no template is selected.
            const isDisabled = this.isExecutingInternal || mainIsGenerating || mainIsExecutingGlobal || !currentPlanTplId;
            this.runPlanBtn.disabled = isDisabled;
            console.log('[RunPlanButtonHandler] updateButtonStateInternal: calculated isDisabled:', isDisabled, 'Button disabled state set to:', this.runPlanBtn.disabled);

            if (this.isExecutingInternal || mainIsExecutingGlobal) {
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
