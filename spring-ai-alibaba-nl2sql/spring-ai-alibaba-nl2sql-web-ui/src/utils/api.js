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
    
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    }

    // 直接返回数据，不需要检查success字段
    const result = await response.json()
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

export default {
  get,
  post,
  put,
  del,
  agentApi,
  businessKnowledgeApi,
  semanticModelApi
}
