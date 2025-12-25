import { request } from '../../utils/request';
import { API_PATH } from '../const';

// prompt 列表查询
export async function getPrompts(params: PromptAPI.GetPromptsParams) {
  return request<PromptAPI.GetPromptsResult>(`${API_PATH}/prompts`, {
    method: 'GET',
    params,
  });
}

// 单个 prompt 查询
export async function getPrompt(params: { promptKey: string }) {
  return request<PromptAPI.GetPromptResult>(`${API_PATH}/prompt`, {
    method: 'GET',
    params,
  });
}

// prompt 发布
export async function publishPrompt(params: PromptAPI.PublishPromptParams) {
  return request<PromptAPI.PublishPromptResult>(`${API_PATH}/prompt`, {
    method: 'POST',
    data: params,
  });
}

// prompt 更新
export async function updatePrompt(params: PromptAPI.UpdatePromptParams) {
  return request<PromptAPI.UpdatePromptResult>(`${API_PATH}/prompt`, {
    method: 'PUT',
    data: params,
  });
}

// prompt 删除
export async function deletePrompt(params: PromptAPI.DeletePromptParams) {
  return request<boolean>(`${API_PATH}/prompt`, {
    method: 'DELETE',
    params,
  });
}

// prompt 版本列表
// api/prompt/versions, 按发布时间倒序
export async function getPromptVersions(params: PromptAPI.GetPromptVersionsParams) {
  return request<PromptAPI.GetPromptVersionsResult>(`${API_PATH}/prompt/versions`, {
    method: 'GET',
    params,
  });
}


// prompt 版本查询
export async function getPromptVersion(params: PromptAPI.GetPromptVersionParams) {
  return request<PromptAPI.GetPromptVersionResult>(`${API_PATH}/prompt/version`, {
    method: 'GET',
    params,
  });
}

// prompt 版本发布
export async function publishPromptVersion(params: PromptAPI.PublishPromptVersionParams) {
  return request<PromptAPI.PublishPromptVersionResult>(`${API_PATH}/prompt/version`, {
    method: 'POST',
    data: params,
  });
}

// 交互式Prompt调试
// 支持持续交互的Prompt调试，可以进行单次调试或多轮对话。接口返回结构化流式结果，支持会话管理。
// POST /api/prompt/run
// 请求头： 
//        Content-Type: application/json
//        Accept: application/x-ndjson
export async function runPrompt(params: PromptAPI.RunPromptParams) {
  return request<PromptAPI.RunPromptResult>(`${API_PATH}/prompt/run`, {
    method: 'POST',
    data: params,
  });
}

// 获取会话详情
// GET /api/prompt/session/{sessionId}
export async function getPromptSession(sessionId: string) {
  return request<PromptAPI.GetPromptSessionResult>(`${API_PATH}/prompt/session`, {
    method: 'GET',
    params: {
      sessionId,
    },
  });
}

// 删除会话
// DELETE /api/prompt/session/{sessionId}
export async function deletePromptSession(sessionId: string) {
  return request<PromptAPI.DeletePromptSessionResult>(`${API_PATH}/prompt/session`, {
    method: 'DELETE',
    params: {
      sessionId,
    },
  });
}


// prompt 模板列表
export async function getPromptTemplates(params: PromptAPI.GetPromptTemplatesParams) {
  return request<PromptAPI.GetPromptTemplatesResult>(`${API_PATH}/prompt/templates`, {
    method: 'GET',
    params,
  });
}

// prompt 模板详情
export async function getPromptTemplate(params: { promptTemplateKey: string }) {
  return request<PromptAPI.GetPromptTemplateResult>(`${API_PATH}/prompt/template`, {
    method: 'GET',
    params,
  });
}

// 获取模型配置列表
// 获取模型列表，已替换废弃的 getModelList 接口
// 返回分页数据格式，支持搜索和过滤功能
export async function getModels(params?: PromptAPI.GetModelsParams) {
  // Use new ModelService API for enabled models
  const { getEnabledModels } = await import('@/services/modelService');
  try {
    const response = await getEnabledModels();
    if (response?.data) {
      // Convert to legacy format
      return {
        code: 200,
        data: {
          totalCount: response.data.length,
          totalPage: 1,
          pageNumber: 1,
          pageSize: response.data.length,
          pageItems: response.data,
        },
      } as PromptAPI.GetModelsResult;
    }
  } catch (error) {
    console.error('Failed to get enabled models from ModelService, falling back to legacy API:', error);
  }
  
  // Fallback to legacy API
  return request<PromptAPI.GetModelsResult>(`${API_PATH}/models`, {
    method: 'GET',
    params,
  }); 
}