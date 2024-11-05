import { ChatModel } from "@/types/chat_model";
import { request } from "ice";

export default {
  // 获取ChatModels列表
  async getChatModels(): Promise<ChatModel[]> {
    return await request({
      url: "studio/api/chat-models",
      method: "get",
    });
  },
};
