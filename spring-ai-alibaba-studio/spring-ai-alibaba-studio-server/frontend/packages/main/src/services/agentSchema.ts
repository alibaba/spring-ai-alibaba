import { request } from '@/request';
import {
  IAgentSchema,
  IAgentSchemaListParams,
  IAgentSchemaListResponse,
  ICreateAgentSchemaRequest,
  IUpdateAgentSchemaRequest,
  IAgentSchemaResponse,
} from '@/types/agentSchema';

/**
 * Agent schema service class
 */
export class AgentSchemaService {
  /**
   * Create a new agent schema
   * @param data Agent schema data
   * @returns Created agent schema
   */
  static async createAgentSchema(data: ICreateAgentSchemaRequest): Promise<IAgentSchemaResponse> {
    return request<IAgentSchemaResponse>('/console/v1/agent-schemas', {
      method: 'POST',
      data,
    });
  }

  /**
   * Update an existing agent schema
   * @param id Agent schema ID
   * @param data Updated agent schema data
   * @returns Updated agent schema
   */
  static async updateAgentSchema(id: number, data: IUpdateAgentSchemaRequest): Promise<IAgentSchemaResponse> {
    return request<IAgentSchemaResponse>(`/console/v1/agent-schemas/${id}`, {
      method: 'PUT',
      data,
    });
  }

  /**
   * Delete an agent schema
   * @param id Agent schema ID
   */
  static async deleteAgentSchema(id: number): Promise<void> {
    return request<void>(`/console/v1/agent-schemas/${id}`, {
      method: 'DELETE',
    });
  }

  /**
   * Get agent schema by ID
   * @param id Agent schema ID
   * @returns Agent schema details
   */
  static async getAgentSchema(id: number): Promise<IAgentSchemaResponse> {
    return request<IAgentSchemaResponse>(`/console/v1/agent-schemas/${id}`);
  }

  /**
   * Get all agent schemas for current workspace
   * @returns List of agent schemas
   */
  static async getAgentSchemas(): Promise<IAgentSchema[]> {
    return request<IAgentSchema[]>('/console/v1/agent-schemas');
  }

  /**
   * Get agent schemas with pagination
   * @param params Query parameters
   * @returns Paginated list of agent schemas
   */
  static async getAgentSchemasByPage(params: IAgentSchemaListParams): Promise<IAgentSchemaListResponse> {
    return request<IAgentSchemaListResponse>('/console/v1/agent-schemas/page', {
      method: 'GET',
      params,
    });
  }

  /**
   * Search agent schemas by name
   * @param name Search term
   * @returns List of matching agent schemas
   */
  static async searchAgentSchemas(name: string): Promise<IAgentSchema[]> {
    return request<IAgentSchema[]>('/console/v1/agent-schemas/search', {
      method: 'GET',
      params: { name },
    });
  }

  /**
   * Enable or disable an agent schema
   * @param id Agent schema ID
   * @param enabled Enable status
   */
  static async setAgentSchemaEnabled(id: number, enabled: boolean): Promise<void> {
    return request<void>(`/console/v1/agent-schemas/${id}/enabled`, {
      method: 'PATCH',
      params: { enabled },
    });
  }
}