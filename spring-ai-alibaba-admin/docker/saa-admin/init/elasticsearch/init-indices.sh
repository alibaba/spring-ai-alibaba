#!/bin/bash

# 设置重试次数和延迟
MAX_RETRIES=3
RETRY_DELAY=10
HEALTH_CHECK_RETRIES=12
HEALTH_CHECK_DELAY=5

# 颜色输出函数
log_info() {
    echo -e "\033[32m[INFO]\033[0m $1"
}

log_warn() {
    echo -e "\033[33m[WARN]\033[0m $1"
}

log_error() {
    echo -e "\033[31m[ERROR]\033[0m $1"
}

log_success() {
    echo -e "\033[32m[SUCCESS]\033[0m $1"
}

# 重试函数
retry_operation() {
    local operation_name="$1"
    local command="$2"
    local retry_count=0
    
    log_info "开始执行: $operation_name"
    
    while [ $retry_count -lt $MAX_RETRIES ]; do
        if eval "$command"; then
            log_success "$operation_name 执行成功"
            return 0
        else
            retry_count=$((retry_count + 1))
            if [ $retry_count -lt $MAX_RETRIES ]; then
                log_warn "$operation_name 执行失败，${RETRY_DELAY}秒后进行第 $retry_count 次重试..."
                sleep $RETRY_DELAY
            else
                log_error "$operation_name 执行失败，已重试 $MAX_RETRIES 次，退出"
                return 1
            fi
        fi
    done
}

# 检查 Elasticsearch 集群健康状态
check_cluster_health() {
    local retry_count=0
    
    log_info "检查 Elasticsearch 集群健康状态..."
    
    while [ $retry_count -lt $HEALTH_CHECK_RETRIES ]; do
        local health_status=$(curl -s "http://elasticsearch:9200/_cluster/health" 2>/dev/null)
        
        if [ $? -eq 0 ] && echo "$health_status" | grep -q '"status":"green"\|"status":"yellow"'; then
            local status=$(echo "$health_status" | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
            log_success "Elasticsearch 集群状态: $status"
            return 0
        else
            retry_count=$((retry_count + 1))
            if [ $retry_count -lt $HEALTH_CHECK_RETRIES ]; then
                log_warn "集群未就绪，${HEALTH_CHECK_DELAY}秒后重试... (${retry_count}/${HEALTH_CHECK_RETRIES})"
                sleep $HEALTH_CHECK_DELAY
            else
                log_error "Elasticsearch 集群健康检查失败，已重试 $HEALTH_CHECK_RETRIES 次"
                return 1
            fi
        fi
    done
}

# 等待 Elasticsearch 启动
log_info "等待 Elasticsearch 启动..."
until curl -s http://elasticsearch:9200/_cluster/health > /dev/null 2>&1; do
    log_info "等待 Elasticsearch 启动..."
    sleep 5
done

log_success "Elasticsearch 已启动"

# 检查集群健康状态
if ! check_cluster_health; then
    log_error "Elasticsearch 集群健康检查失败，退出初始化"
    exit 1
fi

log_info "开始创建索引和配置..."

# 创建 pipeline
log_info "创建 parsing_loongsuite_traces pipeline..."
pipeline_command='curl -X PUT "http://elasticsearch:9200/_ingest/pipeline/parsing_loongsuite_traces" \
  -H "Content-Type: application/json" \
  -d '"'"'{
    "processors": [
      {
        "json": {
          "field": "contents.attribute",
          "target_field": "attributes"
        }
      },
      {
        "json": {
          "field": "contents.resource",
          "target_field": "resources"
        }
      },
      {
        "json": {
          "field": "contents.links",
          "target_field": "spanLinks"
        }
      },
      {
        "json": {
          "field": "contents.logs",
          "target_field": "spanEvents"
        }
      },
      {
        "remove": {
          "field": [
            "contents.attribute",
            "contents.resource",
            "contents.links",
            "contents.logs"
          ]
        }
      },
      {
        "rename": {
          "field": "contents",
          "target_field": "metadata"
        }
      },
      {
        "script": {
          "source": "Map usage = new HashMap();\nlong total = 0;\nif (ctx.attributes.containsKey(\"gen_ai.usage.input_tokens\")) {\n  long input = Long.parseLong(ctx.attributes[\"gen_ai.usage.input_tokens\"]);\n  usage[\"input_tokens\"] = input;\n  total = total + input;\n}\nif (ctx.attributes.containsKey(\"gen_ai.usage.output_tokens\")) {\n  long output = Long.parseLong(ctx.attributes[\"gen_ai.usage.output_tokens\"]);\n  usage[\"output_tokens\"] = output;\n  total = total + output;\n}\nusage[\"total_tokens\"] = total;\nctx.usage = usage;"
        }
      }
    ]
  }'"'"''

