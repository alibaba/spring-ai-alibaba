
{
  "pipelineId": "novel-summary-pipeline-1747884382010",
  "pipelineName": "小说章节总结智能体流水线",
  
  "rootAgent": {
    "agentType": "SEQUENTIAL_AGENT",
    "subAgents": [
      "parallel-map-agent",
      "sequential-reduce-agent"
    ]
  },
  
  "agents": {
    "parallel-map-agent": {
      "agentType": "PARALLEL_AGENT",
      "executionStrategy": "CONCURRENT",
      "subAgents": ["chapter_agent"]
    },
    
    "chapter_agent": {
      "agentType": "LLM_AGENT",
      "inputConfig": {
        "inputFile": "${inputPath}",
        "maxChunkSize": 5000,
        "autoDetectChapters": true
      },
      "outputConfig": {
        "outputFile": "${outputPath}",
        "format": "summary"
      },
      "planTemplate": {
        "steps": [
          {
            "stepRequirement": "根据输入文件，自动检测章节并以5000字为一个batch，以章节+5000字上限切分为小块"
          },
          {
            "stepRequirement": "针对每一个文件块，都总结他的内容，输出到以文件章节名为key，文章总结为value的内容里，保存到输出文件"
          }
        ]
      }
    },
    
    "sequential-reduce-agent": {
      "agentType": "SEQUENTIAL_AGENT",
      "subAgents": [
        "chapter-combiner-agent",
        "final-merger-agent"
      ]
    },
    
    "chapter-combiner-agent": {
      "agentType": "LLM_AGENT",
      "outputKey": "combined_chapters_result",
      "planTemplate": {
        "steps": [
          {
            "stepRequirement": "汇总总结每个章节，然后按照章节顺序把内容拼装为单独的文件"
          }
        ]
      }
    },
    
    "final-merger-agent": {
      "agentType": "LLM_AGENT",
      "planTemplate": {
        "steps": [
          {
            "stepRequirement": "将所有的单独的文件，一个一个的读取并合并为一个整体的文件"
          }
        ]
      }
    }
  },
  
  "executionConfig": {
    "concurrencyLevel": 3,
    "inputPath": "/path/to/novel.txt",
    "outputPath": "/output/novel-complete-summary.txt"
  }
}
