#Copyright 2024-2025 the original author or authors.
#
#Licensed under the Apache License, Version 2.0 (the "License");
#you may not use this file except in compliance with the License.
#You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
#Unless required by applicable law or agreed to in writing, software
#distributed under the License is distributed on an "AS IS" BASIS,
#WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#See the License for the specific language governing permissions and
#limitations under the License.

# ========================================
# Build Stage 1: Frontend Build
# ========================================
FROM node:21-alpine AS frontend-builder

# Install pnpm
RUN npm install -g pnpm

# Set working directory for frontend
WORKDIR /app/ui-vue3

# Copy frontend package files
COPY ui-vue3/package*.json ./
COPY ui-vue3/pnpm-lock.yaml* ./

# Install frontend dependencies
RUN pnpm install

# Copy frontend source code
COPY ui-vue3/ .

# Build frontend
RUN pnpm build-only

# ========================================
# Build Stage 2: Backend Build
# ========================================
FROM dragonwell-registry.cn-hangzhou.cr.aliyuncs.com/dragonwell/dragonwell:17-ubuntu AS backend-builder

# Install Maven
RUN apt-get update && \
    apt-get install -y --no-install-recommends maven && \
    rm -rf /var/lib/apt/lists/*

# Configure Maven to use Alibaba Cloud mirror
RUN mkdir -p /root/.m2 && \
    cat > /root/.m2/settings.xml <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
          http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <mirrors>
    <mirror>
      <id>aliyunmaven</id>
      <mirrorOf>*</mirrorOf>
      <name>阿里云公共仓库</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
EOF

# Set working directory for backend
WORKDIR /app


# Copy source code
COPY . ./

# Build the application (dependencies will be downloaded automatically)
RUN mvn clean package -Dcheckstyle.skip=true -DskipTests

# ========================================
# Runtime Stage: Final Image
# ========================================
FROM dragonwell-registry.cn-hangzhou.cr.aliyuncs.com/dragonwell/dragonwell:17-ubuntu

# Install nginx and other runtime dependencies
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    nginx \
    supervisor \
    && rm -rf /var/lib/apt/lists/*

# Create application directory
WORKDIR /app

# Copy built JAR from backend builder
COPY --from=backend-builder /app/target/spring-ai-alibaba-deepresearch-*.jar app.jar

# Copy built frontend from frontend builder
COPY --from=frontend-builder /app/ui-vue3/ui/ /var/www/html/ui/

# Create nginx configuration
RUN cat > /etc/nginx/conf.d/default.conf <<'EOF'
server {
    listen 80;
    server_name localhost;

    # Serve static files
    location /ui/ {
        alias /var/www/html/ui/;
        try_files $uri $uri/ /ui/index.html;
        index index.html;
    }

    # Proxy API requests to Spring Boot
    location /deep-research/ {
        proxy_pass http://localhost:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    # Redirect root to UI
    location = / {
         return 301 http://$http_host/ui/;
    }
}
EOF

# Create supervisor configuration
RUN cat > /etc/supervisor/conf.d/supervisord.conf <<'EOF'
[supervisord]
nodaemon=true
user=root

[program:nginx]
command=nginx -g "daemon off;"
autostart=true
autorestart=true
stderr_logfile=/var/log/supervisor/nginx.err.log
stdout_logfile=/var/log/supervisor/nginx.out.log
priority=1

[program:spring-boot]
command=java -jar /app/app.jar
autostart=true
autorestart=true
stderr_logfile=/var/log/supervisor/spring-boot.err.log
stdout_logfile=/var/log/supervisor/spring-boot.out.log
environment=SPRING_PROFILES_ACTIVE=docker
priority=2
EOF

# Remove default nginx site and ensure nginx configuration is correct
RUN rm -f /etc/nginx/sites-enabled/default && \
    nginx -t

# Create log directory and verify supervisord installation
RUN mkdir -p /var/log/supervisor &

# Expose ports
EXPOSE 80 8080

# Start supervisor to manage both nginx and spring boot
CMD ["supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]
