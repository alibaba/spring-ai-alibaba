/**
 * 计划模板页面的主要JavaScript文件
 * 负责处理用户输入、发送API请求、展示计划结果等功能
 */

class PlanTemplateManagerOld {
    constructor() {
        // 全局变量，保存当前计划状态
        this.currentPlanTemplateId = null; // 存储计划模板ID
        this.currentPlanId = null; // 存储计划执行ID
        this.currentPlanData = null;
        this.isExecuting = false;
        this.planTemplateList = []; // 存储计划模板列表

        // DOM 元素引用 (只保留清空按钮)
        this.clearBtn = null;

        // 侧边栏折叠/展开相关变量
        this.toggleLeftSidebarBtn = null;
        this.toggleRightSidebarBtn = null;
        this.leftSidebar = null;
        this.rightSidebar = null;

        // 计划提示生成器实例
        this.planPromptGenerator = null;
        // JSON处理器实例
        this.planTemplateHandler = null;
    }

    // Getter methods
    getIsExecuting() {
        return this.isExecuting;
    }

    getMainIsExecuting() { // Assuming this maps to isExecuting
        return this.isExecuting;
    }

    getCurrentPlanTemplateId() {
        return this.planPromptGenerator ? this.planPromptGenerator.getCurrentPlanTemplateId() : this.currentPlanTemplateId;
    }

    getPlanParams() {
        return this.planPromptGenerator ? this.planPromptGenerator.getPlanParams() : null;
    }

    getJsonEditor() {
        return this.planTemplateHandler ? this.planTemplateHandler.getJsonEditor() : null;
    }

    setMainIsExecuting(value) { // Setter for isExecuting, used by RunPlanButtonHandler
        this.isExecuting = value;
        this.updateUIState(); // Update UI when execution state changes
        
        // 发布执行状态变化事件
        TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.EXECUTION_STATE_CHANGED, {
            isExecuting: value
        });
        
