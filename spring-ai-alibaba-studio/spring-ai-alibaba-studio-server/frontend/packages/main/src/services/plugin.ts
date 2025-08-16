import { request } from '@/request';
import { IApiResponse } from '@/types/common';
import {
  ListPluginParams,
  PagingList,
  Plugin,
  PluginTool,
} from '@/types/plugin';

/**
 * Get paginated list of plugins
 * @param params Query parameters including pagination
 * @returns Promise containing paginated plugin list
 */
export async function listPlugin(
  params: ListPluginParams,
): Promise<IApiResponse<PagingList<Plugin>>> {
  const response = await request({
    url: '/console/v1/plugins',
    method: 'GET',
    params,
  });
  return response.data as IApiResponse<PagingList<Plugin>>;
}

/**
 * Delete a plugin by ID
 * @param id Plugin ID to delete
 * @returns Promise containing API response
 */
export async function removePlugin(id: string) {
  const response = await request({
    url: `/console/v1/plugins/${id}`,
    method: 'DELETE',
  });
  return response.data as IApiResponse;
}

/**
 * Create a new plugin
 * @param data Plugin creation data
 * @returns Promise containing API response with plugin ID
 */
export async function createPlugin(data: Plugin) {
  const response = await request({
    url: '/console/v1/plugins',
    method: 'POST',
    data: data,
  });
  return response.data as IApiResponse<string>;
}

/**
 * Update an existing plugin
 * @param data Plugin update data including plugin_id
 * @returns Promise containing API response
 */
export async function updatePlugin(data: Plugin) {
  const response = await request({
    url: `/console/v1/plugins/${data.plugin_id}`,
    method: 'PUT',
    data: data,
  });
  return response.data as IApiResponse<string>;
}

/**
 * Save plugin (create or update based on existence of plugin_id)
 * @param data Plugin data to save
 * @returns Promise containing API response
 */
export async function savePlugin(data: Plugin) {
  const isUpdate = !!data.plugin_id;
  if (isUpdate) return updatePlugin(data);
  return createPlugin(data);
}

/**
 * Get plugin details by ID
 * @param id Plugin ID to retrieve
 * @returns Promise containing plugin details
 */
export async function getPlugin(id: string) {
  const response = await request({
    url: `/console/v1/plugins/${id}`,
    method: 'GET',
  });
  return response.data as IApiResponse<Plugin>;
}

/**
 * Get paginated list of tools for a plugin
 * @param id Plugin ID
 * @returns Promise containing paginated tool list
 */
export async function getPluginToolList(id: string) {
  const response = await request({
    url: `/console/v1/plugins/${id}/tools`,
    method: 'GET',
    params: {
      current: 1,
      size: 100,
    },
  });
  return response.data as IApiResponse<PagingList<PluginTool>>;
}

/**
 * Create a new tool for a plugin
 * @param data Tool creation data
 * @returns Promise containing API response with tool ID
 */
export async function createTool(data: PluginTool) {
  const response = await request({
    url: `/console/v1/plugins/${data.plugin_id}/tools`,
    method: 'POST',
    data: data,
  });
  return response.data.data as string;
}

/**
 * Update an existing tool
 * @param data Tool update data including plugin_id and tool_id
 * @returns Promise containing tool ID
 */
export async function upldateTool(data: PluginTool) {
  await request({
    url: `/console/v1/plugins/${data.plugin_id}/tools/${data.tool_id}`,
    method: 'PUT',
    data: data,
  });
  return data.tool_id;
}

/**
 * Save tool (create or update based on existence of tool_id)
 * @param data Tool data to save
 * @returns Promise containing tool ID
 */
export async function saveTool(data: PluginTool) {
  const isUpdate = !!data.tool_id;
  if (isUpdate) return upldateTool(data);
  return createTool(data);
}

/**
 * Delete a tool from a plugin
 * @param pluginId Plugin ID
 * @param toolId Tool ID to delete
 * @returns Promise containing API response
 */
export async function removeTool(pluginId: string, toolId: string) {
  const response = await request({
    url: `/console/v1/plugins/${pluginId}/tools/${toolId}`,
    method: 'DELETE',
  });
  return response.data as IApiResponse;
}

/**
 * Get tool details
 * @param pluginId Plugin ID
 * @param toolId Tool ID to retrieve
 * @returns Promise containing tool details
 */
export async function getTool(pluginId: string, toolId: string) {
  if (!toolId) return undefined;
  const response = await request({
    url: `/console/v1/plugins/${pluginId}/tools/${toolId}`,
    method: 'GET',
  });
  return response.data as IApiResponse<PluginTool>;
}

/**
 * Publish a tool
 * @param pluginId Plugin ID
 * @param toolId Tool ID to publish
 * @returns Promise containing API response
 */
export async function publishTool(pluginId: string, toolId: string) {
  const response = await request({
    url: `/console/v1/plugins/${pluginId}/tools/${toolId}/publish`,
    method: 'POST',
  });
  return response.data as IApiResponse;
}

/**
 * Test a tool with parameters
 * @param pluginId Plugin ID
 * @param toolId Tool ID to test
 * @param parameters Parameters for testing
 * @returns Promise containing API response
 */
export async function testTool(
  pluginId: string,
  toolId: string,
  parameters: Record<string, any>,
) {
  const response = await request({
    url: `/console/v1/plugins/${pluginId}/tools/${toolId}/test`,
    method: 'POST',
    data: {
      arguments: parameters,
    },
  });
  return response.data as IApiResponse;
}

/**
 * Enable or disable a tool
 * @param toolId Tool ID
 * @param enable Whether to enable the tool
 * @returns Promise containing API response
 */
export async function enableTool(toolId: string, enable: boolean) {
  const response = await request({
    url: `/console/v1/tools/${toolId}/${enable ? 'enable' : 'disable'}`,
    method: 'POST',
  });
  return response.data as IApiResponse;
}

/**
 * Get plugin tools by their IDs
 * @param tool_ids Array of tool IDs
 * @returns Promise containing list of tools
 */
export async function getPluginToolsByIds(tool_ids: string[]) {
  const response = await request({
    url: `/console/v1/tools/query-by-ids`,
    method: 'POST',
    data: {
      tool_ids,
    },
  });
  return response.data as IApiResponse<PluginTool[]>;
}
