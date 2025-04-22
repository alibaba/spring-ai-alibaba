/**
 * API 模块 - 处理与后端的所有通信
 */
const ManusAPI = (() => {
    // API 基础URL
    const BASE_URL = '/api/executor';
    const PLAN_TEMPLATE_URL = '/api/plan-template';

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
     * 根据输入生成执行计划
     * @param {string} query - 用户输入的计划需求
     * @param {string} [existingJson] - 可选的已有JSON数据字符串
     * @returns {Promise<Object>} - 包含完整计划数据的响应
     */
    const generatePlan = async (query, existingJson = null) => {
        try {
            const requestBody = { query };
            
            // 如果存在已有的JSON数据，添加到请求中
            if (existingJson) {
                requestBody.existingJson = existingJson;
            }
            
            const response = await fetch(`${PLAN_TEMPLATE_URL}/generate`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(requestBody)
            });

            if (!response.ok) {
                throw new Error(`生成计划失败: ${response.status}`);
            }

            const responseData = await response.json();
            
            // 如果响应中包含planJson字段，就将其解析为JSON对象
            if (responseData.planJson) {
                try {
                    responseData.plan = JSON.parse(responseData.planJson);
                } catch (e) {
                    console.warn('无法解析计划JSON:', e);
                    responseData.plan = { error: '无法解析计划数据' };
                }
            }

            return responseData;
        } catch (error) {
            console.error('生成计划失败:', error);
            throw error;
        }
    };
    
    /**
     * 执行已生成的计划
     * @param {string} planTemplateId - 计划模板ID
     * @param {string} [rawParam=null] - 可选的执行参数，字符串格式
     * @returns {Promise<Object>} - 包含执行状态的响应
     */
    const executePlan = async (planTemplateId, rawParam = null) => {
        try {
            let response;
            
            // 构建请求体对象
            const requestBody = { planTemplateId: planTemplateId };
            
            // 如果有原始参数，添加到请求体
            if (rawParam) {
                requestBody.rawParam = rawParam;
            }
            
            // 统一使用POST方法
            response = await fetch(`${PLAN_TEMPLATE_URL}/executePlanByTemplateId`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(requestBody)
            });

            if (!response.ok) {
                throw new Error(`执行计划失败: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('执行计划失败:', error);
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
    
    /**
     * 保存计划到服务器
     * @param {string} planId - 计划ID
     * @param {string} planJson - 计划JSON内容
     * @returns {Promise<Object>} - 保存结果
     */
    const savePlan = async (planId, planJson) => {
        try {
            const response = await fetch(`${PLAN_TEMPLATE_URL}/save`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    planId: planId,
                    planJson: planJson
                })
            });

            if (!response.ok) {
                throw new Error(`保存计划失败: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('保存计划失败:', error);
            throw error;
        }
    };
    
    /**
     * 获取计划的所有版本
     * @param {string} planId - 计划ID
     * @returns {Promise<Object>} - 包含版本历史的响应
     */
    const getPlanVersions = async (planId) => {
        try {
            const response = await fetch(`${PLAN_TEMPLATE_URL}/versions`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ planId })
            });

            if (!response.ok) {
                throw new Error(`获取计划版本失败: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('获取计划版本失败:', error);
            throw error;
        }
    };
    
    /**
     * 获取特定版本的计划
     * @param {string} planId - 计划ID
     * @param {number} versionIndex - 版本索引
     * @returns {Promise<Object>} - 包含特定版本计划的响应
     */
    const getVersionPlan = async (planId, versionIndex) => {
        try {
            const response = await fetch(`${PLAN_TEMPLATE_URL}/get-version`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    planId: planId,
                    versionIndex: versionIndex.toString()
                })
            });

            if (!response.ok) {
                throw new Error(`获取特定版本计划失败: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('获取特定版本计划失败:', error);
            throw error;
        }
    };
    
    /**
     * 获取所有计划模板列表
     * @returns {Promise<Object>} - 包含计划模板列表的响应
     */
    const getAllPlanTemplates = async () => {
        try {
            const url = `${PLAN_TEMPLATE_URL}/list`;
            console.log('正在请求计划模板列表，URL:', url);
            
            const response = await fetch(url);
            
            if (!response.ok) {
                throw new Error(`获取计划模板列表失败: ${response.status}`);
            }
            
            return await response.json();
        } catch (error) {
            console.error('获取计划模板列表失败:', error);
            throw error;
        }
    };

    /**
     * 更新现有计划模板
     * @param {string} planId - 计划模板ID
     * @param {string} query - 用户输入的计划需求
     * @param {string} existingJson - 已有的JSON数据字符串
     * @returns {Promise<Object>} - 包含更新后计划数据的响应
     */
    const updatePlanTemplate = async (planId, query, existingJson = null) => {
        try {
            const requestBody = { 
                planId,
                query
            };
            
            // 如果存在已有的JSON数据，添加到请求中
            if (existingJson) {
                requestBody.existingJson = existingJson;
            }
            
            const response = await fetch(`${PLAN_TEMPLATE_URL}/update`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(requestBody)
            });

            if (!response.ok) {
                throw new Error(`更新计划模板失败: ${response.status}`);
            }

            const responseData = await response.json();
            
            // 如果响应中包含planJson字段，就将其解析为JSON对象
            if (responseData.planJson) {
                try {
                    responseData.plan = JSON.parse(responseData.planJson);
                } catch (e) {
                    console.warn('无法解析计划JSON:', e);
                    responseData.plan = { error: '无法解析计划数据' };
                }
            }

            return responseData;
        } catch (error) {
            console.error('更新计划模板失败:', error);
            throw error;
        }
    };

    /**
     * 删除计划模板
     * @param {string} planId - 计划模板ID
     * @returns {Promise<Object>} - 删除结果
     */
    const deletePlanTemplate = async (planId) => {
        try {
            const response = await fetch(`${PLAN_TEMPLATE_URL}/delete`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ planId })
            });

            if (!response.ok) {
                throw new Error(`删除计划模板失败: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('删除计划模板失败:', error);
            throw error;
        }
    };

    // 返回公开的方法
    return {
        BASE_URL,
        PLAN_TEMPLATE_URL,
        sendMessage,
        getDetails,
        generatePlan,
        executePlan,
        savePlan,
        getPlanVersions,
        updatePlanTemplate,
        getVersionPlan,
        getAllPlanTemplates,
        deletePlanTemplate
    };
})();
