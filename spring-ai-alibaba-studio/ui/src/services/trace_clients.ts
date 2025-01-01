import { request } from 'ice';

export default {
  async getTraceDetailClient() {
    return await request('/studio/api/observation/getAITraceInfo');
  },
  async getTraceDetailClientById(traceId: string) {
    return await request({
      url: `/studio/api/observation/detail?traceId=${traceId}`,
      method: 'get',
    });
  },
};
