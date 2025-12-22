import { request } from '../../utils/request';
import { API_PATH } from '../const';

// 获取Trace列表
// 接口地址: GET /api/observability/traces
// 接口作用: 分页查询追踪列表，支持搜索和筛选
// res 示例:
// {
//   "totalCount": 3000,
//     "totalPage": 30,
//       "pageToken": {
//     "spanId": "abc123def456789",
//     },
//   "records": [
//     {
//       "traceId": "abc123def456789abc123def456789",
//       "spanId": "abc123def456789",
//       "durationNs": 245000000,
//       "spanKind": "SPAN_KIND_CLIENT",
//       "service": "frontend",
//       "spanName": "GET /api/users",
//       "startTime": "2025-08-15T10:15:30.123Z",
//       "endTime": "2025-08-15T10:15:30.456Z",
//       "status": "Ok",
//       "errorCount": 0,
//       "attributes": {
//         "http.method": "GET",
//         "http.url": "/api/users",
//         "http.status_code": "200"
//       },
//       "resources": {
//         "service.name": "frontend"
//       },
//       "spanLinks": [
//         {
//           "traceId": "12345678901234567890123456789012",
//           "spanId": "1234567890123456",
//           "attributes": {
//             "http.method": "GET"
//           }
//         }
//       ],
//       "spanEvents": [
//         {
//           "time": "2025-08-15T10:15:30.123Z",
//           "name": "db.query",
//           "attributes": {
//             "db.query": "SELECT * FROM users"
//           }
//         }
//       ]
//     }
//   ]
// }
export async function getTraces(params: TracingAPI.GetTracesParams) {
  return request<TracingAPI.GetTracesResult>(`${API_PATH}/observability/traces`, {
    method: 'GET',
    params,
  });
}

// 获取单个Trace详情
// 接口地址: GET /api/observability/traces/{traceId}
// 接口作用: 根据Trace ID获取完整的Trace数据，包含所有Span的详细信息
// {
//   "records": [
//     {
//       "traceId": "abc123def456789abc123def456789",
//       "spanId": "abc123def456789",
//       "parentSpanId": "xxxxxx",
//       "durationNs": 245000000,
//       "spanKind": "SPAN_KIND_CLIENT",
//       "service": "frontend",
//       "spanName": "GET /api/users",
//       "startTime": "2025-08-15T10:15:30.123Z",
//       "endTime": "2025-08-15T10:15:30.456Z",
//       "status": "Ok",
//       "errorCount": 0,
//       "attributes": {
//         "http.method": "GET",
//         "http.url": "/api/users",
//         "http.status_code": "200"
//       },
//       "resources": {
//           "service.name": "frontend"
//       },
//       "spanLinks": [
//           {
//               "traceId": "12345678901234567890123456789012",
//               "spanId": "1234567890123456",
//               "attributes": {
//                   "http.method": "GET"
//               }
//           }
//       ],
//       "spanEvents": [
//           {
//               "time": "2025-08-15T10:15:30.123Z",
//               "name": "db.query",
//               "attributes": {
//                   "db.query": "SELECT * FROM users"
//               }
//           }
//       ]
//     }
//   ]
// }
export async function getTraceDetail(params: { traceId: string }) {
  return request<TracingAPI.GetTraceDetailResult>(`${API_PATH}/observability/traces/${params.traceId}`, {
    method: 'GET',
  });
}

// 获取服务列表
// 接口地址: GET /api/observability/services
// 接口作用: 获取所有服务列表及其操作
// res 示例:
// {
//   "services": [
//     {
//       "name": "frontend",
//       "operations": ["GET /", "GET /api/users", "POST /api/users"]
//     },
//     {
//       "name": "user-service",
//       "operations": ["getUser", "createUser", "updateUser"]
//     },
//     {
//       "name": "database",
//       "operations": ["SELECT", "INSERT", "UPDATE"]
//     }
//   ]
// }
export async function getServices(params: {startTime: string; endTime: string}) {
  return request<TracingAPI.GetServicesResult>(`${API_PATH}/observability/services`, {
    method: 'GET',
    params
  });
}

// 获取 Trace 概览
// 接口地址: GET /api/observability/overview
// 接口作用: 获取 Trace的概览信息，包括 Span 数量、操作数量、使用 Token 数量等
// res 示例：
// {
//   "span.count": {
//     "total": 88,
//     "detail": [
//       {
//         "spanName": "LLM",
//         "total": "52"
//       },
//       {
//         "spanName": "TOOL",
//         "total": "36"
//       }
//     ]
//   },
//   "operation.count": {
//     "total": 88,
//     "detail": [
//       {
//         "operationName": "chat",
//         "total": "52"
//       },
//       {
//         "spanName": "execute tool",
//         "total": "36"
//       }
//     ]
//   },
//   "usage.tokens": {
//     "total": 3431225,
//     "detail": [
//       {
//         "modelName": "qwen-max",
//         "total": 3431225
//       }
//     ]
//   }
// }
export async function getOverview(params: TracingAPI.GetOverviewParams) {
  return request<TracingAPI.GetOverviewResult>(`${API_PATH}/observability/overview`, {
    method: 'GET',
    params,
  });
}
