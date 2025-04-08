这里主要要处理的问题是：
1）有一些资源有冲突（比如browser, 文件访问）
2）一些资源要跨agent使用，比如 上一个agent 需要，下一个agent 在浏览器执行navigate命令。
3）其他时候又要不同agent有一些临时的中间结果状态 。
4) 在Plan结束的时候要保证所有的资源都是关闭的。

所以,大致的策略是：
1） 组装放在了 ManusConfiguration .
2)  有可能资源冲突的类，用Service的方式独立出来
3） tools要实现，并提供 PlanBasedLifecycleService，来保障最后资源能被关闭
4） tools 不要使用static 。
