# Spring AI Alibaba JManus

<div align="center">

[![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)
[![GitHub Stars](https://img.shields.io/github/stars/alibaba/spring-ai-alibaba.svg)](https://github.com/alibaba/spring-ai-alibaba/stargazers)

🌍 [English](./README.md) | [中文](./README-zh.md)

📚 Developer Docs: [Quick Start (EN)](./README-dev-en.md) | [开发者快速入门 (中文)](./README-dev.md)

[About](#-about) • [Quick Start](#-quick-start) • [Contributing](#-contributing)

</div>

![image](https://github.com/user-attachments/assets/07feeb29-c410-4f56-89bf-532210bc1b63)

---

## ✨ About JManus

JManus is a Java implementation of Manus, currently used in many applications within Alibaba Group. It is primarily used for handling exploratory tasks that require a certain degree of determinism, such as quickly finding data from massive datasets and converting it into a single row in a database, or analyzing logs and issuing alerts.

JManus also provides HTTP service invocation capabilities, making it suitable for integration into existing projects. For details, please refer to the developer quick start guide.

## 🎯 JManus Product Features

### - 🤖 **Pure Java Manus Implementation**: 

A pure Java multi-agent collaboration implementation that provides a complete set of HTTP call interfaces, suitable for secondary integration by Java developers.

![Image](https://github.com/user-attachments/assets/3d98c1c6-aabb-45a2-b192-7b687093a1ee)

### - 🛠️ **Plan-Act Mode**: 

Allows you to precisely control every execution detail, providing extremely high execution determinism.

![Image](https://github.com/user-attachments/assets/a689791f-adf5-44b6-9ea6-151f557a26d4)

### - 🔗 **MCP Integration**:

 Natively supports the Model Context Protocol (MCP) for seamless integration with external services and tools.

![Image](https://github.com/user-attachments/assets/2d3f833f-ba45-42b6-8e1b-f3e9cfd40212)

### - 📜 **Web Interface for Agent Configuration**:

 Easily configure agents through an intuitive web management interface without modifying code.

![Image](https://github.com/user-attachments/assets/bb25f778-f8c3-46da-9da3-6f7ea2f0917d)

### - 🌊 **Infinite Context Handling**: 

Supports precise extraction of target information from massive content without relying on specific long-context models.

![Image](https://github.com/user-attachments/assets/a0245658-fbb7-41dc-989f-86574592f188)


## 🚀 Quick Start

Get JManus up and running in under 5 minutes:

### Prerequisites

- ☕ **Java 17+** (OpenJDK recommended)
- 🌐 **DashScope API Key** (or alternative AI model provider)

### 1. Clone and Navigate

```bash
git clone https://github.com/alibaba/spring-ai-alibaba.git
cd spring-ai-alibaba/spring-ai-alibaba-jmanus
```

### 2. Configure Your API Key

```bash
# Set your DashScope API key
export DASHSCOPE_API_KEY=your_api_key_here
```

> 💡 **Get your DashScope API Key**: Visit [Alibaba Cloud Console](https://bailian.console.aliyun.com/?tab=model#/api-key) to obtain your free API key.
> 
> **Using other providers?** Update the configuration in `src/main/resources/application.yml` to use your preferred AI model platform.


### 3. Database Configuration (Optional)

JManus supports both H2 (default)、MySQL and PostgreSQL databases. 

#### How To Use MySQL/PostgreSQL

1. **Configure Database Connection**:
   Update the database configuration and JPA database-platform in the application-mysql.yml/application-postgres.yml under 'src/main/resources/':

   ```yaml
   spring:
     datasource:
       url: your_url
       username: your_username
       password: your_password
     jpa:
       database-platform: org.hibernate.dialect.MySQLDialect/PostgreSQLDialect
   ```

3. **Activate MySQL/PostgreSQL Profile**:
   Update configuration in `src/main/resources/application.yml`:

   ```bash
   spring:
     ...
     profiles:
       active: mysql/postgres  
   ```

> 💡 **Note**: The application will automatically create required tables on first startup using JPA's `ddl-auto: update` configuration.

### 4. Launch the Application

**For Unix-like systems (macOS, Linux):**
```bash
../mvnw spring-boot:run
```

**For Windows systems:**
```bash
../mvnw.cmd spring-boot:run
```

### 5. Access Your Multi-Agent Dashboard

Navigate to `http://localhost:18080` in your browser.

🎉 **Congratulations!** Your multi-agent system is now live and ready for action.



## stable Release

you can find stable release from here:
[release](https://github.com/rainerWJY/Java-Open-Manus/releases)


## 🤝 Contributing

We enthusiastically welcome contributions from the developer community! Here's how you can make an impact:

### Contribution Opportunities

You can find available tasks on our [project board](https://github.com/orgs/alibaba/projects/24).

- 🐛 **Bug Reports**: [Submit detailed issue reports](https://github.com/alibaba/spring-ai-alibaba/issues)
- 💡 **Feature Requests**: [Propose innovative enhancements](https://github.com/alibaba/spring-ai-alibaba/issues)
- 📝 **Documentation**: Help us improve clarity and completeness
- 🔧 **Code Contributions**: [Submit pull requests](https://github.com/alibaba/spring-ai-alibaba/pulls) with your improvements

### Development Environment Setup

```bash
# Fork and clone the repository
git clone https://github.com/your-username/spring-ai-alibaba.git
cd spring-ai-alibaba/spring-ai-alibaba-jmanus

# Install project dependencies
mvn clean install

# Apply code formatting standards
mvn spotless:apply

# Start the development server
mvn spring-boot:run
```

### Development Guidelines

- Follow existing code style and conventions
- Write comprehensive tests for new features
- Update documentation for any API changes
- Ensure all tests pass before submitting PRs

---

<div align="center">

**Crafted with ❤️ by the Spring AI Alibaba Team**

⭐ **Star us on GitHub** if JManus accelerated your development journey!

</div>
