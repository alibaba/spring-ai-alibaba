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
        this.isGenerating = false;
        this.isExecuting = false;
        this.planTemplateList = []; // 存储计划模板列表

        // 版本控制相关变量
        this.planVersions = []; // 存储所有版本的计划JSON
        this.currentVersionIndex = -1; // 当前版本索引

        // DOM 元素引用
        this.planPromptInput = null;
        this.planParamsInput = null;
        this.generatePlanBtn = null;
        this.jsonEditor = null;
        this.modifyPlanBtn = null;
        this.clearBtn = null;
        this.clearParamBtn = null;
        this.apiUrlElement = null;

        // 侧边栏折叠/展开相关变量
        this.toggleLeftSidebarBtn = null;
        this.toggleRightSidebarBtn = null;
        this.leftSidebar = null;
        this.rightSidebar = null;

        // UI Handler for the plan template list - REMOVED
        // this.planTemplateListUIHandler = null; 
    }

    // Getter methods
    getIsGenerating() {
        return this.isGenerating;
    }

    getMainIsExecuting() { // Assuming this maps to isExecuting
        return this.isExecuting;
    }

    getCurrentPlanTemplateId() {
        return this.currentPlanTemplateId;
    }

    getPlanParams() {
        if (this.planParamsInput) {
            return this.planParamsInput.value.trim();
        }
        return null;
    }

    setMainIsExecuting(value) { // Setter for isExecuting, used by RunPlanButtonHandler
        this.isExecuting = value;
        this.updateUIState(); // Update UI when execution state changes
    }

    /**
     * 初始化函数，设置事件监听器
     */
    async init() { // Made async
        // 获取DOM元素
        this.planPromptInput = document.getElementById('plan-prompt');
        this.planParamsInput = document.getElementById('plan-params');
        this.generatePlanBtn = document.getElementById('generatePlanBtn');
        this.jsonEditor = document.getElementById('plan-json-editor');
        this.modifyPlanBtn = document.getElementById('modifyPlanBtn');
        this.clearBtn = document.getElementById('clearBtn');
        this.clearParamBtn = document.getElementById('clearParamBtn');
        this.apiUrlElement = document.querySelector('.api-url');

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
        // 绑定按钮事件
        this.generatePlanBtn.addEventListener('click', this.handleGeneratePlan.bind(this));
        this.modifyPlanBtn.addEventListener('click', this.handleModifyPlan.bind(this));
        this.clearBtn.addEventListener('click', this.handleClearInput.bind(this));

        if (this.clearParamBtn) {
            this.clearParamBtn.addEventListener('click', () => {
                if (this.planParamsInput) {
                    this.planParamsInput.value = '';
                    this.updateApiUrl();
                }
            });
        }

        if (this.planParamsInput) {
            this.planParamsInput.addEventListener('input', this.updateApiUrl.bind(this));
        }

        document.getElementById('rollbackJsonBtn').addEventListener('click', this.handleRollbackJson.bind(this));
        document.getElementById('restoreJsonBtn').addEventListener('click', this.handleRestoreJson.bind(this));
        document.getElementById('compareJsonBtn').addEventListener('click', this.handleCompareJson.bind(this));

        if (typeof PlanTemplatePollingManager !== 'undefined') { 
            PlanTemplatePollingManager.init({
                getPlanId: () => this.currentPlanId,
                setPlanId: (id) => { this.currentPlanId = id; },
                getIsExecuting: () => this.isExecuting,
                setIsExecuting: (value) => {
                    this.isExecuting = value;
                    this.updateUIState();
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
     * 加载计划模板列表并更新左侧边栏
     */
    async loadPlanTemplateList() {
        try {
            const response = await ManusAPI.getAllPlanTemplates();
            this.planTemplateList = response.templates || [];
            // REMOVED: Call to this.planTemplateListUIHandler.updatePlanTemplateListUI();
            // if (this.planTemplateListUIHandler) {
            //     this.planTemplateListUIHandler.updatePlanTemplateListUI();
            // }
        } catch (error) {
            console.error('加载计划模板列表失败:', error);
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
            if (this.jsonEditor.value.trim()) {
                try {
                    existingJson = JSON.parse(this.jsonEditor.value.trim());
                } catch (e) {
                    alert('当前JSON格式无效，无法作为生成基础。将忽略当前JSON。');
                    existingJson = null;
                }
            }

            let response;
            if (this.currentPlanTemplateId) { //  && (this.currentPlanData || existingJson) // 简化判断，只要有ID就尝试更新
                console.log('正在更新现有计划模板:', this.currentPlanTemplateId);
                response = await ManusAPI.updatePlanTemplate(this.currentPlanTemplateId, query, this.jsonEditor.value.trim() || null); // 传递原始JSON字符串
            } else {
                console.log('正在创建新计划模板');
                response = await ManusAPI.generatePlan(query, this.jsonEditor.value.trim() || null); // 传递原始JSON字符串
            }
            
            this.currentPlanData = response.plan; // API返回的应该是完整的plan template对象

            if (this.currentPlanData && this.currentPlanData.json) {
                this.jsonEditor.value = this.currentPlanData.json; // 显示JSON
                this.saveToVersionHistory(this.currentPlanData.json);
                this.currentPlanTemplateId = this.currentPlanData.id; // 更新ID
                this.planPromptInput.value = this.currentPlanData.prompt || query; // 更新Prompt
            } else {
                alert('计划生成或更新未能返回有效的JSON数据。');
            }
            
            await this.loadPlanTemplateList(); // 重新加载列表以反映更新或新建
            this.updatePlanTemplateListUI(); // 确保选中项正确
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
     * 处理计划数据 (通常由轮询调用)
     * @param {object} planData - 从API获取的计划数据
     */
    handlePlanData(planData) {
        this.currentPlanData = planData; // 保存的是执行详情，不是模板
        // this.jsonEditor.value = JSON.stringify(planData, null, 2); // 执行详情不应该直接填充模板编辑器
        this.updateApiUrl(); // API URL可能基于执行ID
        this.updateUIState(); // 更新按钮状态等
        // PlanUIEvents.EventSystem.emit(PlanUIEvents.UI_EVENTS.PLAN_UPDATE, planData); // 如果有全局事件系统
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
                // 如果参数不是有效的JSON，则不附加到URL，或者可以显示错误
                console.warn("URL参数不是有效的JSON字符串，已忽略。");
            }
        }
        this.apiUrlElement.textContent = url;
    }


    /**
     * 修改计划（保存当前编辑器中的JSON到服务器）
     */
    async handleModifyPlan() {
        if (!this.currentPlanTemplateId) {
            alert('没有选中的计划模板，无法修改。请先选择或生成一个计划。');
            return;
        }

        const jsonContent = this.jsonEditor.value.trim();
        if (!jsonContent) {
            alert('JSON内容不能为空。');
            return;
        }

        try {
            // 尝试解析JSON以验证格式
            JSON.parse(jsonContent);
        } catch (e) {
            alert('JSON格式无效，请修正后再保存。\\n错误: ' + e.message);
            return;
        }
        
        this.isGenerating = true; // 复用isGenerating状态和UI反馈
        this.updateUIState();

        try {
            // 调用保存接口，注意区分是保存模板还是执行后的计划（这里是模板）
            // ManusAPI.updatePlanTemplate(this.currentPlanTemplateId, this.planPromptInput.value, jsonContent);
            // 改为调用新的专用保存接口
            await ManusAPI.savePlanTemplate(this.currentPlanTemplateId, jsonContent);

            this.saveToVersionHistory(jsonContent); // 保存到本地版本历史
            alert('计划修改已保存成功！');
            // 可选择重新加载数据
            // const updatedTemplate = await ManusAPI.getPlanTemplateDetails(this.currentPlanTemplateId);
            // this.currentPlanData = updatedTemplate;
            // this.planPromptInput.value = updatedTemplate.prompt || '';
            // this.planParamsInput.value = updatedTemplate.params || '';
        } catch (error) {
            console.error('保存计划修改失败:', error);
            alert('保存计划修改失败: ' + error.message);
        } finally {
            this.isGenerating = false;
            this.updateUIState();
        }
    }


    /**
     * 清空输入和状态
     */
    handleClearInput() {
        this.planPromptInput.value = '';
        this.jsonEditor.value = '';
        if (this.planParamsInput) {
            this.planParamsInput.value = '';
        }
        this.currentPlanTemplateId = null;
        this.currentPlanId = null;
        this.currentPlanData = null;
        this.isGenerating = false;
        this.isExecuting = false;
        this.planVersions = [];
        this.currentVersionIndex = -1;
        // REMOVED: Call to this.planTemplateListUIHandler.updatePlanTemplateListUI();
        // if (this.planTemplateListUIHandler) {
        //     this.planTemplateListUIHandler.updatePlanTemplateListUI();
        // }
        this.updateApiUrl();
        this.updateUIState();
        console.log('输入已清空');
    }

    /**
     * 更新UI状态（按钮禁用/启用等）
     */
    updateUIState() {
        this.generatePlanBtn.disabled = this.isGenerating || this.isExecuting;
        this.modifyPlanBtn.disabled = this.isGenerating || this.isExecuting || !this.currentPlanTemplateId;
        // runPlanBtn is managed by RunPlanButtonHandler, but its state depends on these:
        // isExecuting, isGenerating, currentPlanTemplateId

        if (this.isGenerating) {
            this.generatePlanBtn.textContent = '生成中...';
        } else {
            this.generatePlanBtn.textContent = this.currentPlanTemplateId ? '更新计划' : '生成计划';
        }
        
        // Update RunPlanButton via its own exposed method if available and needed
        if (typeof RunPlanButtonHandler !== 'undefined' && RunPlanButtonHandler.updateButtonState) {
            RunPlanButtonHandler.updateButtonState();
        }

        // 版本控制按钮状态
        const rollbackBtn = document.getElementById('rollbackJsonBtn');
        const restoreBtn = document.getElementById('restoreJsonBtn');
        if (rollbackBtn) rollbackBtn.disabled = this.planVersions.length <= 1 || this.currentVersionIndex <= 0;
        if (restoreBtn) restoreBtn.disabled = this.planVersions.length <= 1 || this.currentVersionIndex >= this.planVersions.length - 1;

        // API URL (already handled by updateApiUrl)
        // this.updateApiUrl();
    }

    /**
     * 保存当前JSON到版本历史
     * @param {string} jsonText - JSON文本内容
     */
    saveToVersionHistory(jsonText) {
        if (this.currentVersionIndex < this.planVersions.length - 1) {
            this.planVersions = this.planVersions.slice(0, this.currentVersionIndex + 1);
        }
        this.planVersions.push(jsonText);
        this.currentVersionIndex = this.planVersions.length - 1;
        this.updateUIState(); // 更新版本控制按钮状态
    }

    /**
     * 处理回滚JSON按钮点击
     */
    handleRollbackJson() {
        if (this.currentVersionIndex > 0) {
            this.currentVersionIndex--;
            this.jsonEditor.value = this.planVersions[this.currentVersionIndex];
            this.updateUIState();
        }
    }

    /**
     * 处理恢复JSON按钮点击
     */
    handleRestoreJson() {
        if (this.currentVersionIndex < this.planVersions.length - 1) {
            this.currentVersionIndex++;
            this.jsonEditor.value = this.planVersions[this.currentVersionIndex];
            this.updateUIState();
        }
    }

    /**
     * 处理对比JSON按钮点击
     */
    handleCompareJson() {
        if (this.planVersions.length < 2) {
            alert('至少需要两个版本才能进行比较。');
            return;
        }
        // 简单实现：比较当前版本和上一个版本
        const currentJson = this.planVersions[this.currentVersionIndex];
        const previousJson = this.planVersions[this.currentVersionIndex -1 < 0 ? 0 : this.currentVersionIndex -1]; //防止-1

        // 此处可以使用更复杂的差异比较库，例如 diff2html 或 jsdiff
        // 为了简单起见，这里只在控制台打印
        console.log("当前版本:", currentJson);
        console.log("上一个版本:", previousJson);
        alert('版本比较结果已打印到控制台。请使用更专业的工具查看详细差异。');
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

