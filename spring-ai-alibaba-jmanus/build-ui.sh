# /*
#  * Copyright 2025 the original author or authors.
#  *
#  * Licensed under the Apache License, Version 2.0 (the "License");
#  * you may not use this file except in compliance with the License.
#  * You may obtain a copy of the License at
#  *
#  *      https://www.apache.org/licenses/LICENSE-2.0
#  *
#  * Unless required by applicable law or agreed to in writing, software
#  * distributed under the License is distributed on an "AS IS" BASIS,
#  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  * See the License for the specific language governing permissions and
#  * limitations under the License.
#  */
#!/bin/bash

# 构建前端UI并部署到Spring Boot静态资源目录
# 适用于 macOS

set -e  # 遇到错误立即退出

# 获取脚本所在目录的绝对路径
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"

# 定义颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 输出带颜色的日志
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查必要的命令是否存在
check_commands() {
    log_info "检查必要的命令..."
    
    if ! command -v pnpm &> /dev/null; then
        log_error "pnpm 未安装，请先安装 pnpm"
        log_info "安装命令: npm install -g pnpm"
        exit 1
    fi
    
    log_success "命令检查完成"
}

# 检查目录结构
check_directories() {
    log_info "检查项目目录结构..."
    
    if [ ! -d "$PROJECT_ROOT/ui-vue3" ]; then
        log_error "ui-vue3 目录不存在: $PROJECT_ROOT/ui-vue3"
        exit 1
    fi
    
    if [ ! -f "$PROJECT_ROOT/ui-vue3/package.json" ]; then
        log_error "ui-vue3/package.json 文件不存在"
        exit 1
    fi
    
    if [ ! -d "$PROJECT_ROOT/src/main/resources" ]; then
        log_error "Spring Boot resources 目录不存在: $PROJECT_ROOT/src/main/resources"
        exit 1
    fi
    
    log_success "目录结构检查完成"
}

# 构建前端项目
build_frontend() {
    log_info "开始构建前端项目..."
    
    cd "$PROJECT_ROOT/ui-vue3"
    
    # 检查是否存在 node_modules，如果不存在则安装依赖
    if [ ! -d "node_modules" ]; then
        log_warning "node_modules 不存在，正在安装依赖..."
        pnpm install
    fi
    
    # 运行构建命令
    log_info "运行 pnpm run build..."
    pnpm run build
    
    # 检查构建结果
    if [ ! -d "ui" ]; then
        log_error "构建失败，ui 目录不存在"
        exit 1
    fi
    
    log_success "前端项目构建完成"
}

# 清空静态资源目录
clean_static_directory() {
    log_info "清空静态资源目录..."
    
    STATIC_DIR="$PROJECT_ROOT/src/main/resources/static"
    
    # 创建目录（如果不存在）
    mkdir -p "$STATIC_DIR"
    
    # 清空目录内容
    if [ "$(ls -A $STATIC_DIR)" ]; then
        log_warning "删除 $STATIC_DIR 中的现有文件..."
        rm -rf "$STATIC_DIR"/*
        log_success "静态资源目录已清空"
    else
        log_info "静态资源目录已经是空的"
    fi
}

# 拷贝构建文件
copy_build_files() {
    log_info "拷贝构建文件到静态资源目录..."
    
    SOURCE_DIR="$PROJECT_ROOT/ui-vue3/ui"
    TARGET_DIR="$PROJECT_ROOT/src/main/resources/static/"
    
    if [ ! -d "$SOURCE_DIR" ]; then
        log_error "源目录不存在: $SOURCE_DIR"
        exit 1
    fi
    
    # 拷贝文件
    cp -r "$SOURCE_DIR" "$TARGET_DIR/"
    
    # 验证拷贝结果
    if [ "$(ls -A $TARGET_DIR)" ]; then
        log_success "文件拷贝完成"
        log_info "拷贝的文件:"
        ls -la "$TARGET_DIR"
    else
        log_error "文件拷贝失败，目标目录为空"
        exit 1
    fi
}

# 显示构建摘要
show_summary() {
    log_success "=== 构建完成 ==="
    log_info "前端文件已成功部署到: $PROJECT_ROOT/src/main/resources/static/"
    log_info "现在可以运行 Spring Boot 应用了"
    log_info ""
    log_info "启动命令示例:"
    log_info "  mvn spring-boot:run"
    log_info "  或者"
    log_info "  java -jar target/spring-ai-alibaba-jmanus-*.jar"
}

# 主函数
main() {
    log_info "开始前端构建和部署流程..."
    log_info "项目根目录: $PROJECT_ROOT"
    echo ""
    
    check_commands
    check_directories
    build_frontend
    clean_static_directory
    copy_build_files
    show_summary
    
    log_success "所有步骤完成！"
}

# 错误处理
trap 'log_error "脚本执行过程中发生错误，退出码: $?"' ERR

# 运行主函数
main "$@"