if ! retry_operation "创建 parsing_loongsuite_traces pipeline" "$pipeline_command"; then
    log_error "创建 pipeline 失败，退出初始化"
    exit 1
fi

# 验证 pipeline 创建成功
log_info "验证 pipeline 创建是否成功..."
pipeline_verification_command='curl -s -f "http://elasticsearch:9200/_ingest/pipeline/parsing_loongsuite_traces" > /dev/null'

if ! retry_operation "验证 pipeline 创建" "$pipeline_verification_command"; then
    log_error "Pipeline 验证失败，退出初始化"
    exit 1
fi

# 创建索引
log_info "创建 loongsuite_traces 索引..."
index_command='curl -X PUT "http://elasticsearch:9200/loongsuite_traces" \
  -H "Content-Type: application/json" \
  -d '"'"'{
    "settings": {
      "index.default_pipeline": "parsing_loongsuite_traces"
    },
    "mappings": {
      "dynamic": "false",
      "properties": {
        "metadata": {
          "type": "object",
          "properties": {
            "duration": {
              "type": "long"
            },
            "end": {
              "type": "long"
            },
            "host": {
              "type": "keyword"
            },
            "kind": {
              "type": "text"
            },
            "name": {
              "type": "keyword"
            },
            "otlp": {
              "type": "object",
              "properties": {
                "name": {
                  "type": "keyword"
                },
                "version": {
                  "type": "version"
                }
              }
            },
            "parentSpanID": {
              "type": "text"
            },
            "service": {
              "type": "keyword"
            },
            "spanID": {
              "type": "text"
            },
            "start": {
              "type": "long"
            },
            "statusCode": {
              "type": "text"
            },
            "statusMessage": {
              "type": "keyword"
            },
            "traceID": {
              "type": "text"
            },
            "traceState": {
              "type": "keyword"
            }
          }
        },
        "tags": {
          "type": "object"
        },
        "time": {
          "type": "long"
        },
        "attributes": {
          "type": "flattened"
        },
        "resources": {
          "type": "flattened"
        },
        "spanEvents": {
          "type": "nested",
          "properties": {
            "name": {
              "type": "keyword"
            },
            "attribute": {
              "type": "flattened"
            },
            "time": {
              "type": "long"
            }
          }
        },
        "spanLinks": {
          "type": "nested",
          "properties": {
            "spanID": {
              "type": "text"
            },
            "traceID": {
              "type": "text"
            },
            "attribute": {
              "type": "flattened"
            }
          }
        },
        "usage": {
          "type": "object",
          "properties": {
            "input_tokens": {
              "type": "long"
            },
            "output_tokens": {
              "type": "long"
            },
            "total_tokens": {
              "type": "long"
            }
          }
        }
      }
    }
  }'"'"''

if ! retry_operation "创建 loongsuite_traces 索引" "$index_command"; then
    log_error "创建索引失败，退出初始化"
    exit 1
fi

# 等待索引创建完成
log_info "等待索引创建完成..."
sleep 5

# 验证索引创建成功
log_info "验证索引创建是否成功..."
verification_command='curl -s -f "http://elasticsearch:9200/loongsuite_traces/_mapping" > /dev/null'

if ! retry_operation "验证索引创建" "$verification_command"; then
    log_error "索引验证失败，退出初始化"
    exit 1
fi

# 最终验证：检查索引状态
log_info "检查索引状态..."
index_status_command='curl -s "http://elasticsearch:9200/_cat/indices/loongsuite_traces?v"'

if ! retry_operation "检查索引状态" "$index_status_command"; then
    log_error "索引状态检查失败，退出初始化"
    exit 1
fi

log_success "索引创建完成！"
log_success "Elasticsearch 初始化成功！"
exit 0
