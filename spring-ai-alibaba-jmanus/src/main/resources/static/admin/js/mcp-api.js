/**
 * mcp-api.js - MCP配置API管理模块
 * 负责处理所有与MCP配置相关的API交互
 */

class McpAPI {
    /**
     * 基础API路径
     * @type {string}
     */
    static MCP_URL = '/api/mcp';

    /**
     * 获取所有MCP服务器配置
     * @returns {Promise<Array>} - MCP服务器配置数组
     */
    static async getAllMcpServers() {
        try {
            const response = await fetch(`${this.MCP_URL}/list`);
            if (!response.ok) {
                throw new Error(`获取MCP服务器列表失败: ${response.status}`);
            }
            return await response.json();
        } catch (error) {
            console.error('获取MCP服务器列表失败:', error);
            throw error;
        }
    }

    /**
     * 添加新的MCP服务器配置
     * @param {Object} mcpConfig - MCP服务器配置对象
     * @returns {Promise<Object>} - 添加结果
     */
    static async addMcpServer(mcpConfig) {
        try {
            const response = await fetch(`${this.MCP_URL}/add`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(mcpConfig)
            });
            if (!response.ok) {
                throw new Error(`添加MCP服务器失败: ${response.status}`);
            }
            return { success: true, message: '添加MCP服务器成功' };
        } catch (error) {
            console.error('添加MCP服务器失败:', error);
            return { success: false, message: error.message || '添加失败，请重试' };
        }
    }

    /**
     * 删除MCP服务器配置
     * @param {number} id - 要删除的MCP服务器ID
     * @returns {Promise<Object>} - 删除结果
     */
    static async removeMcpServer(id) {
        try {
            const response = await fetch(`${this.MCP_URL}/remove?id=${id}`);
            if (!response.ok) {
                throw new Error(`删除MCP服务器失败: ${response.status}`);
            }
            return { success: true, message: '删除MCP服务器成功' };
        } catch (error) {
            console.error(`删除MCP服务器[${id}]失败:`, error);
            return { success: false, message: error.message || '删除失败，请重试' };
        }
    }
}

// 导出 API 类，使其在其他文件中可用
window.McpAPI = McpAPI;
