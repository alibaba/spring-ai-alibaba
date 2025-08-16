import { IAppType } from '@/services/appComponent';
import { IValueType } from '@spark-ai/flow';

// Interface for application component list item
export interface IAppComponentListItem {
  code?: string;
  name?: string;
  app_name?: string;
  type?: IAppType;
  app_id?: string;
  config: string;
  description?: string;
  status?: number;
  need_update?: number;
  gmt_modified?: number;
  gmt_create?: number;
}

// Response structure for application component list
export interface IAppComponentListResponse {
  total: number;
  records: IAppComponentListItem[];
}

// Interface for referenced application list item
export interface IReferAppListItem {
  type: IAppType;
  app_id: string;
  name: string;
}

// Interface for enabled application list item
export interface IEnableAppListItem {
  app_id: string;
  name: string;
  description: string;
  type: IAppType;
}

// Response structure for enabled application list
export interface IEnableAppListResponse {
  total: number;
  records: IEnableAppListItem[];
}

// Interface for application component input parameter
export interface IAppComponentInputParamItem {
  required: boolean;
  alias: string;
  type: IValueType;
  description?: string;
  display: boolean;
  field: string;
  source: string;
}

// Interface for application component output parameter
export interface IAppComponentOutputParamItem {
  required: boolean;
  field: string;
  type: IValueType;
  description?: string;
  display: boolean;
}
