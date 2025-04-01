/**
 * admin-ui.js - 管理界面UI交互
 */
class AdminUI {
    constructor() {
        // Agent列表容器
        this.agentListContainer = document.querySelector('.agent-list-container');
        // Agent详情表单元素
        this.agentDetailForm = {
            name: document.getElementById('agent-detail-name'),
            description: document.getElementById('agent-detail-desc'),
            systemPrompt: document.getElementById('agent-think-prompt'),
            nextStepPrompt: document.getElementById('agent-next-prompt'),
            toolList: document.querySelector('.tool-list')
        };
    }

    /**
     * 初始化UI
     */
    async init() {
        await this.loadAgents();
        await this.loadAvailableTools();
        this.setupToolListeners();
    }

    /**
     * 加载并渲染Agent列表
     */
    async loadAgents() {
        try {
            const agents = await agentConfigModel.loadAgents();
            this.renderAgentList(agents);
        } catch (error) {
            this.showError('加载Agent列表失败');
        }
    }

    /**
     * 渲染Agent列表
     */
    renderAgentList(agents) {
        this.agentListContainer.innerHTML = agents.map(agent => this.createAgentListItem(agent)).join('');
    }

    /**
     * 创建Agent列表项HTML
     */
    createAgentListItem(agent) {
        return `
            <div class="agent-item" data-agent-id="${agent.id}">
                <div class="agent-item-header">
                    <span class="agent-name">${agent.name}</span>
                    <button class="expand-btn"><span class="icon-play"></span></button>
                </div>
                <div class="agent-item-content">
                    <div class="agent-desc">${agent.description}</div>
                    <div class="agent-tools">
                        ${agent.availableTools.map(tool => `
                            <span class="tool-tag">${tool}</span>
                        `).join('')}
                    </div>
                </div>
            </div>
        `;
    }

    /**
     * 显示Agent详情
     */
    showAgentDetails(agent) {
        this.agentDetailForm.name.value = agent.name || '';
        this.agentDetailForm.description.value = agent.description || '';
        this.agentDetailForm.systemPrompt.value = agent.systemPrompt || '';
        this.agentDetailForm.nextStepPrompt.value = agent.nextStepPrompt || '';
        this.renderToolList(agent.availableTools || []);
    }

    /**
     * 渲染工具列表
     */
    renderToolList(tools) {
        this.agentDetailForm.toolList.innerHTML = tools.map(tool => `
            <div class="tool-item">
                <span class="tool-name">${tool}</span>
                <button class="delete-tool-btn" data-tool="${tool}">×</button>
            </div>
        `).join('');
    }

    /**
     * 清空Agent详情表单
     */
    clearAgentDetails() {
        this.agentDetailForm.name.value = '';
        this.agentDetailForm.description.value = '';
        this.agentDetailForm.systemPrompt.value = '';
        this.agentDetailForm.nextStepPrompt.value = '';
        this.agentDetailForm.toolList.innerHTML = '';
    }

    /**
     * 收集表单数据
     */
    collectFormData() {
        const tools = Array.from(this.agentDetailForm.toolList.querySelectorAll('.tool-item'))
            .map(item => item.querySelector('.tool-name').textContent);

        return {
            name: this.agentDetailForm.name.value,
            description: this.agentDetailForm.description.value,
            systemPrompt: this.agentDetailForm.systemPrompt.value,
            nextStepPrompt: this.agentDetailForm.nextStepPrompt.value,
            availableTools: tools
        };
    }

    /**
     * 加载可用工具列表
     */
    async loadAvailableTools() {
        try {
            await agentConfigModel.loadAvailableTools();
        } catch (error) {
            this.showError('加载工具列表失败');
        }
    }

    /**
     * 显示错误消息
     */
    showError(message) {
        // 可以使用更好的提示UI组件
        alert(message);
    }

    /**
     * 显示成功消息
     */
    showSuccess(message) {
        // 可以使用更好的提示UI组件
        alert(message);
    }
}

// 创建全局UI实例
window.adminUI = new AdminUI();
