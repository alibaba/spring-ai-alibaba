# 百度搜索使用说明

## 普通搜索

普通搜索可以直接使用 BaiduSearchService.query 或 BaiduSearchService.apply 方法进行搜索
可以使用 spring.ai.alibaba.toolcalling.baidu.search.maxResults 添加默认的最大条数

## 百度千帆智能搜索

使用百度千帆智能搜索需要添加如下配置
spring.ai.alibaba.toolcalling.baidu.search.ai.api-key=<your key>
spring.ai.alibaba.toolcalling.baidu.search.ai.enabled=true

其中 api-key 也可以通过环境变量 BAIDU_API_KEY 进行设置

然后使用 BaiduAiSearchService.query 或 BaiduAiSearchService.apply 方法进行搜索

参考文档 https://cloud.baidu.com/doc/AppBuilder/s/pmaxd1hvy
