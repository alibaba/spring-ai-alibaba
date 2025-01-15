import { request } from 'ice';

export default {
  // 获取trace列表
  async getTraceDetailClient() {
    return await request('/studio/api/observation/getAITraceInfo');
  },
  // 根据traceId获取详情
  async getTraceDetailClientById(traceId: string) {
    return await request({
      url: `/studio/api/observation/detail?traceId=${traceId}`,
      method: 'get',
    });
  },
};
