class PlanTemplateListUIHandler {
    constructor() { 
        this.taskListEl = null;
        this.newTaskBtn = null;
        this.planTemplateList = [];
        this.currentPlanTemplateId = null;
        
        this.setupEventListeners();
    }

    init() {
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
                this.handleNewTaskButtonClick();
            });
            console.log('[PlanTemplateListUIHandler] init: 新建任务按钮事件监听器已附加。');
        }
        
        // Request initial state
        this.requestStateUpdate();
        console.log('PlanTemplateListUIHandler 初始化完成');
    }

    setupEventListeners() {
        // 监听状态响应事件
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.STATE_RESPONSE, (data) => {
            if (data.planTemplateList) {
                this.planTemplateList = data.planTemplateList;
                this.updatePlanTemplateListUI();
            }
            if (data.currentPlanTemplateId !== undefined) {
                this.currentPlanTemplateId = data.currentPlanTemplateId;
                this.updatePlanTemplateListUI();
            }
        });

        // 监听当前计划模板变化事件
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.CURRENT_PLAN_TEMPLATE_CHANGED, (data) => {
            this.currentPlanTemplateId = data.templateId;
            this.updatePlanTemplateListUI();
        });
    }

    requestStateUpdate() {
        TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.STATE_REQUEST, {
            requestedFields: ['planTemplateList', 'currentPlanTemplateId']
        });
    }

    handleNewTaskButtonClick() {
        // 发送清空输入事件
        TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.JSON_CONTENT_CLEAR);
        // 发送当前计划模板变化事件（清空选择）
        TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.CURRENT_PLAN_TEMPLATE_CHANGED, { templateId: null });
    }

    updatePlanTemplateListUI() {
        if (!this.taskListEl) {
            console.error('[PlanTemplateListUIHandler] updatePlanTemplateListUI: taskListEl 未初始化');
            return;
        }
        this.taskListEl.innerHTML = ''; // Clear existing list items

        if (this.planTemplateList.length === 0) {
            const emptyItem = document.createElement('li');
            emptyItem.className = 'task-item empty';
            emptyItem.textContent = '没有可用的计划模板';
            this.taskListEl.appendChild(emptyItem);
            return;
        }

        // Sort templates by updateTime or createTime descending
        const sortedTemplates = [...this.planTemplateList].sort((a, b) => {
            const timeA = new Date(a.updateTime || a.createTime);
            const timeB = new Date(b.updateTime || b.createTime);
            return timeB - timeA;
        });

        sortedTemplates.forEach(template => {
            const listItem = document.createElement('li');
            listItem.className = 'task-item';
            if (template.id === this.currentPlanTemplateId) {
                listItem.classList.add('selected');
            }

            const updateTime = new Date(template.updateTime || template.createTime);
            const relativeTime = this.getRelativeTimeString(updateTime);

            listItem.innerHTML = `
                <div class="task-icon">[📄]</div>
                <div class="task-details">
                    <div class="task-title">${template.title || '未命名计划'}</div>
                    <div class="task-preview">${this.truncateText(template.description || '无描述', 40)}</div>
                </div>
                <div class="task-time">${relativeTime}</div>
                <div class="task-actions">
                    <button class="delete-task-btn" title="删除此计划模板">&times;</button>
                </div>
            `;

            const taskDetailsEl = listItem.querySelector('.task-details');
            if (taskDetailsEl) {
                taskDetailsEl.addEventListener('click', () => {
                    this.handlePlanTemplateClick(template);
                });
            }

            const deleteTaskBtn = listItem.querySelector('.delete-task-btn');
            if (deleteTaskBtn) {
                deleteTaskBtn.addEventListener('click', (event) => {
                    event.stopPropagation(); 
                    this.handleDeletePlanTemplate(template);
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
        // 发送计划模板选择事件
        TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.PLAN_TEMPLATE_SELECTED, { templateId: template.id });
        console.log(`[PlanTemplateListUIHandler] Emitted PLAN_TEMPLATE_SELECTED event with templateId: ${template.id}`);
        
        try {
            const versionsResponse = await ManusAPI.getPlanVersions(template.id);
            const planVersionsList = versionsResponse.versions || [];

            if (planVersionsList.length > 0) {
                const latestPlanJson = planVersionsList[planVersionsList.length - 1]; 
                
                // 首先设置版本历史
                TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.VERSION_HISTORY_SET, { 
                    versions: planVersionsList 
                });
                
                // 然后设置JSON内容（这会自动保存到版本历史的最后一个位置）
                TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.JSON_CONTENT_SET, { 
                    content: latestPlanJson 
                });

                try {
                    const parsedPlan = JSON.parse(latestPlanJson);
                    
                    // 通过事件发送计划参数变化
                    TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.PLAN_PARAMS_CHANGED, {
                        prompt: parsedPlan.prompt || '',
                        params: parsedPlan.params || ''
                    });
                } catch (parseError) { 
                    console.warn('解析计划JSON时出错:', parseError);
                    // 发送空的计划参数
                    TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.PLAN_PARAMS_CHANGED, {
                        prompt: '',
                        params: ''
                    });
                }
            } else {
                // 清空JSON内容和参数
                TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.JSON_CONTENT_CLEAR);
                TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.PLAN_PARAMS_CHANGED, {
                    prompt: '',
                    params: ''
                });
            }

            // 更新本地状态
            this.currentPlanTemplateId = template.id;
            this.updatePlanTemplateListUI();
        } catch (error) {
            console.error('加载计划模板详情失败:', error);
            alert('加载计划模板详情失败: ' + error.message);
            
            // 清空相关内容
            TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.JSON_CONTENT_CLEAR);
            TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.PLAN_PARAMS_CHANGED, {
                prompt: '',
                params: ''
            });
            this.updatePlanTemplateListUI();
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
                
                // 更新本地列表
                this.planTemplateList = this.planTemplateList.filter(t => t.id !== template.id);
                
                if (this.currentPlanTemplateId === template.id) {
                    // 如果删除的是当前选中的模板，清空选择和相关内容
                    this.currentPlanTemplateId = null;
                    TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.CURRENT_PLAN_TEMPLATE_CHANGED, { templateId: null });
                    TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.JSON_CONTENT_CLEAR);
                    TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.PLAN_PARAMS_CHANGED, {
                        prompt: '',
                        params: ''
                    });
                }
                
                // 刷新列表UI
                this.updatePlanTemplateListUI(); 
                alert('计划模板已删除。');

            } catch (error) {
                console.error('删除计划模板失败:', error);
                alert('删除计划模板失败: ' + error.message);
                // 即使出错也刷新列表以确保一致性
                this.updatePlanTemplateListUI();
            }
        }
    }

    // 工具函数
    getRelativeTimeString(date) {
        const now = new Date();
        const diffMs = now - date;
        const diffMinutes = Math.floor(diffMs / 60000);
        const diffHours = Math.floor(diffMs / 3600000);
        const diffDays = Math.floor(diffMs / 86400000);

        if (diffMinutes < 1) return '刚刚';
        if (diffMinutes < 60) return `${diffMinutes}分钟前`;
        if (diffHours < 24) return `${diffHours}小时前`;
        if (diffDays < 30) return `${diffDays}天前`;
        
        return date.toLocaleDateString('zh-CN');
    }

    truncateText(text, maxLength) {
        if (!text || text.length <= maxLength) return text;
        return text.substring(0, maxLength) + '...';
    }
}
