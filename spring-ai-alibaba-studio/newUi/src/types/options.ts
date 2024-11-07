export type ImageOptions = {
  responseFormat: string;
  model: string;
  n: number;
  size_width: number;
  size_height: number;
  size: string;
  style: string;
  seed: number;
  ref_img: string;
  ref_strength: number;
  ref_mode: string;
  negative_prompt: string;
};

export type ChatOptions = {
  maxTokens: number;
  presencePenalty: number;
  frequencyPenalty: number;
  stopSequences: string[];
  proxyToolCalls: boolean;
  model: string;
  temperature: number;
  seed: number;
  top_p: number;
  top_k: number;
  stop: Record<string, unknown>[];
  enable_search: boolean;
  incremental_output: boolean;
  repetition_penalty: number;
  tools: {
    type: 'function';
    function: {
      description: string;
      name: string;
      parameters: {
        property1: Record<string, unknown>;
        property2: Record<string, unknown>;
      };
    };
  }[];
  tool_choice: Record<string, unknown>;
  vl_high_resolution_images: boolean;
  multi_model: boolean;
};