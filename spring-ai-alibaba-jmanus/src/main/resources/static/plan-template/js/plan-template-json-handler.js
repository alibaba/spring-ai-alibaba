/**
 * 计划模板JSON处理器类
 * 负责处理JSON编辑、版本控制、模板保存等功能
 */
class PlanTemplateJsonHandler {
    constructor() {
        // 版本控制相关变量
        this.planVersions = []; // 存储所有版本的计划JSON
        this.currentVersionIndex = -1; // 当前版本索引

        // DOM 元素引用
        this.jsonEditor = null;
        this.modifyPlanBtn = null;

        // 外部依赖的实例引用
        this.planTemplateManager = null;
        this.planPromptGenerator = null;
    }

    /**
     * 初始化JSON处理器
     * @param {Object} planTemplateManager - PlanTemplateManagerOld 实例的引用
     * @param {Object} planPromptGenerator - PlanPromptGenerator 实例的引用
     */
    init(planTemplateManager, planPromptGenerator) {
        this.planTemplateManager = planTemplateManager;
        this.planPromptGenerator = planPromptGenerator;

        // 获取DOM元素
        this.jsonEditor = document.getElementById('plan-json-editor');
        this.modifyPlanBtn = document.getElementById('modifyPlanBtn');

        // 绑定事件监听器
        this.bindEventListeners();

        // 初始化UI状态
        this.updateUIState();

        console.log('PlanTemplateJsonHandler 初始化完成');
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
        document.getElementById('compareJsonBtn').addEventListener('click', this.handleCompareJson.bind(this));
    }

    /**
     * 修改计划（保存当前编辑器中的JSON到服务器）
     */
    async handleModifyPlan() {
        const currentTemplateId = this.getCurrentPlanTemplateId();
        if (!currentTemplateId) {
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
        
        const isGenerating = this.planPromptGenerator ? this.planPromptGenerator.getIsGenerating() : false;
        if (isGenerating) return;

        this.updateUIState();

        try {
            // 调用保存接口，注意区分是保存模板还是执行后的计划（这里是模板）
            await ManusAPI.savePlanTemplate(currentTemplateId, jsonContent);

            this.saveToVersionHistory(jsonContent); // 保存到本地版本历史
            alert('计划修改已保存成功！');
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
        const isGenerating = this.planPromptGenerator ? this.planPromptGenerator.getIsGenerating() : false;
        const isExecuting = this.planTemplateManager ? this.planTemplateManager.getMainIsExecuting() : false;
        const currentTemplateId = this.getCurrentPlanTemplateId();
        
        if (this.modifyPlanBtn) {
            this.modifyPlanBtn.disabled = isGenerating || isExecuting || !currentTemplateId;
        }

        // 版本控制按钮状态
        const rollbackBtn = document.getElementById('rollbackJsonBtn');
        const restoreBtn = document.getElementById('restoreJsonBtn');
        if (rollbackBtn) rollbackBtn.disabled = this.planVersions.length <= 1 || this.currentVersionIndex <= 0;
        if (restoreBtn) restoreBtn.disabled = this.planVersions.length <= 1 || this.currentVersionIndex >= this.planVersions.length - 1;
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
        const previousJson = this.planVersions[this.currentVersionIndex - 1 < 0 ? 0 : this.currentVersionIndex - 1]; //防止-1

        // 此处可以使用更复杂的差异比较库，例如 diff2html 或 jsdiff
        // 为了简单起见，这里只在控制台打印
        console.log("当前版本:", currentJson);
        console.log("上一个版本:", previousJson);
        alert('版本比较结果已打印到控制台。请使用更专业的工具查看详细差异。');
    }

    /**
     * 清空JSON相关的数据
     */
    clearJsonData() {
        if (this.jsonEditor) {
            this.jsonEditor.value = '';
        }
        this.planVersions = [];
        this.currentVersionIndex = -1;
        this.updateUIState();
        console.log('JSON数据已清空');
    }

    /**
     * 设置JSON编辑器内容
     * @param {string} jsonContent - JSON内容
     */
    setJsonContent(jsonContent) {
        if (this.jsonEditor && jsonContent) {
            this.jsonEditor.value = jsonContent;
            this.saveToVersionHistory(jsonContent);
        }
    }

    /**
     * 获取JSON编辑器内容
     * @returns {string} JSON内容
     */
    getJsonContent() {
        return this.jsonEditor ? this.jsonEditor.value.trim() : '';
    }

    /**
     * 获取当前计划模板ID
     * @returns {string|null} 当前计划模板ID
     */
    getCurrentPlanTemplateId() {
        return this.planPromptGenerator ? this.planPromptGenerator.getCurrentPlanTemplateId() : null;
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
}
