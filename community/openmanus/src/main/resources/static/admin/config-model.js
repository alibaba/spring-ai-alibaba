/**
 * config-model.js - 配置数据模型管理
 */
class AgentConfigModel {
    constructor() {
        this.agents = [];
        this.currentAgent = null;
        this.availableTools = [];
    }

    async loadAgents() {
        try {
            this.agents = await AdminAPI.getAllAgents();
            return this.agents;
        } catch (error) {
            console.error('加载Agent列表失败:', error);
            throw error;
        }
    }

    async loadAgentDetails(id) {
        try {
            this.currentAgent = await AdminAPI.getAgentById(id);
            return this.currentAgent;
        } catch (error) {
            console.error('加载Agent详情失败:', error);
            throw error;
        }
    }

    async loadAvailableTools() {
        try {
            this.availableTools = await AdminAPI.getAvailableTools();
            return this.availableTools;
        } catch (error) {
            console.error('加载可用工具列表失败:', error);
            throw error;
        }
    }

    async saveAgent(agentData) {
        try {
            if (agentData.id) {
                return await AdminAPI.updateAgent(agentData.id, agentData);
            } else {
                return await AdminAPI.createAgent(agentData);
            }
        } catch (error) {
            console.error('保存Agent失败:', error);
            throw error;
        }
    }

    async deleteAgent(id) {
        try {
            await AdminAPI.deleteAgent(id);
            this.agents = this.agents.filter(agent => agent.id !== id);
        } catch (error) {
            console.error('删除Agent失败:', error);
            throw error;
        }
    }
}

// 创建全局配置模型实例
window.agentConfigModel = new AgentConfigModel();
