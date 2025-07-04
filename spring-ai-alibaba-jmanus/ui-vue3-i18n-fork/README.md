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

[English](./README.md) | [中文](./README-zh.md)

Modern Web UI for [Spring AI Alibaba JManus](../README.md) - AI-Powered Task Automation Platform.

## ✨ Features

- 🎨 **Linear Design**: Dark theme with cards and glow effects
- 💬 **v0.dev-like Interaction**: Intuitive conversation-to-plan workflow
- 🤖 **AI Chat Interface**: Real-time conversation with thinking states
- 📝 **Dual Preview**: Code editor and chat bubbles for different output types
- 🎯 **Task Planning**: Step-by-step execution with progress tracking
- 🌐 **Internationalization**: Support for multiple languages
- 📱 **Responsive Design**: Works on desktop and mobile devices

## 🚀 Quick Start

### Prerequisites

- Node.js >= 16 (comes with npm)
- pnpm (recommended)

**For Java Developers:**

#### 1. Install Node.js and npm

Node.js is a JavaScript runtime that includes npm (Node Package Manager).

- **Windows/macOS**: 
  Visit [Node.js official website](https://nodejs.org/) and download the LTS version installer for your operating system.
- **Linux**: 
  Use your package manager. For Ubuntu/Debian:

  ```bash
  sudo apt update
  sudo apt install nodejs npm
  ```

Verify installation:

```bash
node -v
npm -v
```

#### 2. Install pnpm (Recommended)

pnpm is a fast, disk space efficient package manager.

```bash
npm install -g pnpm
```

Verify installation:

```bash
pnpm -v
```

### Installation

```bash
# Clone the repository (if not already cloned)
# git clone https://github.com/spring-ai-alibaba/spring-ai-alibaba.git

# Navigate to UI directory
# cd spring-ai-alibaba-jmanus/ui-vue3

# Install dependencies
pnpm install

# Start development server
pnpm run dev
```

### Build

```bash
# Build for production
pnpm run build

# Preview production build
pnpm run preview
```

## 🛠️ Development

### Available Scripts

- `pnpm run dev` - Start development server
- `pnpm run build` - Build for production
- `pnpm run preview` - Preview production build
- `pnpm run type-check` - Run TypeScript type checking
- `pnpm run lint` - Run ESLint
- `pnpm run format` - Format code with Prettier
- `pnpm run test:unit` - Run unit tests
- `pnpm run test:e2e` - Run end-to-end tests

### Project Structure

```
src/
├── components/          # Reusable components
│   └── editor/         # Monaco Editor component
├── layout/             # Layout components
├── views/              # Page components
│   ├── conversation/   # Main conversation page
│   ├── plan/          # Task planning page
│   └── error/         # Error pages
├── router/            # Vue Router configuration
├── base/              # Base utilities
│   ├── i18n/         # Internationalization
│   ├── http/         # HTTP client
│   └── constants.ts  # Constants
└── utils/            # Utility functions
```

### Technology Stack

- **Vue 3** - Progressive JavaScript framework
- **TypeScript** - Type-safe JavaScript
- **Vite** - Fast build tool
- **Vue Router** - Official router for Vue.js
- **Pinia** - State management
- **Ant Design Vue** - UI component library
- **Monaco Editor** - Code editor
- **Iconify** - Icon framework
- **i18n** - Internationalization

## 🎨 Design System

### Color Palette

- **Primary**: Linear gradient from `#667eea` to `#764ba2`
- **Background**: `#0a0a0a` (Dark)
- **Surface**: `rgba(255, 255, 255, 0.05)` (Glass effect)
- **Border**: `rgba(255, 255, 255, 0.1)`
- **Text**: `#ffffff` (Primary), `#888888` (Secondary)

### Components

- **Cards**: Glass morphism with backdrop blur
- **Buttons**: Gradient backgrounds with hover effects
- **Inputs**: Transparent with focus states
- **Animations**: Smooth transitions and floating effects

## 🌐 Configuration

The UI can be configured through environment variables:

```env
# API Configuration
VITE_API_BASE_URL=http://localhost:8080

# Other configurations...
```

## 📖 Documentation

For detailed documentation, please refer to:

- [JManus Documentation](../README.md)
- [API Documentation](./docs/api.md)
- [Configuration Guide](./docs/configuration.md)

## 🤝 Contributing

We welcome contributions! Please read our [Contributing Guide](../../CONTRIBUTING.md) before submitting a Pull Request.

### Development Setup

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](../../LICENSE) file for details.

## 🙏 Acknowledgments

- [Vue.js](https://vuejs.org/) - The Progressive JavaScript Framework
- [Ant Design Vue](https://antdv.com/) - Enterprise-class UI components
- [Monaco Editor](https://microsoft.github.io/monaco-editor/) - Code editor
- [Linear](https://linear.app/) - Design inspiration
- [v0.dev](https://v0.dev/) - Interaction pattern inspiration
