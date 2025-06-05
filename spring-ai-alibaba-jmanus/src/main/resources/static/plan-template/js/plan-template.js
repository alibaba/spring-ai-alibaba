/**
 * 计划模板页面的主要JavaScript文件
 * 负责处理用户输入、发送API请求、展示计划结果等功能
 */

class PlanTemplateManagerOld {
    constructor() {
        // 全局变量，保存当前计划状态
        this.currentPlanData = null;
        this.isExecuting = false;
        this.planTemplateList = []; // 存储计划模板列表

        // DOM 元素引用
        // 注意：计划相关的输入元素现在由各自的专门处理器管理

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
        return this.planPromptGenerator ? this.planPromptGenerator.getCurrentPlanTemplateId() : null;
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
            templateId: this.getCurrentPlanTemplateId(),
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
        // 绑定UI事件监听器
        this.bindUIEvents();


        // 如果需要计划执行状态管理，请使用 PlanExecutionManagerController

        this.updateUIState();
        // await this.loadPlanTemplateList(); // Await loading list
        console.log('PlanTemplateManagerOld 初始化完成');
    }

    /**
     * 绑定UI事件监听器
     */
    bindUIEvents() {
 
        // 监听计划生成完成事件
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.PLAN_GENERATED, async () => {
            await this.loadPlanTemplateList();
            // 通知状态变化
        });

        // 监听计划模板选择事件
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.PLAN_TEMPLATE_SELECTED, (data) => {
            this.setCurrentPlanTemplateId(data.templateId);
            this.isExecuting = false;
            // 发布状态变化事件
            TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.CURRENT_PLAN_TEMPLATE_CHANGED, { 
                templateId: data.templateId 
            });
        });

        // 监听当前计划模板变化事件
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.CURRENT_PLAN_TEMPLATE_CHANGED, (data) => {
            this.setCurrentPlanTemplateId(data.templateId);
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
     * 更新UI状态（按钮禁用/启用等）
     */
    updateUIState() {
        // Update RunPlanButton via its own exposed method if available and needed
        if (typeof RunPlanButtonHandler !== 'undefined' && RunPlanButtonHandler.updateButtonState) {
            RunPlanButtonHandler.updateButtonState();
        }
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

