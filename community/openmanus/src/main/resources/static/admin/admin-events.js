/**
 * admin-events.js - 管理界面事件处理
 */
class AdminEvents {
    constructor() {
        // Agent列表相关元素
        this.agentListContainer = document.querySelector('.agent-list-container');
        this.addAgentBtn = document.querySelector('.add-agent-btn');
        
        // Agent详情相关元素
        this.saveAgentBtn = document.querySelector('.save-agent-btn');
        this.addToolBtn = document.querySelector('.add-tool-btn');
        
        this.currentAgentId = null;
        this.bindEvents();
    }

    /**
     * 绑定所有事件监听器
     */
    bindEvents() {
        // Agent列表事件
        this.agentListContainer.addEventListener('click', (e) => this.handleAgentListClick(e));
        this.addAgentBtn.addEventListener('click', () => this.handleAddAgent());

        // Agent详情事件
        this.saveAgentBtn.addEventListener('click', () => this.handleSaveAgent());
        this.addToolBtn.addEventListener('click', () => this.handleAddTool());

        // 工具列表事件委托
        document.querySelector('.tool-list').addEventListener('click', (e) => this.handleToolListClick(e));
    }

    /**
     * 处理Agent列表点击事件
     */
    async handleAgentListClick(event) {
        const agentItem = event.target.closest('.agent-item');
        if (!agentItem) return;

        const agentId = agentItem.dataset.agentId;
        if (event.target.closest('.expand-btn')) {
            // 展开/折叠Agent详情
            agentItem.classList.toggle('expanded');
        } else {
            // 加载Agent详情
            try {
                const agent = await agentConfigModel.loadAgentDetails(agentId);
                this.currentAgentId = agentId;
                adminUI.showAgentDetails(agent);
            } catch (error) {
                adminUI.showError('加载Agent详情失败');
            }
        }
    }

    /**
     * 处理添加新Agent
     */
    handleAddAgent() {
        this.currentAgentId = null;
        adminUI.clearAgentDetails();
    }

    /**
     * 处理保存Agent
     */
    async handleSaveAgent() {
        try {
            const formData = adminUI.collectFormData();
            if (this.currentAgentId) {
                formData.id = this.currentAgentId;
            }

            const savedAgent = await agentConfigModel.saveAgent(formData);
            this.currentAgentId = savedAgent.id;
            
            // 重新加载Agent列表
            await adminUI.loadAgents();
            adminUI.showSuccess('Agent保存成功');
        } catch (error) {
            adminUI.showError('保存Agent失败');
        }
    }

    /**
     * 处理添加工具
     */
    handleAddTool() {
        const availableTools = agentConfigModel.availableTools;
        // 这里应该显示一个工具选择对话框
        // 简化处理，直接显示第一个可用工具
        if (availableTools.length > 0) {
            const currentTools = Array.from(document.querySelectorAll('.tool-item'))
                .map(item => item.querySelector('.tool-name').textContent);
            
            const newTools = availableTools.filter(tool => !currentTools.includes(tool.key));
            
            if (newTools.length > 0) {
                const toolList = document.querySelector('.tool-list');
                const toolHtml = `
                    <div class="tool-item">
                        <span class="tool-name">${newTools[0].key}</span>
                        <button class="delete-tool-btn" data-tool="${newTools[0].key}">×</button>
                    </div>
                `;
                toolList.insertAdjacentHTML('beforeend', toolHtml);
            } else {
                adminUI.showError('没有更多可用的工具');
            }
        }
    }

    /**
     * 处理工具列表点击事件
     */
    handleToolListClick(event) {
        if (event.target.classList.contains('delete-tool-btn')) {
            const toolItem = event.target.closest('.tool-item');
            if (toolItem) {
                toolItem.remove();
            }
        }
    }
}

// 等待DOM加载完成后初始化
document.addEventListener('DOMContentLoaded', () => {
    window.adminEvents = new AdminEvents();
    window.adminUI.init();
});
