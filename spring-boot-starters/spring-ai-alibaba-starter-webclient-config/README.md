# Spring AI Alibaba Starter WebClient Config

This Spring Boot starter provides optimized WebClient configuration to prevent "Connection reset" errors when using Spring AI with DashScope and other AI providers.

## Problem

When using Spring AI's DashScope integration (or other providers that use WebClient), you may encounter `Connection reset` errors after a period of inactivity. This happens because:

1. The WebClient maintains a connection pool with idle connections
2. The server (e.g., dashscope.aliyuncs.com) closes idle connections after a timeout
3. When the client tries to reuse these stale connections, it gets a "Connection reset" error

## Solution

This starter automatically configures WebClient with proper connection pool settings that:
- Evict idle connections before they're closed by the server
- Set maximum connection lifetime
- Periodically clean up stale connections in the background

## Usage

### Maven

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-webclient-config</artifactId>
</dependency>
```

### Gradle

```gradle
implementation 'com.alibaba.cloud.ai:spring-ai-alibaba-starter-webclient-config'
```

## Configuration

The starter provides default configuration that works for most use cases. You can customize the settings in your `application.yml` or `application.properties`:

```yaml
spring:
  ai:
    alibaba:
      webclient:
        enabled: true  # Enable WebClient configuration (default: true)
        max-connections: 500  # Maximum connections per host (default: 500)
        max-idle-time: 30s  # Max idle time before eviction (default: 30s)
        max-life-time: 5m  # Max connection lifetime (default: 5m)
        eviction-interval: 10s  # Background eviction interval (default: 10s)
        pending-acquire-queue: true  # Enable pending acquire queue (default: true)
        max-pending-acquires: 1000  # Max pending requests (default: 1000)
```

### Properties

| Property | Default | Description |
|----------|---------|-------------|
| `spring.ai.alibaba.webclient.enabled` | `true` | Enable WebClient connection pool configuration |
| `spring.ai.alibaba.webclient.max-connections` | `500` | Maximum number of connections per host |
| `spring.ai.alibaba.webclient.max-idle-time` | `30s` | Maximum time a connection can be idle before being evicted. This is the key setting to prevent "Connection reset" errors |
| `spring.ai.alibaba.webclient.max-life-time` | `5m` | Maximum time a connection can live |
| `spring.ai.alibaba.webclient.eviction-interval` | `10s` | Interval for evicting idle connections in background |
| `spring.ai.alibaba.webclient.pending-acquire-queue` | `true` | Enable pending acquire queue |
| `spring.ai.alibaba.webclient.max-pending-acquires` | `1000` | Maximum pending acquire requests |

## How It Works

The starter automatically provides a `WebClient.Builder` bean with a configured `ConnectionProvider` that:

1. **Evicts idle connections**: Connections idle for more than `max-idle-time` are removed
2. **Background eviction**: Periodically scans and evicts stale connections every `eviction-interval`
3. **Connection lifetime limit**: Connections are replaced after `max-life-time` even if still active
4. **Connection validation**: Validates connections before reuse to detect stale connections

This prevents the "Connection reset" error by ensuring the client never tries to reuse a connection that the server has already closed.

## License

Apache License 2.0
