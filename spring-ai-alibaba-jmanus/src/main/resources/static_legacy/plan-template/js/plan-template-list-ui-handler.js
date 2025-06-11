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
        
        // 直接加载计划模板列表，而不是请求状态
        this.loadPlanTemplateList();
        console.log('PlanTemplateListUIHandler 初始化完成');
    }

    setupEventListeners() {

        // 监听当前计划模板变化事件
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.CURRENT_PLAN_TEMPLATE_CHANGED, (data) => {
            this.currentPlanTemplateId = data.templateId;
            this.updatePlanTemplateListUI();
        });

        // 监听生成状态变化事件，当生成成功时自动刷新列表
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.GENERATION_STATE_CHANGED, async (data) => {
            if (!data.isGenerating && data.success) {
                console.log('[PlanTemplateListUIHandler] 检测到计划生成完成，刷新列表...');
                await this.loadPlanTemplateList();
            }
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
                
                // 重新加载列表而不是手动更新
                await this.loadPlanTemplateList();
                alert('计划模板已删除。');

            } catch (error) {
                console.error('删除计划模板失败:', error);
                alert('删除计划模板失败: ' + error.message);
                // 即使出错也刷新列表以确保一致性
                await this.loadPlanTemplateList();
            }
        }
    }

    /**
     * 直接从API加载计划模板列表
     */
    async loadPlanTemplateList() {
        try {
            console.log('[PlanTemplateListUIHandler] 开始加载计划模板列表...');
            const response = await ManusAPI.getAllPlanTemplates();
            
            // 处理 API 返回的数据结构: { count: number, templates: Array }
            if (response && response.templates && Array.isArray(response.templates)) {
                this.planTemplateList = response.templates;
                console.log(`[PlanTemplateListUIHandler] 成功加载 ${response.templates.length} 个计划模板`);
            } else {
                this.planTemplateList = [];
                console.warn('[PlanTemplateListUIHandler] API 返回的数据格式异常，使用空列表', response);
            }
            
            // 更新UI
            this.updatePlanTemplateListUI();
            
            
        } catch (error) {
            console.error('[PlanTemplateListUIHandler] 加载计划模板列表失败:', error);
            this.planTemplateList = [];
            this.updatePlanTemplateListUI();
            
            // 显示错误提示
            if (this.taskListEl) {
                const errorItem = document.createElement('li');
                errorItem.className = 'task-item error';
                errorItem.textContent = '加载计划模板列表失败: ' + error.message;
                this.taskListEl.appendChild(errorItem);
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
