// API Key base info
export interface IApiKey {
  id?: number;
  api_key?: string;
  description?: string;
  gmt_create?: string;
}

// Create API Key parameters
export interface ICreateApiKeyParams {
  description: string;
}

// Update API Key parameters
export interface IUpdateApiKeyParams {
  id: number | string;
  description: string;
}

// Pagination list response
export interface IPagingList<T> {
  current: number;
  size: number;
  total: number;
  records: T[];
}
