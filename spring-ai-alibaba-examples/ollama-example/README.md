# 如何本地部署大模型,这里用ollama来做示例
## 1. 下载ollama
### 进入Ollama官网  https://ollama.com/  
下载对应自己的系统的ollama
## 2. 安装ollama
### 双击OllamaSetup.exe，进行安装
>注意，在windows下安装时，是不允许选择安装位置的，默认是安装在系统盘的
安装完毕后，打开终端进行验证，在终端中输入ollama,验证ollama是否安装成功
>
> 如果ollama安装成功，会显示ollama的版本信息
## 3. 设置模型存放目录
### 3.1、在windows，ollama安装的模型，默认存放目录为C:/Users//.ollama/models
可以通过以下命令更改模型安装时的存放目录
> 只设置当前用户（需要先创建D:\ollama_models目录）
setx OLLAMA_MODELS "D:\ollama_models"
### 3.2、重启终端
>setx命令在windows中设置环境变量时，这个变量的更改只会在新打开的命令提示符窗口或终端会话中生效
### 3.3、重启ollama服务
> 在终端中输入ollama
## 4.查看Ollama支持的模型
>https://ollama.com/library
> 
>点击某个模型连接，比如llama2，可以看到模型详细的介绍
## 5.模型安装
>可以通过以下命令进行模型安装
> 
> ollama pull llama2

下载过程比较慢，耐心等待
## 6.查看已安装的模型列表
通过以下命令查看已安装的模型列表
> ollama list
## 7.运行模型
> ollama run llama2

出现send Message就说明部署好啦，可以使用模型对话了
>退出模型命令
> /bye
> 
## 温馨提示
本地部署的大模型默认端口为11434,访问地址为 http://127.0.0.1:11434
#### 可以通过修改环境变量来允许外部访问