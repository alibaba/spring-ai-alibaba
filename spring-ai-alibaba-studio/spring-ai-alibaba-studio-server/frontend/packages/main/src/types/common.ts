export interface IApiResponse<T = any> {
  data: T;
  request_id: string;
}
