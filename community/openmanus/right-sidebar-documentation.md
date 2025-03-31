# 右侧边栏结构与样式文档

本文档详细介绍了 JTaskFlow 界面中右侧边栏(`right-sidebar`)的结构组成和对应的 CSS 样式文件。

## 目录

1. [结构概述](#结构概述)
2. [关键元素](#关键元素)
3. [CSS 样式文件](#css-样式文件)
4. [响应式行为](#响应式行为)
5. [图标系统](#图标系统)

## 结构概述

右侧边栏是应用中用于显示终端输出和状态信息的组件。它位于主界面的右侧，可以展开或收起，主要用于展示 JTaskFlow 的后台操作和命令执行情况。

右侧边栏在 HTML 中的基本结构是:

```html
<aside class="sidebar right-sidebar" id="rightSidebar">
    <div class="right-sidebar-header">...</div>
    <div class="right-sidebar-content">...</div>
    <div class="right-sidebar-footer">...</div>
    <div class="right-sidebar-status-bar">...</div>
</aside>
```

## 关键元素

右侧边栏包含以下几个关键元素：

### 1. 右侧边栏标题 (right-sidebar-header)

显示标题信息，告诉用户当前的上下文环境。

```html
<div class="right-sidebar-header">
    <h3>Java TaskFlow 的电脑</h3>
</div>
```

样式特点：
- 使用弹性布局，标题与可能的控制按钮之间的对齐
- 带有底部边框，创建视觉分隔
- 使用 `white-space: nowrap` 防止换行
- 在边栏收起时隐藏内容 (`overflow: hidden`)

### 2. 内容区域 (right-sidebar-content)

包含终端状态和输出信息的主要区域。

```html
<div class="right-sidebar-content">
    <div class="terminal-status">...</div>
    <div class="status-detail">...</div>
    <div class="terminal-output">...</div>
</div>
```

内容区域包含以下子元素：

#### 2.1 终端状态 (terminal-status)

显示当前终端的状态信息。

```html
<div class="terminal-status">
    <span class="icon-terminal"></span>
    <span>Java TaskFlow 正在使用终端</span>
</div>
```

#### 2.2 状态详情 (status-detail)

显示当前执行的命令详情。

```html
<div class="status-detail">
    正在执行命令 <code class="code-inline">cd /home/ubuntu && mkdir -p /home/ubuntu/screenplay/fin...</code>
</div>
```

#### 2.3 终端输出 (terminal-output)

显示命令执行的实际输出，采用类似终端的暗色背景设计。

```html
<div class="terminal-output">
    <div class="terminal-header">workspace</div>
    <pre><code>ubuntu@sandbox:~$ cd /home/ubuntu && mkdir -p /home/ubuntu/screenplay/final/new_poster
ubuntu@sandbox:~$ </code></pre>
</div>
```

### 3. 底部控制区 (right-sidebar-footer)

包含播放控制和实时状态切换的功能区域。

```html
<div class="right-sidebar-footer">
    <div class="playback-controls">
        <button class="icon-play"></button>
        <div class="progress-bar-stub"></div>
    </div>
    <button class="realtime-btn">
        <span class="icon-play"></span> 跳到实时
    </button>
    <span class="realtime-indicator">
        <span class="icon-realtime"></span> 实时
    </span>
</div>
```

### 4. 状态栏 (right-sidebar-status-bar)

显示整体进度和当前工作状态。

```html
<div class="right-sidebar-status-bar">
    <span>Manus 正在工作: 向用户呈现最终成果</span>
    <span>10 / 10 <span class="icon-up-arrow"></span></span>
</div>
```

## CSS 样式文件

右侧边栏的样式主要定义在以下 CSS 文件中：

1. **right-sidebar.css** - 主要的右侧边栏样式定义
    - 定义了边栏的基本尺寸、颜色和布局
    - 包含所有子元素的样式
    - 处理展开/收起状态的过渡效果

2. **layout.css** - 整体布局相关样式
    - 定义了边栏在整体布局中的位置和行为
    - 包含通用图标定义

3. **sidebar.css** - 通用边栏样式
    - 包含左右边栏共享的基础样式

### right-sidebar.css 样式概述

右侧边栏的主要样式特点：

```css
.right-sidebar {
    width: 420px; /* 右侧边栏宽度 */
    max-width: 800px;
    border-left: 1px solid #e0e0e0;
    padding: 15px;
    background-color: #fdfdfd; /* 略微不同的背景色 */
    transition: width 0.3s ease-in-out, padding 0.3s ease-in-out, border-width 0.3s ease-in-out; /* 平滑过渡 */
}

/* 收起状态的右侧边栏 */
.right-sidebar.collapsed {
    width: 0;
    padding-left: 0;
    padding-right: 0;
    border-width: 0;
    overflow: hidden;
}
```

终端输出区域的样式：

```css
.terminal-output {
    background-color: #202124; /* 深色背景 */
    color: #e8eaed; /* 浅色文本 */
    padding: 10px 12px;
    border-radius: 4px;
    font-family: monospace;
    font-size: 12px;
    margin-top: 10px;
}
```

## 响应式行为

右侧边栏支持展开和收起状态：

1. **展开状态**：宽度为 420px
2. **收起状态**：宽度为 0，内容被隐藏，边框也被移除

收起状态是通过添加 `.collapsed` 类来控制的：

```css
.right-sidebar.collapsed {
    width: 0;
    padding-left: 0;
    padding-right: 0;
    border-width: 0;
    overflow: hidden;
}
```

收起和展开之间的切换采用 CSS 过渡效果实现平滑动画：

```css
transition: width 0.3s ease-in-out, padding 0.3s ease-in-out, border-width 0.3s ease-in-out;
```

## 图标系统

右侧边栏使用伪元素实现的简单图标系统，在 layout.css 中定义：

```css
.icon-terminal::before { content: ">_"; }
.icon-play::before { content: "►"; }
.icon-realtime::before { content: "●"; color: #1a73e8; }
.icon-up-arrow::before { content: "▲"; }
```

这些图标应用在状态指示、控制按钮和导航元素上。