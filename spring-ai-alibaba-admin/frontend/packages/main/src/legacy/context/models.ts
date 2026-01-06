import { createContext } from "react";

export const ModelsContext = createContext<{
  modelNameMap: Record<number, string>;
  models: PromptAPI.GetModelsResult["pageItems"];
  setModels: (models: PromptAPI.GetModelsResult["pageItems"]) => void;
}>({
  modelNameMap: {},
  models: [],
  setModels: (models: PromptAPI.GetModelsResult["pageItems"]) => {}
});