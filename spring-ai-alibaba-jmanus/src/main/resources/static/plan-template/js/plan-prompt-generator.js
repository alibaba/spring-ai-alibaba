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
        this.planParamsInput = null;
        this.clearParamBtn = null;
        this.apiUrlElement = null;

        // 外部依赖的实例引用
        this.planTemplateManager = null;
    }

    /**
     * 初始化计划提示生成器
     * @param {Object} planTemplateManager - PlanTemplateManagerOld 实例的引用
     */
    init(planTemplateManager) {
        this.planTemplateManager = planTemplateManager;

        // 获取DOM元素
        this.planPromptInput = document.getElementById('plan-prompt');
        this.generatePlanBtn = document.getElementById('generatePlanBtn');
        this.planParamsInput = document.getElementById('plan-params');
        this.clearParamBtn = document.getElementById('clearParamBtn');
        this.apiUrlElement = document.querySelector('.api-url');

        // 绑定事件监听器
        this.bindEventListeners();

        // 初始化UI状态
        this.updateUIState();
        this.updateApiUrl();

        console.log('PlanPromptGenerator 初始化完成');
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
                if (this.planParamsInput) {
                    this.planParamsInput.value = '';
                    this.updateApiUrl();
                }
            });
        }

        // 参数输入变化事件
        if (this.planParamsInput) {
            this.planParamsInput.addEventListener('input', this.updateApiUrl.bind(this));
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

        this.isGenerating = true;
        this.updateUIState();

        try {
            let existingJson = null;
            const jsonEditor = this.planTemplateManager.getJsonEditor();
            
            if (jsonEditor && jsonEditor.value.trim()) {
                try {
                    existingJson = JSON.parse(jsonEditor.value.trim());
                } catch (e) {
                    alert('当前JSON格式无效，无法作为生成基础。将忽略当前JSON。');
                    existingJson = null;
                }
            }

            let response;
            if (this.currentPlanTemplateId) {
                console.log('正在更新现有计划模板:', this.currentPlanTemplateId);
                response = await ManusAPI.updatePlanTemplate(
                    this.currentPlanTemplateId, 
                    query, 
                    jsonEditor ? jsonEditor.value.trim() || null : null
                );
            } else {
                console.log('正在创建新计划模板');
                response = await ManusAPI.generatePlan(
                    query, 
                    jsonEditor ? jsonEditor.value.trim() || null : null
                );
            }
            
            this.currentPlanData = response.plan;

            if (this.currentPlanData && this.currentPlanData.json) {
                // 通过JSON处理器更新JSON编辑器内容
                if (this.planTemplateManager.planTemplateHandler) {
                    this.planTemplateManager.planTemplateHandler.setJsonContent(this.currentPlanData.json);
                }
                
                // 更新当前模板ID
                this.currentPlanTemplateId = this.currentPlanData.id;
                
                // 更新Prompt输入框
                this.planPromptInput.value = this.currentPlanData.prompt || query;
                
                // 通知主管理器更新状态
                if (this.planTemplateManager) {
                    this.planTemplateManager.setCurrentPlanTemplateId(this.currentPlanTemplateId);
                    this.planTemplateManager.setCurrentPlanData(this.currentPlanData);
                    await this.planTemplateManager.loadPlanTemplateList();
                }
            } else {
                alert('计划生成或更新未能返回有效的JSON数据。');
            }
            
            this.updateApiUrl();

        } catch (error) {
            console.error('生成计划失败:', error);
            alert('生成计划失败: ' + error.message);
        } finally {
            this.isGenerating = false;
            this.updateUIState();
        }
    }

    /**
     * 更新API URL，添加用户提供的参数
     */
    updateApiUrl() {
        if (!this.apiUrlElement) return;

        let url = `${ManusAPI.BASE_URL}/execute/${this.currentPlanTemplateId || '{planTemplateId}'}`;
        if (this.planParamsInput && this.planParamsInput.value.trim()) {
            try {
                const params = JSON.parse(this.planParamsInput.value.trim());
                const queryString = new URLSearchParams(params).toString();
                if (queryString) {
                    url += `?${queryString}`;
                }
            } catch (e) {
                console.warn("URL参数不是有效的JSON字符串，已忽略。");
            }
        }
        this.apiUrlElement.textContent = url;
    }

    /**
     * 更新UI状态（按钮禁用/启用等）
     */
    updateUIState() {
        if (this.generatePlanBtn) {
            const isExecuting = this.planTemplateManager ? this.planTemplateManager.getMainIsExecuting() : false;
            this.generatePlanBtn.disabled = this.isGenerating || isExecuting;
            
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
        if (this.planParamsInput) {
            this.planParamsInput.value = '';
        }
        this.currentPlanTemplateId = null;
        this.currentPlanData = null;
        this.isGenerating = false;
        this.updateApiUrl();
        this.updateUIState();
        console.log('计划提示数据已清空');
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
        if (this.planParamsInput) {
            return this.planParamsInput.value.trim();
        }
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
        this.updateApiUrl();
    }

    setCurrentPlanData(data) {
        this.currentPlanData = data;
        if (data && this.planPromptInput) {
            this.planPromptInput.value = data.prompt || '';
        }
    }

    setPlanParams(params) {
        if (this.planParamsInput) {
            this.planParamsInput.value = params || '';
            this.updateApiUrl();
        }
    }
}
