#!/bin/bash

# 构建并部署静态文件到 Spring Boot 资源目录
# 用法: ./build-and-deploy.sh

set -e  # 遇到错误立即退出

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 获取脚本所在目录（agent-chat-ui 目录）
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 定义路径
OUT_DIR="$SCRIPT_DIR/out"
TARGET_DIR="$SCRIPT_DIR/../spring-ai-alibaba-studio/src/main/resources/META-INF/resources/chatui"

echo -e "${GREEN}=== 开始构建和部署静态文件 ===${NC}"

# 步骤 1: 清理目标目录
echo -e "${YELLOW}步骤 1: 清理目标目录...${NC}"
if [ -d "$TARGET_DIR" ]; then
  echo "删除目录: $TARGET_DIR"
  rm -rf "$TARGET_DIR"/*
  echo -e "${GREEN}✓ 目标目录已清理${NC}"
else
  echo -e "${YELLOW}目标目录不存在，将创建: $TARGET_DIR${NC}"
  mkdir -p "$TARGET_DIR"
fi

# 步骤 2: 构建静态文件
echo -e "${YELLOW}步骤 2: 构建静态文件...${NC}"
if ! command -v pnpm &> /dev/null; then
  echo -e "${RED}错误: 未找到 pnpm 命令，请先安装 pnpm${NC}"
  exit 1
fi

pnpm run build:static

if [ ! -d "$OUT_DIR" ]; then
  echo -e "${RED}错误: 构建输出目录不存在: $OUT_DIR${NC}"
  exit 1
fi

if [ -z "$(ls -A $OUT_DIR)" ]; then
  echo -e "${RED}错误: 构建输出目录为空${NC}"
  exit 1
fi

echo -e "${GREEN}✓ 静态文件构建完成${NC}"

# 步骤 3: 复制文件到目标目录
echo -e "${YELLOW}步骤 3: 复制文件到目标目录...${NC}"
cp -r "$OUT_DIR"/* "$TARGET_DIR"/

echo -e "${GREEN}✓ 文件复制完成${NC}"

# 显示统计信息
FILE_COUNT=$(find "$TARGET_DIR" -type f | wc -l | tr -d ' ')
echo -e "${GREEN}=== 部署完成 ===${NC}"
echo -e "目标目录: ${GREEN}$TARGET_DIR${NC}"
echo -e "文件数量: ${GREEN}$FILE_COUNT${NC}"
