import { request } from '@/request';
import { IApiResponse } from '@/types/common';
import type {
  ICreateMcpParams,
  IGetMcpServerParams,
  IListMcpServersByCodesParams,
  IListMcpServersParams,
  IMcpServer,
  IMcpServerCallToolRequest,
  IMcpServerCallToolResponse,
  IPagingList,
  IUpdateMcpParams,
} from '@/types/mcp';

/**
 * Create a new MCP server
 * @param params Parameters for creating MCP server
 * @returns Promise containing API response with server code
 */
export async function createMcpServer(
  params: ICreateMcpParams,
): Promise<IApiResponse<string>> {
  const response = await request({
    url: '/console/v1/mcp-servers',
    method: 'POST',
    data: params,
  });
  return response.data as IApiResponse<string>;
}

/**
 * Update an existing MCP server
 * @param params Parameters for updating MCP server
 * @returns Promise containing API response with server code
 */
export async function updateMcpServer(
  params: IUpdateMcpParams,
): Promise<IApiResponse<string>> {
  const response = await request({
    url: '/console/v1/mcp-servers',
    method: 'PUT',
    data: params,
  });
  return response.data as IApiResponse<string>;
}

/**
 * Delete an MCP server
 * @param server_code Server code to delete
 * @returns Promise containing API response
 */
export async function deleteMcpServer(
  server_code: string,
): Promise<IApiResponse<null>> {
  const response = await request({
    url: `/console/v1/mcp-servers/${server_code}`,
    method: 'DELETE',
  });
  return response.data as IApiResponse<null>;
}

/**
 * Get details of an MCP server
 * @param params Parameters containing server code
 * @returns Promise containing MCP server details
 */
export async function getMcpServer(
  params: IGetMcpServerParams,
): Promise<IApiResponse<IMcpServer>> {
  const response = await request({
    url: `/console/v1/mcp-servers/${params.server_code}`,
    method: 'GET',
    params,
  });
  return response.data as IApiResponse<IMcpServer>;
}

/**
 * Get paginated list of MCP servers
 * @param params Query parameters including pagination
 * @returns Promise containing paginated list of MCP servers
 */
export async function listMcpServers(
  params: IListMcpServersParams,
): Promise<IApiResponse<IPagingList<IMcpServer>>> {
  const response = await request({
    url: '/console/v1/mcp-servers',
    method: 'GET',
    params,
  });
  return response.data as IApiResponse<IPagingList<IMcpServer>>;
}

/**
 * Get MCP servers by their codes
 * @param params Parameters containing server codes
 * @returns Promise containing list of MCP servers
 */
export async function listMcpServersByCodes(
  params: IListMcpServersByCodesParams,
): Promise<IApiResponse<IMcpServer[]>> {
  const response = await request({
    url: '/console/v1/mcp-servers/query-by-codes',
    method: 'POST',
    data: params,
  });
  return response.data as IApiResponse<IMcpServer[]>;
}

/**
 * Debug MCP server tool
 * @param params Parameters for tool debugging
 * @returns Promise containing tool execution results
 */
export async function debugMcpTool(
  params: IMcpServerCallToolRequest,
): Promise<IApiResponse<IMcpServerCallToolResponse>> {
  const response = await request({
    url: '/console/v1/mcp-servers/debug-tools',
    method: 'POST',
    data: params,
  });
  return response.data as IApiResponse<IMcpServerCallToolResponse>;
}
