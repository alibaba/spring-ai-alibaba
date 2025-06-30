# Troubleshooting Guide

## Maven Dependency Download Exception (#888)

### Problem
```
[ERROR] Could not resolve dependencies for project com.alibaba.cloud.ai:spring-ai-alibaba-helloworld
[ERROR] com.alibaba.cloud.ai:spring-ai-alibaba-starter:jar:1.0.0-M8.1-SNAPSHOT was not found
```

### Solution
```bash
# Step 1: Configure Python 3
export PYTHON=python3

# Step 2: Clear Maven cache and reinstall
mvn clean install
```

### If Still Not Working
```bash
# Force update dependencies
mvn clean install -U
```

### Alternative: Add Repository to pom.xml
```xml
<repositories>
    <repository>
        <id>spring-snapshots</id>
        <url>https://repo.spring.io/snapshot</url>
        <snapshots><enabled>true</enabled></snapshots>
    </repository>
</repositories>
```

---
*For more complex issues, please create a new GitHub issue with full error details.*
