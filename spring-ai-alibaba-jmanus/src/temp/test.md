{
  "planId": "planTemplate-1747884382010",
  "title": "分章节总结小说的内容",
  "steps": [
    {"stepRequirement": "读取文件，并以5000字为一个batch ， 以章节+ 5000字上限 切分为小块" },
    {"stepRequirement": "针对每一个文件块，都总结他的内容，输出到以文件章节名为key ， 文章总结为value的 内容里" },
    {"stepRequirement": " 汇总总结每个章节，然后按照章节顺序把内容拼装为单独的文件" },
    {"stepRequirement": " 将所有的单独的文件，一个一个的读取并合并为一个整体的文件" }
  ]
}





每个agent都有
一个输入文件/目录 （文件大小自动统计）
  一个处理的goal 
一个输出文件/目录（文件大小自动统计）



mapReduce节点：

一个输入文件/目录 （文件大小自动统计）
  一个处理的goal
    切分
    分散处理
    聚合
一个输出文件/目录（文件大小自动统计）



Agent:
  goal:
  subAgents:
    



