import { request } from '@/request';
import {
  IAppComponentInputParamItem,
  IAppComponentListItem,
  IAppComponentListResponse,
  IAppComponentOutputParamItem,
  IEnableAppListResponse,
  IReferAppListItem,
} from '@/types/appComponent';

/**
 * Enum defining application component types
 */
export enum IAppType {
  /** Basic agent type */
  AGENT = 'basic',
  /** Workflow type */
  WORKFLOW = 'workflow',
}

/**
 * Parameters for querying application component list
 */
export interface IAppComponentListQueryParams {
  /** Optional name filter */
  name?: string;
  /** Component type */
  type: IAppType;
  /** Page size */
  size: number;
  /** Current page number */
  current: number;
  /** Optional application ID filter */
  app_id?: string;
}

/**
 * Get list of application components
 * @param params Query parameters
 * @returns Promise containing component list response
 */
export const getAppComponentList = (params: IAppComponentListQueryParams) => {
  return request({
    url: '/console/v1/component-servers',
    method: 'GET',
    params,
  }).then((res) => res.data.data as IAppComponentListResponse);
};

/**
 * Parameters for querying enabled applications
 */
interface IQueryEnableAppParams {
  /** Optional application name filter */
  app_name?: string;
  /** Application type */
  type: IAppType;
  /** Page size */
  size: number;
  /** Current page number */
  current: number;
}

/**
 * Get list of enabled applications
 * @param params Query parameters
 * @returns Promise containing enabled app list response
 */
export const getEnableAppList = (params: IQueryEnableAppParams) => {
  return request({
    url: '/console/v1/component-servers/app-publishable',
    method: 'GET',
    params,
  }).then((res) => res.data.data as IEnableAppListResponse);
};

/**
 * Data structure for saving component information
 */
interface IComponentSaveData {
  /** Component name */
  name: string;
  /** Component description */
  description: string;
  /** Application ID */
  app_id: string;
  /** Configuration data */
  config: string;
  /** Component type */
  type: IAppType;
}

/**
 * Create a new application component
 * @param data Component data to create
 * @returns Promise containing creation result
 */
export const createAppComponent = (data: IComponentSaveData) => {
  return request({
    url: '/console/v1/component-servers',
    method: 'POST',
    data,
  }).then((res) => res.data.data);
};

export const updateAppComponent = (
  data: IComponentSaveData & { code: string },
) => {
  const { code, ...extraParams } = data;
  return request({
    url: `/console/v1/component-servers/${code}`,
    method: 'PUT',
    data: extraParams,
  }).then((res) => res.data.data);
};

export const deleteAppComponentByCode = (code: string) => {
  return request({
    url: `/console/v1/component-servers/${code}`,
    method: 'DELETE',
  }).then((res) => res.data.data);
};

export const getAppComponentDetailByCode = (code: string) => {
  return request({
    url: `/console/v1/component-servers/${code}/detail-by-code`,
    method: 'GET',
  }).then((res) => res.data.data as IAppComponentListItem);
};

export const getAppComponentDetailByAppCode = (appId: string) => {
  return request({
    url: `/console/v1/component-servers/${appId}/detail-by-appid`,
    method: 'GET',
  }).then((res) => res.data.data as IAppComponentListItem);
};

export const getAppComponentRefApps = (code: string) => {
  return request({
    url: `/console/v1/component-servers/${code}/query-refer`,
    method: 'GET',
  }).then((res) => res.data.data as IReferAppListItem[]);
};

export const getConfigByAppId = (appId: string) => {
  return request({
    url: `/console/v1/component-servers/${appId}/query-config`,
    method: 'GET',
  }).then((res) => res.data.data);
};

export const getAppComponentInputAndOutputParams = (code: string) => {
  return request({
    url: `/console/v1/component-servers/${code}/query-schema`,
    method: 'GET',
  }).then(
    (res) =>
      res.data.data as {
        input: IAppComponentInputParamItem[];
        output: IAppComponentOutputParamItem[];
        output_type: string;
      },
  );
};

/**
 * Get component schemas by codes
 * @param codes Array of component codes
 * @returns Promise containing mapping of codes to their schemas
 */
export const getAppComponentServersSchemaByCodes = (
  codes: string[],
): Promise<{
  [key: string]: {
    input: IAppComponentInputParamItem[];
    output: IAppComponentOutputParamItem[];
  };
}> => {
  return request({
    url: '/console/v1/component-servers/schema-by-codes',
    method: 'POST',
    data: { codes },
  }).then((res) => res.data.data);
};

/**
 * Get application components by codes
 * @param codes Array of component codes
 * @returns Promise containing list of component items
 */
export const getAppComponentServersByCodes = (
  codes: string[],
): Promise<IAppComponentListItem[]> => {
  return request({
    url: '/console/v1/component-servers/query-by-codes',
    method: 'POST',
    data: { codes },
  }).then((res) => res.data.data);
};
