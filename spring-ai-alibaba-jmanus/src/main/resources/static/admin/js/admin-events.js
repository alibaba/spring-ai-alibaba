/**
 * admin-events.js - 管理界面事件处理
 */
import { AdminUtils } from './admin-utils.js';

class AdminEvents {
    constructor() {
        // Agent列表相关元素
        this.agentListContainer = document.querySelector('.agent-list-container');
        this.addAgentBtn = document.querySelector('.add-agent-btn');
        
        // Agent详情相关元素
        this.saveAgentBtn = document.querySelector('.save-agent-btn');
        this.addToolBtn = document.querySelector('.add-tool-btn');
        this.deleteAgentBtn = document.querySelector('.delete-agent-btn');
        
        // 添加导入导出按钮
        this.importAgentsBtn = document.querySelector('.import-agents-btn');
        this.exportAgentsBtn = document.querySelector('.export-agents-btn');
        
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
        this.deleteAgentBtn.addEventListener('click', () => this.handleDeleteAgent());

        // 导入导出事件
        this.importAgentsBtn.addEventListener('click', () => this.handleImportAgents());
        this.exportAgentsBtn.addEventListener('click', () => this.handleExportAgents());

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
        // currentTools are the keys/names of tools currently displayed in the agent's tool list
        const currentTools = Array.from(document.querySelectorAll('.tool-list .tool-item .tool-name'))
            .map(item => item.textContent);
            
        if (availableTools && availableTools.length > 0) {
            // The onSelect callback now receives the complete list of tools that should be active.
            adminUI.showToolSelectionDialog(availableTools, currentTools, (finalSelectedToolsArray) => {
                const toolList = document.querySelector('.tool-list');
                toolList.innerHTML = ''; // Clear the current list

                finalSelectedToolsArray.forEach(selectedTool => {
                    // Assuming selectedTool is an object and has a 'key' property for its name/identifier.
                    // Adjust 'selectedTool.key' if the actual property name is different (e.g., name, id).
                    const toolName = selectedTool.key || selectedTool.name; // Use key, fallback to name
                    if (toolName) {
                        const toolHtml = `
                            <div class="tool-item">
                                <span class="tool-name">${toolName}</span>
                            </div>
                        `;
                        toolList.insertAdjacentHTML('beforeend', toolHtml);
                    }
                });
            });
        } else {
            adminUI.showError('工具列表未加载或为空');
        }
    }


    /**
     * 处理删除Agent
     */
    async handleDeleteAgent() {
        if (!this.currentAgentId) {
            adminUI.showError('未选择任何Agent');
            return;
        }

        const confirmed = await AdminUtils.confirmDialog('确定要删除该Agent吗？');
        if (!confirmed) return;

        try {
            await agentConfigModel.deleteAgent(this.currentAgentId);
            this.currentAgentId = null;
            await adminUI.loadAgents();
            adminUI.clearAgentDetails();
            adminUI.showSuccess('Agent删除成功');
        } catch (error) {
            adminUI.showError('删除Agent失败');
        }
    }

    /**
     * 处理导入Agents配置
     */
    handleImportAgents() {
        // 创建隐藏的文件输入框
        const fileInput = document.createElement('input');
        fileInput.type = 'file';
        fileInput.accept = '.json';
        fileInput.style.display = 'none';
        document.body.appendChild(fileInput);

        fileInput.addEventListener('change', async (e) => {
            const file = e.target.files[0];
            if (!file) return;

            try {
                const reader = new FileReader();
                reader.onload = async (event) => {
                    try {
                        const agentConfigs = JSON.parse(event.target.result);
                        for (const config of agentConfigs) {
                            await agentConfigModel.saveAgent(config,true);
                        }
                        await adminUI.loadAgents();
                        adminUI.showSuccess('成功导入Agent配置');
                    } catch (error) {
                        adminUI.showError('导入失败：无效的配置文件格式');
                    }
                };
                reader.readAsText(file);
            } catch (error) {
                adminUI.showError('导入失败');
            } finally {
                document.body.removeChild(fileInput);
            }
        });

        fileInput.click();
    }

    /**
     * 处理导出Agents配置
     */
    async handleExportAgents() {
        try {
            const agents = await agentConfigModel.loadAgents();
            const exportData = JSON.stringify(agents, null, 2);
            
            // 创建下载链接
            const blob = new Blob([exportData], { type: 'application/json' });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `agents-config-${new Date().toISOString().split('T')[0]}.json`;
            document.body.appendChild(a);
            a.click();
            
            // 清理
            setTimeout(() => {
                document.body.removeChild(a);
                URL.revokeObjectURL(url);
            }, 100);
            
            adminUI.showSuccess('成功导出Agent配置');
        } catch (error) {
            adminUI.showError('导出失败');
        }
    }
}

// 等待DOM加载完成后初始化
document.addEventListener('DOMContentLoaded', () => {
    window.adminEvents = new AdminEvents();
    window.adminUI.init();
});
