import { ChatOptions, ImageOptions } from "./options";


export type ChatModel = {
  name: string;
  model: string;
  modelType: string;
  chatOptions: ChatOptions;
  imageOptions: ImageOptions;
};