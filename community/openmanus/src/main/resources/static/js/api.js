/**
 * API 模块 - 处理与后端的所有通信
 */
const ManusAPI = (() => {
    // API 基础URL
    const BASE_URL = '/api/manus';

    /**
     * 向 Manus 发送消息，获取异步处理结果
     * @param {string} query - 用户输入的查询内容
     * @returns {Promise<Object>} - 包含任务 ID 和初始状态的响应
     */
    const sendMessage = async (query) => {
        try {
            const response = await fetch(`${BASE_URL}/execute`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ query })
            });

            if (!response.ok) {
                throw new Error(`API请求失败: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('发送消息失败:', error);
            throw error;
        }
    };

    /**
     * 获取详细的执行记录
     * @param {string} planId - 计划ID
     * @returns {Promise<Object>} - 包含详细执行记录的响应，如果404则返回null
     */
    const getDetails = async (planId) => {
        try {
            const response = await fetch(`${BASE_URL}/details/${planId}`);

            if (response.status === 404) {
                // 静默处理404错误，返回null
                console.log(`Plan ${planId} 不存在或已被删除，忽略此次查询`);
                return null;
            }
            
            if (!response.ok) {
                throw new Error(`获取详细信息失败: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            // 记录错误但不抛出异常
            console.log('获取详细信息失败:', error.message);
            return null;
        }
    };

    // 返回公开的方法
    return {
        sendMessage,
        getDetails
    };
})();
