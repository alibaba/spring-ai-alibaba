/**
 * 计划提示生成器类
 * 负责处理计划提示输入、参数处理和计划生成功能
 */
class PlanPromptGenerator {
    constructor() {

        // 当前计划状态
        this.currentPlanTemplateId = null;
        this.currentPlanData = null;
        this.isGenerating = false;

        // DOM 元素引用 - 只包含计划提示生成相关的元素
        this.planPromptInput = null;
        this.generatePlanBtn = null;
        this.clearParamBtn = null;
        this.clearBtn = null; // 清空所有数据按钮
        this.apiUrlElement = null;

        // 缓存的状态数据，用于减少事件查询
        this.cachedExecutionState = false;
    }

    /**
     * 初始化计划提示生成器
     */
    init() {
        // 获取DOM元素
        this.planPromptInput = document.getElementById('plan-prompt');
        this.generatePlanBtn = document.getElementById('generatePlanBtn');
        this.clearParamBtn = document.getElementById('clearParamBtn');
        this.clearBtn = document.getElementById('clearBtn');
        this.apiUrlElement = document.querySelector('.api-url');

        // 绑定事件监听器
        this.bindEventListeners();
        this.bindUIEvents();

        // 初始化UI状态
        this.updateUIState();

        console.log('PlanPromptGenerator 初始化完成');
    }

    /**
     * 绑定UI事件监听器
     */
    bindUIEvents() {
        // 监听执行状态变化
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.EXECUTION_STATE_CHANGED, (data) => {
            this.cachedExecutionState = data.isExecuting;
            this.updateUIState();
        });

        // 监听当前计划模板变化
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.CURRENT_PLAN_TEMPLATE_CHANGED, (data) => {
            this.currentPlanTemplateId = data.templateId ; // 兼容处理
            this.currentPlanData = data.planData;
            if (data.planData && this.planPromptInput) {
                this.planPromptInput.value = data.planData.prompt || '';
            }
            this.updateUIState();
        });

       
    }

    /**
     * 绑定事件监听器
     */
    bindEventListeners() {
        // 生成计划按钮事件
        if (this.generatePlanBtn) {
            this.generatePlanBtn.addEventListener('click', this.handleGeneratePlan.bind(this));
        }

        // 清除参数按钮事件
        if (this.clearParamBtn) {
            this.clearParamBtn.addEventListener('click', () => {
                console.log('清除参数按钮点击事件已移除');
            });
        }

        // 清空所有数据按钮事件
        if (this.clearBtn) {
            this.clearBtn.addEventListener('click', this.handleClearInput.bind(this));
        }
    }

    /**
     * 生成计划
     */
    async handleGeneratePlan() {
        const query = this.planPromptInput.value.trim();
        if (!query) {
            alert('请输入计划需求描述');
            return;
        }
        if (this.isGenerating) return;

        // 读取 jsonContent 从 plan-json-editor 字段，如果不为空则添加到参数中
        const planJsonEditor = document.getElementById('plan-json-editor');
        const jsonContent = planJsonEditor && planJsonEditor.value.trim() ? planJsonEditor.value.trim() : null;

        this.isGenerating = true;
        this.updateUIState();
        
        // 发布生成状态变化事件
        TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.GENERATION_STATE_CHANGED, {
            isGenerating: true
        });

        try {

            let response;
            if (this.currentPlanTemplateId) {
                console.log('正在更新现有计划模板:', this.currentPlanTemplateId);
                response = await ManusAPI.updatePlanTemplate(
                    this.currentPlanTemplateId, 
                    query, 
                    jsonContent || null
                );
            } else {
                console.log('正在创建新计划模板');
                response = await ManusAPI.generatePlan(
                    query, 
                    jsonContent || null
                );
            }
            
            // 处理API返回的数据结构
            console.log('API响应数据:', response);
            
            // 根据实际API响应结构提取数据
            const planJson = response.planJson || (response.plan && response.plan.json) || null;
            const planTemplateId = response.planTemplateId || (response.plan && response.plan.id) || null;
            const planData = response.plan || null;

            if (planJson) {
                // 保存当前计划数据
                this.currentPlanData = {
                    json: planJson,
                    id: planTemplateId,
                    prompt: query,
                    plan: planData
                };
                
                // 通过事件设置JSON内容
                TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.JSON_CONTENT_SET, {
                    content: planJson
                });
                
                // 更新当前模板ID
                this.currentPlanTemplateId = planTemplateId;
                
                // 更新Prompt输入框
                this.planPromptInput.value = query;
                
                // 发布计划模板变化事件
                TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.CURRENT_PLAN_TEMPLATE_CHANGED, {
                    templateId: this.currentPlanTemplateId,
                    planData: this.currentPlanData
                });
                
                // 检查是否是重复内容并显示相应提示
                if (response.duplicate) {
                    console.log('生成的计划内容与现有版本相同');
                    // 可以在这里添加一个不那么突兀的提示，比如toast通知
                    // 暂时使用console.log记录，不中断用户体验
                } else if (response.saved) {
                    console.log('新版本已保存:', response.saveMessage);
                }
                
                console.log('计划生成成功，模板ID:', this.currentPlanTemplateId);
            } else {
                console.warn('API响应数据结构异常:', response);
                alert('计划生成或更新未能返回有效的JSON数据。');
            }

        } catch (error) {
            console.error('生成计划失败:', error);
            alert('生成计划失败: ' + error.message);
        } finally {
            this.isGenerating = false;
            this.updateUIState();
            
            // 发布生成状态变化事件，包含是否成功的信息
            TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.GENERATION_STATE_CHANGED, {
                isGenerating: false,
                success: this.currentPlanTemplateId !== null,
                templateId: this.currentPlanTemplateId,
                planData: this.currentPlanData
            });
        }
    }


    /**
     * 更新UI状态（按钮禁用/启用等）
     */
    updateUIState() {
        if (this.generatePlanBtn) {
            this.generatePlanBtn.disabled = this.isGenerating || this.cachedExecutionState;
            
            if (this.isGenerating) {
                this.generatePlanBtn.textContent = '生成中...';
            } else {
                this.generatePlanBtn.textContent = this.currentPlanTemplateId ? '更新计划' : '生成计划';
            }
        }
    }

    /**
     * 清空提示相关的输入和状态
     */
    clearPromptData() {
        if (this.planPromptInput) {
            this.planPromptInput.value = '';
        }
        this.currentPlanTemplateId = null;
        this.currentPlanData = null;
        this.isGenerating = false;
        this.updateUIState();
        console.log('计划提示数据已清空');
    }

    /**
     * 处理清空所有输入的操作
     */
    handleClearInput() {
        // 清空自身的提示数据
        this.clearPromptData();
        
        console.log('所有输入已清空');
    }

    // Getter methods
    getIsGenerating() {
        return this.isGenerating;
    }

    getCurrentPlanTemplateId() {
        return this.currentPlanTemplateId;
    }

    getCurrentPlanData() {
        return this.currentPlanData;
    }

    getPlanParams() {
        return null;
    }

    getPlanPrompt() {
        if (this.planPromptInput) {
            return this.planPromptInput.value.trim();
        }
        return null;
    }

    // Setter methods
    setCurrentPlanTemplateId(id) {
        this.currentPlanTemplateId = id;
        this.updateUIState();
    }

    setCurrentPlanData(data) {
        this.currentPlanData = data;
        if (data && this.planPromptInput) {
            this.planPromptInput.value = data.prompt || '';
        }
    }

    setPlanParams(params) {
        console.warn('setPlanParams 方法已移除');
    }
}
