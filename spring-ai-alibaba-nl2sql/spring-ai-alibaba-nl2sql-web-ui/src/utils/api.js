/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * API 调用工具类
 * 统一处理 API 请求和响应
 */

// 这个是后端服务接口地址
const API_BASE_URL = 'http://localhost:8065/api'

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
      // 对于错误响应，尝试解析JSON错误信息
      let errorData = null
      try {
        errorData = await response.json()
      } catch (e) {
        // 如果无法解析JSON，使用默认错误信息
        errorData = { 
          message: response.statusText,
          status: response.status,
          url: `${API_BASE_URL}${url}`
        }
      }
      
      const error = new Error(`HTTP ${response.status}: ${response.statusText}`)
      error.response = {
        status: response.status,
        statusText: response.statusText,
        data: errorData
      }
      throw error
    }

    // 获取响应数据
    const contentType = response.headers.get('content-type')
    if (contentType && contentType.includes('application/json')) {
      // 如果响应包含JSON数据，则解析
      const text = await response.text()
      return text ? JSON.parse(text) : null
    } else {
      // 对于非JSON响应或空响应，返回null
      return null
    }
  } catch (error) {
    console.error('API 请求失败:', error)
    // 提供更详细的错误信息
    if (!error.response) {
      error.message = `网络错误或服务器无响应: ${error.message}`
    }
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

// 预设问题相关API
export const presetQuestionApi = {
  // 获取智能体的预设问题
  getByAgentId: (agentId) => get(`/agent/${agentId}/preset-questions`),
  // 批量保存预设问题
  batchSave: (agentId, questions) => post(`/agent/${agentId}/preset-questions`, questions),
  // 删除预设问题
  delete: (agentId, questionId) => del(`/agent/${agentId}/preset-questions/${questionId}`)
}

/**
 * 智能体相关 API
 */
export const agentApi = {
  // 获取智能体列表
  getList(params) {
    return get('/agent/list', params)
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
    return get('/knowledge', params)
  },

  // 根据数据集ID获取业务知识
  getByDatasetId(datasetId) {
    return get(`/knowledge/dataset/${datasetId}`)
  },

  // 根据智能体ID获取业务知识
  getByAgentId(agentId) {
    return get(`/knowledge/agent/${agentId}`)
  },

  // 搜索业务知识
  search(keyword) {
    return get('/knowledge/search', { content: keyword })

  },

  // 在智能体范围内搜索业务知识
  searchInAgent(agentId, keyword) {
    return get(`/knowledge/agent/${agentId}/search`, { content: keyword })

  },

  // 创建业务知识
  create(data) {
    return post('/knowledge/add', data)
  },

  // 批量创建业务知识
  createList(dataList) {
    return post('/knowledge/addList', dataList)
  },

  // 为智能体创建业务知识
  createForAgent(agentId, data) {
    return post(`/knowledge/agent/${agentId}/add`, data)
  },

  // 为智能体批量创建业务知识
  createListForAgent(agentId, dataList) {
    return post(`/knowledge/agent/${agentId}/addList`, dataList)
  },

  // 获取业务知识详情
  getDetail(id) {
    return get(`/knowledge/${id}`)
  },

  // 更新业务知识
  update(id, data) {
    return put(`/knowledge/update/${id}`, data)
  },

  // 删除业务知识
  delete(id) {
    return del(`/knowledge/delete/${id}`)
  },

  // 删除智能体的所有业务知识
  deleteByAgentId(agentId) {
    return del(`/knowledge/agent/${agentId}`)
  },

  // 获取数据集ID列表
  getDatasetIds() {
    return get('/knowledge/datasetIds')
  }
}

/**
 * 语义模型相关 API
 */
export const semanticModelApi = {
  // 获取语义模型列表
  getList(params) {
    // 处理params可能为空的情况
    if (params && params.agentId) {
      // 如果有agentId参数，使用agent端点
      return get(`/fields/agent/${params.agentId}`)
    } else if (params && params.datasetId) {
      // 如果有datasetId参数，使用dataset端点
      return get(`/fields/dataset/${params.datasetId}`)
    } else if (params && params.keyword) {
      // 如果有keyword参数，使用搜索端点
      return get('/fields/search', { content: params.keyword })
    } else {
      // 默认获取所有数据集ID，然后获取第一个数据集的数据（这里需要根据实际需求调整）
      return get('/fields/datasetIds').then(response => {
        if (response && response.length > 0) {
          return get(`/fields/dataset/${response[0]}`)
        }
        return []
      })
    }
  },

  // 搜索语义模型
  search(keyword) {
    return get('/fields/search', { content: keyword })
  },

  // 根据数据集ID获取语义模型
  getByDatasetId(datasetId) {
    return get(`/fields/dataset/${datasetId}`)
  },

  // 根据智能体ID获取语义模型
  getByAgentId(agentId) {
    return get(`/fields/agent/${agentId}`)
  },

  // 创建语义模型
  create(data) {
    // 转换数据格式以匹配SemanticModelDTO
    const dto = {
      agentId: data.agentId,
      datasetId: data.datasetId,
      originalFieldName: data.originalFieldName,
      agentFieldName: data.agentFieldName,
      fieldSynonyms: data.fieldSynonyms,
      fieldDescription: data.fieldDescription,
      originalDescription: data.originalDescription,
      fieldType: data.fieldType,
      defaultRecall: data.defaultRecall,
      enabled: data.enabled
    }
    return post('/fields/add', dto)
  },

  // 获取语义模型详情
  getDetail(id) {
    // 注意：SemanticModelPersistenceController没有单独的详情接口
    // 可能需要通过搜索或其他方式获取
    return Promise.resolve(null)
  },

  // 更新语义模型
  update(id, data) {
    // 转换数据格式以匹配SemanticModelDTO
    const dto = {
      agentId: data.agentId,
      datasetId: data.datasetId,
      originalFieldName: data.originalFieldName,
      agentFieldName: data.agentFieldName,
      fieldSynonyms: data.fieldSynonyms,
      fieldDescription: data.fieldDescription,
      originalDescription: data.originalDescription,
      fieldType: data.fieldType,
      defaultRecall: data.defaultRecall,
      enabled: data.enabled
    }
    return put(`/fields/${id}`, dto)
  },

  // 删除语义模型
  delete(id) {
    return del(`/fields/${id}`)
  },

  // 按数据集批量启用/禁用
  batchEnable(datasetId, enabled) {
    if (enabled) {
      // 这里需要获取datasetId下的所有字段ID，然后批量启用
      // 由于接口限制，暂时返回一个简单的实现
      return Promise.resolve()
    } else {
      return Promise.resolve()
    }
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
  },

  // 获取数据源的表列表
  getTables(datasourceId) {
    return get(`/datasource/${datasourceId}/tables`)
  }
}

/**
 * 智能体调试相关 API
 */
export const agentDebugApi = {
  // 创建流式调试连接
  createDebugStream(agentId, query) {
    const encodedQuery = encodeURIComponent(query)
    return new EventSource(`${API_BASE_URL}/agent/${agentId}/debug/stream?query=${encodedQuery}`)
  },

  // 获取调试历史记录
  getDebugHistory(agentId, params = {}) {
    return get(`/agent/${agentId}/debug/history`, params)
  },

  // 清除调试历史记录
  clearDebugHistory(agentId) {
    return del(`/agent/${agentId}/debug/history`)
  },

  // 获取调试统计信息
  getDebugStatistics(agentId) {
    return get(`/agent/${agentId}/debug/statistics`)
  },

  // 保存调试会话
  saveDebugSession(agentId, sessionData) {
    return post(`/agent/${agentId}/debug/session`, sessionData)
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
  datasourceApi,
  agentDebugApi
}
