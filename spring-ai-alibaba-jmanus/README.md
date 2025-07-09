# Spring AI Alibaba JManus

<div align="center">

[![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)
[![GitHub Stars](https://img.shields.io/github/stars/alibaba/spring-ai-alibaba.svg)](https://github.com/alibaba/spring-ai-alibaba/stargazers)

🌍 [English](./README.md) | [中文](./README-zh.md)

**A comprehensive Java implementation of the OpenManus Multi-Agent Framework featuring UNLIMITED context window capabilities**

*Empowering developers of all skill levels to effortlessly build sophisticated multi-agent systems and unlock unprecedented productivity*

[About](#-about) • [Quick Start](#-quick-start) • [Contributing](#-contributing)

</div>

![image](https://github.com/user-attachments/assets/07feeb29-c410-4f56-89bf-532210bc1b63)

---

## 🎯 About

JManus is a robust, production-ready implementation of the [OpenManus](https://github.com/FoundationAgents/OpenManus) multi-agent framework, built on the solid foundation of Spring AI. It empowers developers to create sophisticated AI agent ecosystems with minimal configuration while ensuring enterprise-grade reliability and scalability. 

Leveraging the proven Plan-Act architectural pattern, JManus supports **custom agent definitions** and intelligently **decomposes complex tasks** into collaborative subtasks distributed across multiple specialized agents. This innovative approach enables **unlimited context processing** through strategic multi-agent orchestration, transcending the limitations of single-model context windows.

### Why Choose JManus?

- 🤖 **Native Multi-Agent Architecture**: Built-in collaborative framework supporting user-defined agent capabilities and specialized roles
- 🌊 **Unlimited Context Processing**: Overcome single-model context limitations through intelligent multi-agent coordination for infinite content handling
- 🎯 **Plan-Act Pattern Excellence**: Complete implementation of the Plan-Act paradigm with intelligent planning and execution separation
- 🔗 **MCP Integration**: Native Model Context Protocol support enabling seamless integration with external services and tools

### 💡 Real-World Applications

- **🤝 Customer Experience**: Automated multi-tier customer support with intelligent escalation and resolution
- **📊 Data Intelligence**: Complex ETL pipelines with AI-driven data processing and quality assurance
- **🔍 Research & Analytics**: Automated information discovery, synthesis, and report generation
- **💼 Business Automation**: End-to-end workflow orchestration across diverse enterprise systems
- **🎓 Educational Technology**: Interactive learning environments with personalized content generation
- **🧪 Quality Assurance**: Comprehensive automated testing workflows with intelligent validation and reporting

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

### 3. Launch the Application

**For Unix-like systems (macOS, Linux):**
```bash
../mvnw spring-boot:run
```

**For Windows systems:**
```bash
../mvnw.cmd spring-boot:run
```

### 4. Access Your Multi-Agent Dashboard

Navigate to `http://localhost:18080` in your browser.

🎉 **Congratulations!** Your multi-agent system is now live and ready for action.

## 🤝 Contributing

We enthusiastically welcome contributions from the developer community! Here's how you can make an impact:

### Contribution Opportunities

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
