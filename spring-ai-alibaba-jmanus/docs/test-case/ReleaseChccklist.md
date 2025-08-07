# release时候的产品质量保证冒泡checklist（最多15个check项目，多于15个要保持在15个内） 
## v302:
### 测试点：
* 通过聊天界面：send message   通过百度查询阿里最新股价  ok
* 通过plan-act界面：做plan-act 任务执行 ： 查股价  ok
* 通过plan-act界面：通过百度查询阿里最新股价的 plan ok 
* 配置界面-通用配置 ： 能正常打开关闭 浏览器headless  -> 遗留一个第一次使用可能会超时的问题 #
* 配置界面-mcp :可以添加一个mcp服务 higress.ai的ip服务 -> client与server不大兼容。但目前看client是对的
* 配置界面-agent : 可以修改agent的tools 增加 ip服务 ok
* 配置界面-Model配置 : 可以删除和增加 Model配置 ok
* 首页： 可以正常做 plan-act模式的左下角的任务 ok
* 聊天界面：可以正常切换中英文，并检查所有界面切换后都是一致的 ok

### 配置checklist:
* 要关闭h2的控制台 ok
