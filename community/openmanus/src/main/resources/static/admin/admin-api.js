/**
 * admin-api.js - 管理界面API请求处理模块
 * 负责处理所有与后端API的交互
 */

class AdminAPI {
    /**
     * 基础API路径
     * @type {string}
     */
    static BASE_URL = '/api/config';

    /**
     * 处理HTTP错误
     * @param {Response} response - 响应对象
     * @returns {Promise} - 如果响应成功则返回响应本身，否则抛出错误
     */
    static async _handleResponse(response) {
        if (!response.ok) {
            // 尝试解析错误消息
            try {
                const errorData = await response.json();
                throw new Error(errorData.message || `API请求失败: ${response.status}`);
            } catch (e) {
                // 如果无法解析JSON，则使用状态文本
                throw new Error(`API请求失败: ${response.status} ${response.statusText}`);
            }
        }
        return response;
    }

    /**
     * 根据配置组名获取配置项
     * @param {string} groupName - 配置组名，如 "manus"
     * @returns {Promise<Array>} - 配置项数组
     */
    static async getConfigsByGroup(groupName) {
        try {
            const response = await fetch(`${this.BASE_URL}/group/${groupName}`);
            const result = await this._handleResponse(response);
            return await result.json();
        } catch (error) {
            console.error(`获取${groupName}组配置失败:`, error);
            throw error;
        }
    }

    /**
     * 批量更新配置项
     * 注意：仅更新同一个配置组的配置
     * @param {Array} configs - 配置项数组，包含id和新的configValue
     * @param {string} groupName - 配置组名，用于验证所有配置是否属于同一组
     * @returns {Promise<Object>} - 保存结果
     */
    static async batchUpdateConfigs(configs, groupName) {
        if (!configs || configs.length === 0) {
            console.warn('没有需要更新的配置');
            return { success: true, message: '没有需要更新的配置' };
        }

        // 验证所有配置是否属于指定的组
        if (groupName) {
            const invalidConfigs = configs.filter(config => config.configGroup !== groupName);
            if (invalidConfigs.length > 0) {
                console.warn(`发现${invalidConfigs.length}个不属于${groupName}组的配置项`);
                // 可以决定是否过滤它们或直接返回错误
            }
        }

        try {
            const response = await fetch(`${this.BASE_URL}/batch-update`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(configs)
            });

            await this._handleResponse(response);
            return { success: true, message: '配置保存成功' };
        } catch (error) {
            console.error('批量更新配置失败:', error);
            return { success: false, message: error.message || '更新失败，请重试' };
        }
    }
}

// 导出 API 类，使其在其他文件中可用
window.AdminAPI = AdminAPI;
