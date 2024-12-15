import { request } from 'ice';

export default {
    async getTraceDetailClient() {
        return await request('/studio/api/observation/getAITraceInfo');
    },
};

