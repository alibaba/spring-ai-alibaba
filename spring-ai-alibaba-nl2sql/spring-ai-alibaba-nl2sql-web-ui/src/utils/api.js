/**
 * API 调用工具类
 * 统一处理 API 请求和响应
 */

const API_BASE_URL = '/api'

/**
 * 发送 HTTP 请求的通用方法
 * @param {string} url - 请求URL
 * @param {Object} options - 请求选项
 * @returns {Promise} - 返回处理后的响应数据
 */
async function request(url, options = {}) {
  const config = {
    headers: {
      'Content-Type': 'application/json',
      ...options.headers
    },
    ...options
  }

  try {
    const response = await fetch(`${API_BASE_URL}${url}`, config)
    
    // 获取响应数据
    const result = await response.json()
    
    if (!response.ok) {
      // 创建一个类似axios的错误对象结构
      const error = new Error(`HTTP ${response.status}: ${response.statusText}`)
      error.response = {
        status: response.status,
        statusText: response.statusText,
        data: result
      }
      throw error
    }

    // 直接返回数据，不需要检查success字段
    return result
  } catch (error) {
    console.error('API 请求失败:', error)
    throw error
  }
}

/**
 * GET 请求
 */
export function get(url, params = {}) {
  const queryString = new URLSearchParams(params).toString()
  const fullUrl = queryString ? `${url}?${queryString}` : url
  return request(fullUrl, { method: 'GET' })
}

/**
 * POST 请求
 */
export function post(url, data = {}) {
  return request(url, {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

/**
 * PUT 请求
 */
export function put(url, data = {}) {
  return request(url, {
    method: 'PUT',
    body: JSON.stringify(data)
  })
}

/**
 * DELETE 请求
 */
export function del(url) {
  return request(url, { method: 'DELETE' })
}

/**
 * 智能体相关 API
 */
export const agentApi = {
  // 获取智能体列表
  getList(params) {
    return get('/agent', params)
  },

  // 创建智能体
  create(data) {
    return post('/agent', data)
  },

  // 获取智能体详情
  getDetail(id) {
    return get(`/agent/${id}`)
  },

  // 更新智能体
  update(id, data) {
    return put(`/agent/${id}`, data)
  },

  // 删除智能体
  delete(id) {
    return del(`/agent/${id}`)
  },

  // 发布智能体
  publish(id) {
    return post(`/agent/${id}/publish`)
  },

  // 下线智能体
  offline(id) {
    return post(`/agent/${id}/offline`)
  }
}

/**
 * 业务知识相关 API
 */
export const businessKnowledgeApi = {
  // 获取业务知识列表
  getList(params) {
    return get('/business-knowledge', params)
  },

  // 创建业务知识
  create(data) {
    return post('/business-knowledge', data)
  },

  // 获取业务知识详情
  getDetail(id) {
    return get(`/business-knowledge/${id}`)
  },

  // 更新业务知识
  update(id, data) {
    return put(`/business-knowledge/${id}`, data)
  },

  // 删除业务知识
  delete(id) {
    return del(`/business-knowledge/${id}`)
  }
}

/**
 * 语义模型相关 API
 */
export const semanticModelApi = {
  // 获取语义模型列表
  getList(params) {
    return get('/semantic-model', params)
  },

  // 创建语义模型
  create(data) {
    return post('/semantic-model', data)
  },

  // 获取语义模型详情
  getDetail(id) {
    return get(`/semantic-model/${id}`)
  },

  // 更新语义模型
  update(id, data) {
    return put(`/semantic-model/${id}`, data)
  },

  // 删除语义模型
  delete(id) {
    return del(`/semantic-model/${id}`)
  }
}

/**
 * 智能体知识相关 API
 */
export const agentKnowledgeApi = {
  // 获取智能体知识列表
  getByAgentId(agentId, params = {}) {
    return get(`/agent-knowledge/agent/${agentId}`, params)
  },

  // 获取知识详情
  getDetail(id) {
    return get(`/agent-knowledge/${id}`)
  },

  // 创建知识
  create(data) {
    return post('/agent-knowledge', data)
  },

  // 更新知识
  update(id, data) {
    return put(`/agent-knowledge/${id}`, data)
  },

  // 删除知识
  delete(id) {
    return del(`/agent-knowledge/${id}`)
  },

  // 批量更新状态
  batchUpdateStatus(data) {
    return put('/agent-knowledge/batch/status', data)
  },

  // 获取统计信息
  getStatistics(agentId) {
    return get(`/agent-knowledge/statistics/${agentId}`)
  },

  // 搜索知识
  search(agentId, keyword) {
    return get(`/agent-knowledge/agent/${agentId}`, { keyword })
  },

  // 按类型筛选
  getByType(agentId, type) {
    return get(`/agent-knowledge/agent/${agentId}`, { type })
  },

  // 按状态筛选
  getByStatus(agentId, status) {
    return get(`/agent-knowledge/agent/${agentId}`, { status })
  }
}

/**
 * 数据源 API
 */
export const datasourceApi = {
  // 获取所有数据源列表
  getList(params = {}) {
    return get('/datasource', params)
  },

  // 根据ID获取数据源详情
  getDetail(id) {
    return get(`/datasource/${id}`)
  },

  // 创建数据源
  create(data) {
    return post('/datasource', data)
  },

  // 更新数据源
  update(id, data) {
    return put(`/datasource/${id}`, data)
  },

  // 删除数据源
  delete(id) {
    return del(`/datasource/${id}`)
  },

  // 测试数据源连接
  testConnection(id) {
    return post(`/datasource/${id}/test`)
  },

  // 获取数据源统计信息
  getStatistics() {
    return get('/datasource/stats')
  },

  // 获取智能体关联的数据源列表
  getAgentDatasources(agentId) {
    return get(`/datasource/agent/${agentId}`)
  },

  // 为智能体添加数据源
  addToAgent(agentId, datasourceId) {
    return post(`/datasource/agent/${agentId}`, { datasourceId })
  },

  // 移除智能体的数据源关联
  removeFromAgent(agentId, datasourceId) {
    return del(`/datasource/agent/${agentId}/${datasourceId}`)
  },

  // 启用/禁用智能体的数据源
  toggleDatasource(agentId, datasourceId, isActive) {
    return put(`/datasource/agent/${agentId}/${datasourceId}/toggle`, { isActive })
  }
}

export default {
  get,
  post,
  put,
  del,
  agentApi,
  businessKnowledgeApi,
  semanticModelApi,
  agentKnowledgeApi,
  datasourceApi
}
