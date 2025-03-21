news_items = [
    {"title": "新环境政策", "source": "[Example.com](http://example.com)", "date": "2023-04-01", "summary": "郑州市实施了新的环保政策，旨在减少污染并提升居民生活质量。"},
    {"title": "经济增长情况", "source": "[Example.org](http://example.org)", "date": "2023-03-28", "summary": "尽管面临诸多挑战，郑州市经济持续增长，多个行业表现出强劲的发展势头。"}
]

markdown_content = "# 郑州新闻汇总\n\n"

for item in news_items:
    markdown_content += f"## {item['title']}\n"
    markdown_content += f"- 来源: {item['source']}\n"
    markdown_content += f"- 发布日期: {item['date']}\n"
    markdown_content += f"- 摘要: {item['summary']}\n\n"

print(markdown_content)