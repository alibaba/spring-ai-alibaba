import {request} from 'ice';

export default {

    // 获取ChatModels列表
    async getChatModels() {
        return await request({
            url: 'studio/api/chat-models',
            method: 'get',
        });
    },
};