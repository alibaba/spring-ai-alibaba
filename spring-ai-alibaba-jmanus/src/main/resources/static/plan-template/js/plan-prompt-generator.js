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
        this.planParamsInput = document.getElementById('plan-params');
        this.clearParamBtn = document.getElementById('clearParamBtn');
        this.clearBtn = document.getElementById('clearBtn');
        this.apiUrlElement = document.querySelector('.api-url');

        // 绑定事件监听器
        this.bindEventListeners();
        this.bindUIEvents();

        // 初始化UI状态
        this.updateUIState();
        this.updateApiUrl();

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
            this.currentPlanTemplateId = data.templateId || data.planTemplateId; // 兼容处理
            this.currentPlanData = data.planData;
            if (data.planData && this.planPromptInput) {
                this.planPromptInput.value = data.planData.prompt || '';
            }
            this.updateUIState();
            this.updateApiUrl();
        });

        // 监听状态请求
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.STATE_REQUEST, (data) => {
            if (data.type === 'planParams') {
                TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.STATE_RESPONSE, {
                    planParams: this.getPlanParams()
                });
            }
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
                if (this.planParamsInput) {
                    this.planParamsInput.value = '';
                    this.updateApiUrl();
                }
            });
        }

        // 清空所有数据按钮事件
        if (this.clearBtn) {
            this.clearBtn.addEventListener('click', this.handleClearInput.bind(this));
        }

        // 参数输入变化事件
        if (this.planParamsInput) {
            this.planParamsInput.addEventListener('input', () => {
                this.updateApiUrl();
                // 发布参数变化事件
                TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.PLAN_PARAMS_CHANGED, {
                    params: this.planParamsInput.value.trim()
                });
            });
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
        
        // 发布生成状态变化事件
        TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.GENERATION_STATE_CHANGED, {
            isGenerating: true
        });

        try {
            // 通过事件获取当前JSON内容
            let existingJson = null;
            const jsonContent = await this.requestJsonContent();
            
            if (jsonContent && jsonContent.trim()) {
                try {
                    existingJson = JSON.parse(jsonContent.trim());
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

                // 发布计划生成完成事件
                TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.PLAN_GENERATED, {
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
            
            this.updateApiUrl();

        } catch (error) {
            console.error('生成计划失败:', error);
            alert('生成计划失败: ' + error.message);
        } finally {
            this.isGenerating = false;
            this.updateUIState();
            
            // 发布生成状态变化事件
            TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.GENERATION_STATE_CHANGED, {
                isGenerating: false
            });
        }
    }

    /**
     * 通过事件请求JSON内容
     * @returns {Promise<string>} JSON内容
     */
    async requestJsonContent() {
        return new Promise((resolve) => {
            const handleResponse = (data) => {
                TaskPilotUIEvent.EventSystem.off(TaskPilotUIEvent.UI_EVENTS.STATE_RESPONSE, handleResponse);
                resolve(data.jsonContent || '');
            };
            
            TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.STATE_RESPONSE, handleResponse);
            TaskPilotUIEvent.EventSystem.emit(TaskPilotUIEvent.UI_EVENTS.STATE_REQUEST, {
                type: 'jsonContent'
            });
            
            // 超时处理
            setTimeout(() => {
                TaskPilotUIEvent.EventSystem.off(TaskPilotUIEvent.UI_EVENTS.STATE_RESPONSE, handleResponse);
                resolve('');
            }, 100);
        });
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
