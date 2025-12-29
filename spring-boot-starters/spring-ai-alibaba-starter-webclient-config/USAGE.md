# Fixing Connection Reset Errors - Usage Guide

## Problem Description

When using Spring AI Alibaba with DashScope (or other AI providers), you may encounter the following error after your application has been idle for a while:

```
org.springframework.web.reactive.function.client.WebClientRequestException: Connection reset
Caused by: java.net.SocketException: Connection reset
```

This typically happens in the following scenario:
1. Your application makes its first API call successfully
2. The application becomes idle (no API calls for a period of time)
3. The next API call fails with "Connection reset"

## Root Cause

The issue occurs because:
1. Spring AI's DashScope integration uses Spring WebFlux's WebClient with Reactor Netty
2. Reactor Netty maintains a connection pool to reuse HTTP connections
3. When connections are idle, the server (dashscope.aliyuncs.com) may close them after its own timeout
4. When your application tries to reuse these closed connections, it gets a "Connection reset" error

## Solution

Use the `spring-ai-alibaba-starter-webclient-config` starter which automatically configures WebClient with proper connection pool management.

### Step 1: Add Dependency

Add this dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.alibaba.cloud.ai</groupId>
    <artifactId>spring-ai-alibaba-starter-webclient-config</artifactId>
</dependency>
```

For Gradle:

```gradle
implementation 'com.alibaba.cloud.ai:spring-ai-alibaba-starter-webclient-config'
```

### Step 2: That's It!

The starter will automatically configure WebClient with the following optimizations:
- **Idle connection eviction**: Removes connections that have been idle for 30 seconds (default)
- **Background cleanup**: Periodically scans and removes stale connections every 10 seconds
- **Connection lifetime limit**: Replaces connections after 5 minutes
- **Connection validation**: Validates connections before reuse

### Step 3: (Optional) Customize Configuration

You can customize the connection pool settings in your `application.yml`:

```yaml
spring:
  ai:
    alibaba:
      webclient:
        enabled: true
        max-connections: 500      # Maximum connections per host
        max-idle-time: 30s        # Remove connections idle for this long
        max-life-time: 5m         # Replace connections after this time
        eviction-interval: 10s    # Scan for stale connections every X seconds
```

Or in `application.properties`:

```properties
spring.ai.alibaba.webclient.enabled=true
spring.ai.alibaba.webclient.max-connections=500
spring.ai.alibaba.webclient.max-idle-time=30s
spring.ai.alibaba.webclient.max-life-time=5m
spring.ai.alibaba.webclient.eviction-interval=10s
```

## Configuration Properties

| Property | Default | Description |
|----------|---------|-------------|
| `max-idle-time` | `30s` | **Most important setting** - Connections idle longer than this are removed. Should be less than the server's idle timeout |
| `eviction-interval` | `10s` | How often to scan for and remove idle connections |
| `max-life-time` | `5m` | Maximum time a connection can exist, regardless of use |
| `max-connections` | `500` | Maximum number of connections per host |

## Tuning Recommendations

### If you still see connection reset errors:

1. **Reduce `max-idle-time`**: Try `15s` or `20s` if the server closes connections quickly
   ```yaml
   spring.ai.alibaba.webclient.max-idle-time: 15s
   ```

2. **Increase `eviction-interval` frequency**: Scan more frequently for stale connections
   ```yaml
   spring.ai.alibaba.webclient.eviction-interval: 5s
   ```

### For high-traffic applications:

1. **Increase `max-connections`**: Support more concurrent requests
   ```yaml
   spring.ai.alibaba.webclient.max-connections: 1000
   ```

2. **Increase `max-life-time`**: Keep connections longer if they're frequently reused
   ```yaml
   spring.ai.alibaba.webclient.max-life-time: 10m
   ```

### For low-traffic applications:

1. **Decrease `max-idle-time`**: Remove idle connections more aggressively
   ```yaml
   spring.ai.alibaba.webclient.max-idle-time: 15s
   ```

2. **Reduce `max-connections`**: Save resources
   ```yaml
   spring.ai.alibaba.webclient.max-connections: 100
   ```

## Verification

After adding the starter, you should see a log message like this at startup:

```
INFO com.alibaba.cloud.ai.autoconfigure.webclient.WebClientAutoConfiguration - Configuring WebClient with connection pool settings: maxConnections=500, maxIdleTime=PT30S, maxLifeTime=PT5M, evictionInterval=PT10S
```

## Disabling

If you need to disable this configuration for any reason:

```yaml
spring:
  ai:
    alibaba:
      webclient:
        enabled: false
```

## How It Works Internally

The starter provides a `WebClient.Builder` bean that Spring AI's DashScope integration automatically uses. This builder is configured with a custom `ConnectionProvider` from Reactor Netty that:

1. **Evicts idle connections**: Checks connection idle time and removes connections that exceed `max-idle-time`
2. **Background eviction**: Runs a background task every `eviction-interval` to proactively clean up stale connections
3. **Connection lifetime management**: Ensures no connection lives longer than `max-life-time`
4. **Proper pooling**: Maintains a pool of up to `max-connections` connections per host

This prevents the application from ever attempting to reuse a connection that the server has already closed.

## Additional Resources

- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Reactor Netty Connection Pool Documentation](https://projectreactor.io/docs/netty/release/reference/index.html#_connection_pool)
- [Spring AI Alibaba Documentation](https://java2ai.com)
