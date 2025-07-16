# Spring AI Alibaba JManus

<div align="center">

[![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)
[![GitHub Stars](https://img.shields.io/github/stars/alibaba/spring-ai-alibaba.svg)](https://github.com/alibaba/spring-ai-alibaba/stargazers)

🌍 [English](./README.md) | [中文](./README-zh.md)
📚 Developer Docs: [Quick Start (EN)](./README-dev-en.md) | [开发者快速入门 (中文)](./README-dev.md)

**A comprehensive Java implementation of the OpenManus Multi-Agent Framework featuring UNLIMITED context window capabilities**

*Empowering developers of all skill levels to effortlessly build sophisticated multi-agent systems and unlock unprecedented productivity*

[About](#-about) • [Quick Start](#-quick-start) • [Contributing](#-contributing)

</div>

![image](https://github.com/user-attachments/assets/07feeb29-c410-4f56-89bf-532210bc1b63)

---

## 🎯 About

JManus is a Java implementation of Manus, currently used in many applications within Alibaba Group. It is primarily used for handling exploratory tasks that require a certain degree of determinism, such as quickly finding data from massive datasets and converting it into a single row in a database, or analyzing logs and issuing alerts.


### JManus Product Features

- 🤖 **Pure Java Manus Implementation**: A pure Java multi-agent collaboration implementation that provides a complete set of HTTP call interfaces, suitable for secondary integration by Java developers.
![Image](https://github.com/user-attachments/assets/f27b763e-9c68-44e0-a57d-4f31d04c0200)
- 🌊 **Infinite Context Handling**: Overcomes single-model context limitations through intelligent multi-agent collaboration, enabling infinite content processing.
![Image](https://github.com/user-attachments/assets/a0245658-fbb7-41dc-989f-86574592f188)
- 🎯 **Excellent Plan-Act Mode**: Fully implements the Plan-Act paradigm, featuring a separation of intelligent planning and execution.
![Image](https://github.com/user-attachments/assets/d00fc59d-3f10-4163-a548-784eb21f77d6)
- 🔗 **MCP Integration**: Natively supports the Model Context Protocol (MCP) for seamless integration with external services and tools.
![Image](https://github.com/user-attachments/assets/df24679a-77f1-4e66-a15f-5e0fadcffacf)
- 📜 **Web Interface for Agent Configuration**: Easily configure agents through an intuitive web management interface without modifying code.
![Image](https://github.com/user-attachments/assets/96d5902a-f741-4e82-9007-136cf4c56bb0)


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
