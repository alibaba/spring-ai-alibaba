
{
  "pipelineId": "novel-summary-pipeline-1747884382010",
  "title": "小说章节总结智能体流水线",
  "steps": [
    {
      "agentType": "PARALLEL_AGENT",
      "stepRequirement": "读取文件，并以5000字为一个batch，以章节+5000字上限切分为小块,进行并行处理",
      "steps": [
        {
          "agentType": "LLM_AGENT",
          "stepRequirement": "读取文件，并以5000字为一个batch，以章节+5000字上限切分为小块"
        },
        {
          "agentType": "LLM_AGENT", 
          "stepRequirement": "针对每一个文件块，都总结他的内容，输出到以文件章节名为key，文章总结为value的内容里"
        }
      ]
    },
    {
      "agentType": "LLM_AGENT",
      "stepRequirement": "汇总总结每个章节，然后按照章节顺序把内容拼装为单独的文件"
    },
    {
      "agentType": "LLM_AGENT",
      "stepRequirement": "将所有的单独的文件，一个一个的读取并合并为一个整体的文件"
    }
  ]
}
