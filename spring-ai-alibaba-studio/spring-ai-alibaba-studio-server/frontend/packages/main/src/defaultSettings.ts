export default {
  agentSystemPromptMaxLength: 10000, // Maximum length for agent system prompt
  agentUserPromptMaxLength: 10000, // Maximum length for agent user prompt
  agentAgentComponentMaxLimit: 5, // Maximum number of agent components per agent
  agentWorkflowComponentMaxLimit: 5, // Maximum number of workflow components per agent
  agentKnowledgeBaseMaxLimit: 10, // Maximum number of knowledge bases per agent
  agentMcpMaxLimit: 5, // Maximum number of MCPs per agent
  agentToolMaxLimit: 10, // Maximum number of plugin tools per agent
  agentPresetQuestionMaxLimit: 5, // Maximum number of preset questions per agent
  agentWelcomeMessageMaxLength: 2000, // Maximum length for agent welcome message
  agentSSETimeout: '180000', // SSE connection timeout for agent conversation API (in milliseconds)
};
