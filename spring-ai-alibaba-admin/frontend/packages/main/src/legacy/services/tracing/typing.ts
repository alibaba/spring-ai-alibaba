declare namespace TracingAPI {
  interface GetTracesParams {
    service?: string;
    spanName?: string;
    startTime: string;
    endTime: string;
    limit?: number; // 1-200
    attributes?: string; // JSON string
    pageToken?: string //
  }

  interface GetTracesResult {
    totalCount: number;
    totalPage: number;
    pageToken: {
      spanId: string;
    };
    pageItems: {
      traceId: string;
      spanId: string;
      parentSpanId: string;
      durationNs: number;
      spanKind: string;
      service: string;
      spanName: string;
      startTime: string;
      endTime: string;
      status: string;
      errorCount: number;
      attributes: Record<string, string>;
      resources: Record<string, string>;
      spanLinks: {
        traceId: string;
        spanId: string;
        attributes: Record<string, string>;
      }[];
      spanEvents: {
        time: string;
        name: string;
        attributes: Record<string, string>;
      }[];
    }[];
  }

  interface GetTraceDetailParams {
    traceId: string;
  }

  interface GetTraceDetailResult {
    records: {
      traceId: string;
      spanId: string;
      parentSpanId: string;
      durationNs: number;
      spanKind: string;
      service: string;
      spanName: string;
      startTime: string;
      endTime: string;
      status: string;
      errorCount: number;
      attributes: Record<string, string>;
      resources: Record<string, string>;
      spanLinks: {
        traceId: string;
        spanId: string;
        attributes: Record<string, string>;
      }[];
      spanEvents: {
        time: string;
        name: string;
        attributes: Record<string, string>;
      }[];
    }[]
  }

  interface GetServicesParams {
    startTime: string;
    endTime: string;
  }

  interface GetServicesResult {
    services: {
      name: string;
      operations: string[];
    }[];
  }

  interface GetOverviewParams {
    startTime: string;
    endTime: string;
    detail?: boolean;
  }

  interface GetOverviewResult {
    "span.count": {
      total: number;
      detail: {
        spanName: string;
        total: number;
      }[];
    };
    "operation.count": {
      total: number;
      detail: {
        spanName: string;
        total: number;
      }[];
    };
    "usage.tokens": {
      total: number;
      detail: {
        modelName: string;
        total: number;
      }[];
    };
  }
}