/**
 * 计划模板处理器类
 * 负责处理编辑、版本控制、模板保存等功能
 */
class PlanTemplateHandler {
    constructor() {
        // 版本控制相关变量
        this.planVersions = []; // 存储所有版本的计划
        this.currentVersionIndex = -1; // 当前版本索引

        // DOM 元素引用
        this.jsonEditor = null;
        this.modifyPlanBtn = null;

        // 缓存的状态数据
        this.cachedGeneratingState = false;
        this.cachedExecutionState = false;
        this.currentPlanTemplateId = null;
    }

    /**
     * 初始化处理器
     */
    init() {
        // 获取DOM元素
        this.jsonEditor = document.getElementById('plan-json-editor');
        this.modifyPlanBtn = document.getElementById('modifyPlanBtn');

        // 绑定事件监听器
        this.bindEventListeners();
        this.bindUIEvents();

        // 初始化UI状态
        this.updateUIState();

        console.log('PlanTemplateHandler 初始化完成');
    }

    /**
     * 绑定UI事件监听器
     */
    bindUIEvents() {
        // 监听生成状态变化
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.GENERATION_STATE_CHANGED, (data) => {
            this.cachedGeneratingState = data.isGenerating;
            this.updateUIState();
        });

        // 监听执行状态变化
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.EXECUTION_STATE_CHANGED, (data) => {
            this.cachedExecutionState = data.isExecuting;
            this.updateUIState();
        });

        // 监听当前计划模板变化
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.CURRENT_PLAN_TEMPLATE_CHANGED, (data) => {
            this.currentPlanTemplateId = data.templateId || data.planTemplateId; // 兼容两种字段名
            console.log('[PlanTemplateHandler] 接收到计划模板变化事件:', data, '设置currentPlanTemplateId为:', this.currentPlanTemplateId);
            this.updateUIState();
        });

        // 监听JSON内容设置请求
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.JSON_CONTENT_SET, (data) => {
            this.setContent(data.content);
        });

        // 监听JSON内容清空请求
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.JSON_CONTENT_CLEAR, () => {
            this.clearData();
        });

        // 监听版本历史设置请求
        TaskPilotUIEvent.EventSystem.on(TaskPilotUIEvent.UI_EVENTS.VERSION_HISTORY_SET, (data) => {
            this.setPlanVersions(data.versions);
            console.log('[PlanTemplateHandler] 版本历史已设置，共', data.versions.length, '个版本');
        });

       

    }

    /**
     * 绑定事件监听器
     */
    bindEventListeners() {
        // 修改计划按钮事件
        if (this.modifyPlanBtn) {
            this.modifyPlanBtn.addEventListener('click', this.handleModifyPlan.bind(this));
        }

        // 版本控制按钮事件
        document.getElementById('rollbackJsonBtn').addEventListener('click', this.handleRollbackJson.bind(this));
        document.getElementById('restoreJsonBtn').addEventListener('click', this.handleRestoreJson.bind(this));
    }

    /**
     * 修改计划（保存当前编辑器中的内容到服务器）
     */
    async handleModifyPlan() {
        console.log('[PlanTemplateHandler] handleModifyPlan 被调用, currentPlanTemplateId:', this.currentPlanTemplateId);
        
        if (!this.currentPlanTemplateId) {
            console.warn('[PlanTemplateHandler] 没有选中的计划模板，无法修改');
            alert('没有选中的计划模板，无法修改。请先选择或生成一个计划。');
            return;
        }

        const jsonContent = this.jsonEditor.value.trim();
        if (!jsonContent) {
            alert('内容不能为空。');
            return;
        }

        try {
            // 尝试解析以验证格式
            JSON.parse(jsonContent);
        } catch (e) {
            alert('格式无效，请修正后再保存。\\n错误: ' + e.message);
            return;
        }
        
        if (this.cachedGeneratingState) return;

        this.updateUIState();

        try {
            // 调用保存接口，注意区分是保存模板还是执行后的计划（这里是模板）
            const saveResult = await ManusAPI.savePlanTemplate(this.currentPlanTemplateId, jsonContent);
            
            // 根据保存结果显示不同的消息
            if (saveResult.duplicate) {
                alert(`保存完成：${saveResult.message}\n\n当前版本数：${saveResult.versionCount}`);
            } else if (saveResult.saved) {
                this.saveToVersionHistory(jsonContent); // 保存到本地版本历史
                alert(`保存成功：${saveResult.message}\n\n当前版本数：${saveResult.versionCount}`);
            } else {
                alert(`保存状态：${saveResult.message}`);
            }
        } catch (error) {
            console.error('保存计划修改失败:', error);
            alert('保存计划修改失败: ' + error.message);
        } finally {
            this.updateUIState();
        }
    }

    /**
     * 更新UI状态（按钮禁用/启用等）
     */
    updateUIState() {
        if (this.modifyPlanBtn) {
            this.modifyPlanBtn.disabled = this.cachedGeneratingState || this.cachedExecutionState || !this.currentPlanTemplateId;
        }

        // 版本控制按钮状态
        const rollbackBtn = document.getElementById('rollbackJsonBtn');
        const restoreBtn = document.getElementById('restoreJsonBtn');
        
        const canRollback = this.planVersions.length > 1 && this.currentVersionIndex > 0;
        const canRestore = this.planVersions.length > 1 && this.currentVersionIndex < this.planVersions.length - 1;
        
        if (rollbackBtn) {
            rollbackBtn.disabled = !canRollback;
            console.log('[PlanTemplateHandler] 回滚按钮状态:', canRollback ? '启用' : '禁用', 
                       '版本数:', this.planVersions.length, '当前索引:', this.currentVersionIndex);
        }
        if (restoreBtn) {
            restoreBtn.disabled = !canRestore;
            console.log('[PlanTemplateHandler] 恢复按钮状态:', canRestore ? '启用' : '禁用', 
                       '版本数:', this.planVersions.length, '当前索引:', this.currentVersionIndex);
        }
    }

    /**
     * 保存当前内容到版本历史
     * @param {string} content - 内容
     */
    saveToVersionHistory(content) {
        if (this.currentVersionIndex < this.planVersions.length - 1) {
            this.planVersions = this.planVersions.slice(0, this.currentVersionIndex + 1);
        }
        this.planVersions.push(content);
        this.currentVersionIndex = this.planVersions.length - 1;
        this.updateUIState(); // 更新版本控制按钮状态
    }

    /**
     * 处理回滚按钮点击
     */
    handleRollbackJson() {
        console.log('[PlanTemplateHandler] 回滚被点击，当前版本索引:', this.currentVersionIndex, '总版本数:', this.planVersions.length);
        if (this.currentVersionIndex > 0) {
            this.currentVersionIndex--;
            this.jsonEditor.value = this.planVersions[this.currentVersionIndex];
            this.updateUIState();
            console.log('[PlanTemplateHandler] 已回滚到版本索引:', this.currentVersionIndex);
        } else {
            console.log('[PlanTemplateHandler] 无法回滚，已经是最早版本');
        }
    }

    /**
     * 处理恢复按钮点击
     */
    handleRestoreJson() {
        console.log('[PlanTemplateHandler] 恢复被点击，当前版本索引:', this.currentVersionIndex, '总版本数:', this.planVersions.length);
        if (this.currentVersionIndex < this.planVersions.length - 1) {
            this.currentVersionIndex++;
            this.jsonEditor.value = this.planVersions[this.currentVersionIndex];
            this.updateUIState();
            console.log('[PlanTemplateHandler] 已恢复到版本索引:', this.currentVersionIndex);
        } else {
            console.log('[PlanTemplateHandler] 无法恢复，已经是最新版本');
        }
    }

    /**
     * 清空相关的数据
     */
    clearData() {
        if (this.jsonEditor) {
            this.jsonEditor.value = '';
        }
        this.planVersions = [];
        this.currentVersionIndex = -1;
        this.updateUIState();
        console.log('数据已清空');
    }

    /**
     * 设置编辑器内容
     * @param {string} content - 内容
     */
    setContent(content) {
        if (this.jsonEditor && content) {
            this.jsonEditor.value = content;
            // 只有当版本历史为空时才保存到版本历史
            // 这样避免重复保存，特别是从现有模板加载时
            if (this.planVersions.length === 0) {
                this.saveToVersionHistory(content);
            } else {
                // 如果已有版本历史，只更新当前版本索引到最新
                this.currentVersionIndex = this.planVersions.length - 1;
                this.updateUIState();
            }
        }
    }

    /**
     * 获取编辑器内容
     * @returns {string} 内容
     */
    getContent() {
        return this.jsonEditor ? this.jsonEditor.value.trim() : '';
    }

    /**
     * 获取当前计划模板ID
     * @returns {string|null} 当前计划模板ID
     */
    getCurrentPlanTemplateId() {
        return this.currentPlanTemplateId;
    }

    // Getter methods
    getJsonEditor() {
        return this.jsonEditor;
    }

    getPlanVersions() {
        return this.planVersions;
    }

    getCurrentVersionIndex() {
        return this.currentVersionIndex;
    }

    // Setter methods
    setPlanVersions(versions) {
        this.planVersions = versions || [];
        this.currentVersionIndex = this.planVersions.length - 1;
        this.updateUIState();
    }

    // 兼容性方法，用于与外部代码保持兼容
    getEditor() {
        return this.getJsonEditor();
    }

    setJsonContent(content) {
        this.setContent(content);
    }

    clearJsonData() {
        this.clearData();
    }
}
