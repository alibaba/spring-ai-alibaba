class PlanTemplateListUIHandler {
    constructor(planTemplateManager) { 
       this.planTemplateManager = planTemplateManager; // Will hold the PlanTemplateManagerOld instance
        this.taskListEl = null;
        this.newTaskBtn = null;
    }

    init() { // Removed planTemplateManager from params, as it's set in constructor
        // this.planTemplateManager = planTemplateManager; // No longer needed here

        this.taskListEl = document.querySelector('.task-list');
        this.newTaskBtn = document.querySelector('.new-task-btn');

        if (!this.taskListEl) {
            console.error('[PlanTemplateListUIHandler] init: 未找到任务列表元素 .task-list');
            return;
        }
        if (!this.newTaskBtn) {
            console.warn('[PlanTemplateListUIHandler] init: 未找到新建任务按钮 .new-task-btn');
        }

        console.log('[PlanTemplateListUIHandler] init: DOM 元素已找到。');

        if (this.newTaskBtn) {
            this.newTaskBtn.addEventListener('click', () => {
                this.planTemplateManager.handleClearInput();
                this.updatePlanTemplateListUI(); // Explicitly update list after clearing
            });
            console.log('[PlanTemplateListUIHandler] init: 新建任务按钮事件监听器已附加。');
        }
        
        this.updatePlanTemplateListUI(); // Initial population of the list
        console.log('PlanTemplateListUIHandler 初始化完成');
    }

    updatePlanTemplateListUI() {
        if (!this.taskListEl || !this.planTemplateManager) {
            console.error('[PlanTemplateListUIHandler] updatePlanTemplateListUI: 依赖项未初始化 (taskListEl or planTemplateManager)');
            return;
        }
        this.taskListEl.innerHTML = ''; // Clear existing list items

        const planTemplateList = this.planTemplateManager.planTemplateList;
        const currentPlanTemplateId = this.planTemplateManager.currentPlanTemplateId;

        if (planTemplateList.length === 0) {
            const emptyItem = document.createElement('li');
            emptyItem.className = 'task-item empty';
            emptyItem.textContent = '没有可用的计划模板';
            this.taskListEl.appendChild(emptyItem);
            return;
        }

        // Sort templates by updateTime or createTime descending
        const sortedTemplates = [...planTemplateList].sort((a, b) => {
            const timeA = new Date(a.updateTime || a.createTime);
            const timeB = new Date(b.updateTime || b.createTime);
            return timeB - timeA;
        });

        sortedTemplates.forEach(template => {
            const listItem = document.createElement('li');
            listItem.className = 'task-item';
            if (template.id === currentPlanTemplateId) {
                listItem.classList.add('selected');
            }

            const updateTime = new Date(template.updateTime || template.createTime);
            const relativeTime = this.planTemplateManager.constructor.getRelativeTimeString(updateTime);

            listItem.innerHTML = `
                <div class="task-icon">[📄]</div>
                <div class="task-details">
                    <div class="task-title">${template.title || '未命名计划'}</div>
                    <div class="task-preview">${this.planTemplateManager.constructor.truncateText(template.description || '无描述', 40)}</div>
                </div>
                <div class="task-time">${relativeTime}</div>
                <div class="task-actions">
                    <button class="delete-task-btn" title="删除此计划模板">&times;</button>
                </div>
            `;

            const taskDetailsEl = listItem.querySelector('.task-details');
            if (taskDetailsEl) {
                taskDetailsEl.addEventListener('click', () => {
                    this.handlePlanTemplateClick(template); // Call local method
                });
            }

            const deleteTaskBtn = listItem.querySelector('.delete-task-btn');
            if (deleteTaskBtn) {
                deleteTaskBtn.addEventListener('click', (event) => {
                    event.stopPropagation(); 
                    this.handleDeletePlanTemplate(template); // Call local method
                });
            }
            this.taskListEl.appendChild(listItem);
        });
        
        if (this.newTaskBtn) {
            this.newTaskBtn.innerHTML = '<span class="icon-add"></span> 新建计划 <span class="shortcut">⌘ K</span>';
        }
        console.log('[PlanTemplateListUIHandler] updatePlanTemplateListUI: 列表已更新。');
    }

    async handlePlanTemplateClick(template) {
        this.planTemplateManager.currentPlanTemplateId = template.id;
        this.planTemplateManager.currentPlanId = null; 
        this.planTemplateManager.isExecuting = false; 
        TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.PLAN_TEMPLATE_SELECTED, { templateId: template.id });
        console.log(`[PlanTemplateListUIHandler] Emitted PLAN_TEMPLATE_SELECTED event with templateId: ${template.id}`);
        
        try {
            const versionsResponse = await ManusAPI.getPlanVersions(template.id);
            const planVersionsList = versionsResponse.versions || [];

            if (planVersionsList.length > 0) {
                const latestPlanJson = planVersionsList[planVersionsList.length - 1]; 
                
                this.planTemplateManager.jsonEditor.value = latestPlanJson;
                this.planTemplateManager.saveToVersionHistory(latestPlanJson);

                try {
                    const parsedPlan = JSON.parse(latestPlanJson);
                    this.planTemplateManager.currentPlanData = { 
                        json: latestPlanJson,
                        prompt: parsedPlan.prompt || '', 
                        params: parsedPlan.params || '' 
                    };
                    this.planTemplateManager.planPromptInput.value = this.planTemplateManager.currentPlanData.prompt;
                    if (this.planTemplateManager.planParamsInput) {
                        this.planTemplateManager.planParamsInput.value = this.planTemplateManager.currentPlanData.params;
                    }
                } catch (parseError) { 
                    console.warn('解析计划JSON时出错:', parseError);
                    this.planTemplateManager.currentPlanData = { json: latestPlanJson }; 
                    this.planTemplateManager.planPromptInput.value = '';
                    if (this.planTemplateManager.planParamsInput) {
                        this.planTemplateManager.planParamsInput.value = '';
                    }
                }
            } else {
                this.planTemplateManager.jsonEditor.value = '';
                this.planTemplateManager.planPromptInput.value = '';
                if (this.planTemplateManager.planParamsInput) {
                    this.planTemplateManager.planParamsInput.value = '';
                }
                this.planTemplateManager.currentPlanData = null;
            }

            this.updatePlanTemplateListUI(); // Update the list UI (e.g., to show selection)
            this.planTemplateManager.updateApiUrl();
            this.planTemplateManager.updateUIState();
        } catch (error) {
            console.error('加载计划模板详情失败:', error);
            alert('加载计划模板详情失败: ' + error.message);
            this.planTemplateManager.jsonEditor.value = '';
            this.planTemplateManager.planPromptInput.value = '';
            if (this.planTemplateManager.planParamsInput) {
                this.planTemplateManager.planParamsInput.value = '';
            }
            this.planTemplateManager.currentPlanData = null;
            this.planTemplateManager.updateUIState(); // Ensure UI state is consistent on error
            this.updatePlanTemplateListUI(); // Also refresh list on error
        }
    }

    async handleDeletePlanTemplate(template) {
        if (!template || !template.id) {
            console.warn("[PlanTemplateListUIHandler] handleDeletePlanTemplate: 无效的模板对象或ID");
            return;
        }

        if (confirm(`确定要删除计划模板 "${template.title || '未命名计划'}" 吗？此操作不可恢复。`)) {
            try {
                await ManusAPI.deletePlanTemplate(template.id);
                // Update the list in PlanTemplateManagerOld
                this.planTemplateManager.planTemplateList = this.planTemplateManager.planTemplateList.filter(t => t.id !== template.id);
                
                if (this.planTemplateManager.currentPlanTemplateId === template.id) {
                    // If the deleted template was the current one, clear inputs and reset state
                    this.planTemplateManager.handleClearInput(); 
                }
                // Always refresh the list UI from here
                this.updatePlanTemplateListUI(); 
                alert('计划模板已删除。');

            } catch (error) {
                console.error('删除计划模板失败:', error);
                alert('删除计划模板失败: ' + error.message);
                // Optionally, refresh list even on error to ensure consistency if backend state changed partially
                this.updatePlanTemplateListUI();
            }
        }
    }
}
