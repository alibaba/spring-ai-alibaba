import { request } from '@/request';
import {
  ITool,
  IToolListParams,
  IToolListResponse,
  ICreateToolRequest,
  IUpdateToolRequest,
  IToolResponse,
} from '@/types/tool';

/**
 * Tool service class
 */
export class ToolService {
  /**
   * Create a new tool
   * @param data Tool data
   * @returns Created tool
   */
  static async createTool(data: ICreateToolRequest): Promise<IToolResponse> {
    return request<IToolResponse>('/console/v1/tools', {
      method: 'POST',
      data,
    });
  }

  /**
   * Update an existing tool
   * @param id Tool ID
   * @param data Updated tool data
   * @returns Updated tool
   */
  static async updateTool(id: number, data: IUpdateToolRequest): Promise<IToolResponse> {
    return request<IToolResponse>(`/console/v1/tools/${id}`, {
      method: 'PUT',
      data,
    });
  }

  /**
   * Delete a tool
   * @param id Tool ID
   */
  static async deleteTool(id: number): Promise<void> {
    return request<void>(`/console/v1/tools/${id}`, {
      method: 'DELETE',
    });
  }

  /**
   * Get tool by ID
   * @param id Tool ID
   * @returns Tool details
   */
  static async getTool(id: number): Promise<IToolResponse> {
    return request<IToolResponse>(`/console/v1/tools/${id}`);
  }

  /**
   * Get all tools for current workspace
   * @returns List of tools
   */
  static async getTools(): Promise<ITool[]> {
    return request<ITool[]>('/console/v1/tools');
  }

  /**
   * Get tools with pagination
   * @param params Query parameters
   * @returns Paginated list of tools
   */
  static async getToolsByPage(params: IToolListParams): Promise<IToolListResponse> {
    return request<IToolListResponse>('/console/v1/tools/page', {
      method: 'GET',
      params,
    });
  }

  /**
   * Search tools by name
   * @param name Search term
   * @returns List of matching tools
   */
  static async searchTools(name: string): Promise<ITool[]> {
    return request<ITool[]>('/console/v1/tools/search', {
      method: 'GET',
      params: { name },
    });
  }

  /**
   * Get tools by plugin ID
   * @param pluginId Plugin ID
   * @returns List of tools for the plugin
   */
  static async getToolsByPlugin(pluginId: string): Promise<ITool[]> {
    return request<ITool[]>(`/console/v1/tools/plugin/${pluginId}`);
  }

  /**
   * Enable or disable a tool
   * @param id Tool ID
   * @param enabled Enable status
   */
  static async setToolEnabled(id: number, enabled: boolean): Promise<void> {
    return request<void>(`/console/v1/tools/${id}/enabled`, {
      method: 'PATCH',
      params: { enabled },
    });
  }
}