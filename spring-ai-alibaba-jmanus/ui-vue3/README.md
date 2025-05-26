# JManus UI

<p align="center">
  <img src="./public/logo.png" alt="JManus UI Logo" width="200"/>
</p>

<p align="center">
  <a href="https://vuejs.org/"><img src="https://img.shields.io/badge/vue-3.x-brightgreen.svg" alt="Vue 3"></a>
  <a href="https://www.typescriptlang.org/"><img src="https://img.shields.io/badge/typescript-5.x-blue.svg" alt="TypeScript"></a>
  <a href="https://ant.design/"><img src="https://img.shields.io/badge/UI-Ant%20Design%20Vue-blue" alt="Ant Design Vue"></a>
  <a href="./LICENSE"><img src="https://img.shields.io/badge/license-Apache%202.0-blue.svg" alt="License"></a>
</p>

English | [中文](./README-zh.md)

A modern, responsive web interface for [Spring AI Alibaba JManus](../README.md).

## 🚀 Quick Start

### Prerequisites

- Node.js >= 16 (comes with npm)
- pnpm

**For Java Developers:**

#### 1. Install Node.js and npm

Node.js is a JavaScript runtime environment that includes npm (Node Package Manager), a JavaScript package manager.

- **Windows/macOS**:
  Visit the [official Node.js website](https://nodejs.org/) to download and install the LTS (Long-Term Support) version for your operating system. The installer will automatically install Node.js and npm.
- **Linux**:
  You can use your distribution's package manager. For example, on Ubuntu/Debian:
  ```bash
  sudo apt update
  sudo apt install nodejs npm
  ```
  For other distributions, please refer to the [Node.js installation guide](https://nodejs.org/en/download/package-manager).

After installation, you can verify it by running:
```bash
node -v
npm -v
```

#### 2. Install pnpm (Recommended)

pnpm is a fast, disk space-efficient package manager. We recommend using pnpm to manage this project's dependencies.

Install pnpm globally using npm:
```bash
npm install -g pnpm
```

Verify the pnpm installation:
```bash
pnpm -v
```

Why use pnpm?
- **Fast**: pnpm is generally faster than npm and yarn for installing dependencies.
- **Disk Space Efficient**: pnpm uses a content-addressable store to store all modules, meaning shared dependencies are not duplicated across projects.
- **Strict Dependency Management**: pnpm creates a non-flat `node_modules` directory, which helps prevent certain dependency issues.

### Installation

```bash
# Clone the repository (if you haven't already)
# git clone https://github.com/spring-ai-alibaba/spring-ai-alibaba.git

# Navigate to the UI directory
# cd spring-ai-alibaba-jmanus/ui-vue3

# Install dependencies using pnpm
pnpm install

# Start development server
pnpm run dev
```

### Build

```bash
# Build for production using pnpm
pnpm run build
```

## �� Configuration

The UI can be configured through environment variables:

```env
# API Configuration
VITE_API_BASE_URL=http://localhost:8080

# Other configurations...
```

## 📖 Documentation

For detailed documentation about JManus and its UI, please refer to:
- [JManus Documentation](../README.md)

## 🤝 Contributing

We welcome contributions! Please read our [Contributing Guide](../../CONTRIBUTING.md) before submitting a Pull Request.

### Development Setup

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request
