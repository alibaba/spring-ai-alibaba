# Baidu Search Usage Instructions

## Regular Search

Regular search can be performed directly using the `BaiduSearchService.query` or `BaiduSearchService.apply` methods.
You can use `spring.ai.alibaba.toolcalling.baidu.search.maxResults` to add a default maximum number of results.

## Baidu Qianfan AI Search

To use Baidu Qianfan AI Search, add the following configuration:
```
spring.ai.alibaba.toolcalling.baidu.search.ai.api-key=<your key>
spring.ai.alibaba.toolcalling.baidu.search.ai.enabled=true
```

The api-key can also be set through the environment variable `BAIDU_API_KEY`.

Then use the `BaiduAiSearchService.query` or `BaiduAiSearchService.apply` methods to perform searches.

Reference documentation: https://cloud.baidu.com/doc/AppBuilder/s/pmaxd1hvy