        if (this.planPromptGenerator) {
            this.planPromptGenerator.updateUIState();
        }
        if (this.planTemplateHandler) {
            this.planTemplateHandler.updateUIState();
        }
    }

    setCurrentPlanTemplateId(id) {
        this.currentPlanTemplateId = id;
        if (this.planPromptGenerator) {
            this.planPromptGenerator.setCurrentPlanTemplateId(id);
        }
        
        // 发布当前计划模板变化事件
        TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.CURRENT_PLAN_TEMPLATE_CHANGED, {
            templateId: id,
            planData: this.currentPlanData
        });
    }

    setCurrentPlanData(data) { 
        this.currentPlanData = data;
        if (this.planPromptGenerator) {
            this.planPromptGenerator.setCurrentPlanData(data);
        }
        
        // 发布当前计划模板变化事件
        TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.CURRENT_PLAN_TEMPLATE_CHANGED, {
            templateId: this.currentPlanTemplateId,
            planData: data
        });
    }

    setPlanPromptGenerator(generator) {
        this.planPromptGenerator = generator;
    }

    setPlanTemplateHandler(handler) {
        this.planTemplateHandler = handler;
    }

    /**
     * 初始化函数，设置事件监听器
     */
    async init() { // Made async
        // 获取DOM元素 (只保留清空按钮)
        this.clearBtn = document.getElementById('clearBtn');

        // 获取侧边栏切换按钮和侧边栏元素
        this.toggleLeftSidebarBtn = document.getElementById('toggleLeftSidebarBtn');
        this.toggleRightSidebarBtn = document.getElementById('toggleRightSidebarBtn');
        this.leftSidebar = document.getElementById('leftSidebar');
        this.rightSidebar = document.getElementById('rightSidebar');

        // 绑定侧边栏切换按钮事件
        if (this.toggleLeftSidebarBtn && this.leftSidebar) {
            this.toggleLeftSidebarBtn.addEventListener('click', this.handleToggleLeftSidebar.bind(this));
        }

        if (this.toggleRightSidebarBtn && this.rightSidebar) {
            this.toggleRightSidebarBtn.addEventListener('click', this.handleToggleRightSidebar.bind(this));
        }
        
        // 绑定按钮事件 (只保留清空按钮)
        this.clearBtn.addEventListener('click', this.handleClearInput.bind(this));

        // 绑定UI事件监听器
        this.bindUIEvents();

        if (typeof PlanTemplatePollingManager !== 'undefined') { 
            PlanTemplatePollingManager.init({
                getPlanId: () => this.currentPlanId,
                setPlanId: (id) => { this.currentPlanId = id; },
                getIsExecuting: () => this.isExecuting,
                setIsExecuting: (value) => {
                    this.isExecuting = value;
                    this.updateUIState();
                    // 发布执行状态变化事件
                    TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.EXECUTION_STATE_CHANGED, {
                        isExecuting: value
                    });
                },
                handlePlanData: this.handlePlanData.bind(this),
                updateUI: this.updateUIState.bind(this)
            });
        }

        this.updateUIState();
        await this.loadPlanTemplateList(); // Await loading list
        console.log('PlanTemplateManagerOld 初始化完成');
    }

    /**
     * 绑定UI事件监听器
     */
    bindUIEvents() {
        // 监听状态请求
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.STATE_REQUEST, (data) => {
            if (data.type === 'planParams') {
                const params = this.planPromptGenerator ? this.planPromptGenerator.getPlanParams() : null;
                TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.STATE_RESPONSE, {
                    planParams: params
                });
            } else if (data.requestedFields) {
                // 处理字段请求
                const response = {};
                if (data.requestedFields.includes('planTemplateList')) {
                    response.planTemplateList = this.planTemplateList;
                }
                if (data.requestedFields.includes('currentPlanTemplateId')) {
                    response.currentPlanTemplateId = this.currentPlanTemplateId;
                }
                if (data.requestedFields.includes('planParams')) {
                    response.planParams = this.planPromptGenerator ? this.planPromptGenerator.getPlanParams() : null;
                }
                TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.STATE_RESPONSE, response);
            }
        });

        // 监听计划生成完成事件
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.PLAN_GENERATED, async () => {
            await this.loadPlanTemplateList();
            // 通知状态变化
            TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.STATE_RESPONSE, {
                planTemplateList: this.planTemplateList
            });
        });

        // 监听计划模板选择事件
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.PLAN_TEMPLATE_SELECTED, (data) => {
            this.currentPlanTemplateId = data.templateId;
            this.currentPlanId = null;
            this.isExecuting = false;
            // 发布状态变化事件
            TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.CURRENT_PLAN_TEMPLATE_CHANGED, { 
                templateId: this.currentPlanTemplateId 
            });
        });

        // 监听当前计划模板变化事件
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.CURRENT_PLAN_TEMPLATE_CHANGED, (data) => {
            this.currentPlanTemplateId = data.templateId;
            this.currentPlanId = null;
            this.isExecuting = false;
            this.updateUIState();
        });
    }    /**
     * 加载计划模板列表并更新左侧边栏
     */
    async loadPlanTemplateList() {
        try {
            const response = await ManusAPI.getAllPlanTemplates();
            this.planTemplateList = response.templates || [];
            
            // 发布状态更新事件
            TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.STATE_RESPONSE, {
                planTemplateList: this.planTemplateList
            });
        } catch (error) {
            console.error('加载计划模板列表失败:', error);
        }
    }

    /**
     * 处理计划数据 (通常由轮询调用)
     * @param {object} planData - 从API获取的计划数据
     */
    handlePlanData(planData) {
        this.currentPlanData = planData; // 保存的是执行详情，不是模板
        // this.jsonEditor.value = JSON.stringify(planData, null, 2); // 执行详情不应该直接填充模板编辑器
        if (this.planPromptGenerator) {
            this.planPromptGenerator.updateApiUrl(); // 通过计划提示生成器更新API URL
        }
        this.updateUIState(); // 更新按钮状态等
        // PlanUIEvents.EventSystem.emit(PlanUIEvents.UI_EVENTS.PLAN_UPDATE, planData); // 如果有全局事件系统
    }

    /**
     * 清空输入和状态
     */
    handleClearInput() {
        // 清空JSON处理器的数据
        if (this.planTemplateHandler) {
            this.planTemplateHandler.clearJsonData();
        }
        
        // 清空计划提示生成器的数据
        if (this.planPromptGenerator) {
            this.planPromptGenerator.clearPromptData();
        }
        
        // 清空自身的状态
        this.currentPlanTemplateId = null;
        this.currentPlanId = null;
        this.currentPlanData = null;
        this.isExecuting = false;
        
        this.updateUIState();
        console.log('输入已清空');
    }

    /**
     * 更新UI状态（按钮禁用/启用等）
     */
    updateUIState() {
        // Update RunPlanButton via its own exposed method if available and needed
        if (typeof RunPlanButtonHandler !== 'undefined' && RunPlanButtonHandler.updateButtonState) {
            RunPlanButtonHandler.updateButtonState();
        }
    }


    /**
     * 处理左侧边栏折叠/展开
     */
    handleToggleLeftSidebar() {
        this.leftSidebar.classList.toggle('collapsed');
        // 可以根据需要调整主内容区域的边距或宽度
        document.querySelector('.main-content').classList.toggle('left-collapsed');
    }

    /**
     * 处理右侧边栏折叠/展开
     */
    handleToggleRightSidebar() {
        this.rightSidebar.classList.toggle('collapsed');
        // 可以根据需要调整主内容区域的边距或宽度
        document.querySelector('.main-content').classList.toggle('right-collapsed');
    }
    
    // --- Static Helper Methods ---
    /**
     * 将日期对象转换为相对时间字符串
     * @param {Date} date - 日期对象
     * @returns {string} 相对时间字符串
     */
    static getRelativeTimeString(date) {
        const now = new Date();
        const diff = now.getTime() - date.getTime();
        const seconds = Math.round(diff / 1000);
        const minutes = Math.round(seconds / 60);
        const hours = Math.round(minutes / 60);
        const days = Math.round(hours / 24);

        if (seconds < 60) return `${seconds}秒前`;
        if (minutes < 60) return `${minutes}分钟前`;
        if (hours < 24) return `${hours}小时前`;
        if (days < 7) return `${days}天前`;
        return date.toLocaleDateString(); // 超过一周直接显示日期
    }

    /**
     * 截断文本并添加省略号
     * @param {string} text - 要截断的文本
     * @param {number} maxLength - 最大长度
     * @returns {string} 截断后的文本
     */
    static truncateText(text, maxLength) {
        if (!text) return '';
        if (text.length <= maxLength) return text;
        return text.substring(0, maxLength) + '...';
    }
}

