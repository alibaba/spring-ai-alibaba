# Use Alibaba Dragonwell 17 Ubuntu base image
FROM dragonwell-registry.cn-hangzhou.cr.aliyuncs.com/dragonwell/dragonwell:17-ubuntu

# Set working directory
WORKDIR /app

# Update package index and install basic dependencies
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    ca-certificates \
    curl \
    gnupg \
    lsb-release \
    software-properties-common \
    && rm -rf /var/lib/apt/lists/*

# Install Node.js 16.x (LTS)
RUN curl -fsSL https://deb.nodesource.com/setup_16.x | bash - && \
    apt-get install -y --no-install-recommends nodejs \
    && rm -rf /var/lib/apt/lists/*

# Replace Docker official GPG key and repository with Alibaba Cloud mirror
RUN mkdir -p /etc/apt/keyrings && \
    curl -fsSL https://mirrors.aliyun.com/docker-ce/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg && \
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://mirrors.aliyun.com/docker-ce/linux/ubuntu \
    $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null && \
    apt-get update && \
    apt-get install -y docker-ce-cli

# Verify installations
RUN java -version && \
    node --version && \
    npm --version && \
    docker --version

# Copy application JAR
COPY target/spring-ai-alibaba-deepresearch-0.1.0.jar app.jar

# Expose port (modify as needed)
EXPOSE 8080

# Set entrypoint to run application
ENTRYPOINT ["java", "-jar", "app.jar"]
