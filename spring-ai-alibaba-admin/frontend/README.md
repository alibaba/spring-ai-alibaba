![](https://gw.alicdn.com/imgextra/i4/O1CN01e5pj8L1K9T2t2TP1u_!!6000000001121-1-tps-1000-625.gif)

<p align="center">English | <a href="./README.zh-CN.md">中文</a></p>

## 📋 Project Overview
frontend-monorepo is a frontend project based on Monorepo architecture, mainly consisting of two core functional modules: the workbench application and the visual workflow editor. The project adopts modern frontend technology stack and supports multilingual internationalization.

### 📂 Project Structure
```
frontend/
├── packages/                # Monorepo subprojects
│   ├── main/                # Main workbench application
│   ├── spark-flow/          # Visual workflow editor
│   └── spark-i18n/          # Internationalization support
└── package.json             # Root project configuration
```

## 💎 Core Modules
### 🖥️ main (Main Workbench)
- Single-page application built with Umi 4
- Main functional modules:
  - Workflow editor integration
  - Agent management
  - MCP management
  - Plugin management
  - Knowledge base management
  - Model service configuration
- Technical features:
  - Uses TailwindCSS for styling
  - Integrates spark-flow as workflow editor
  - Supports both Python and Java backend modes
  - Supports internationalization

### 🎨 spark-flow (Workflow Editor)
- Core features:
  - Visual node editing
  - Node type system (start/end/LLM/script/plugin etc.)
  - Process testing and debugging
  - Version management
- Technical implementation:
  - Flowchart rendering based on XYFlow (React Flow)
  - Uses ELK.js for automatic layout
  - State management with Zustand
  - Supports internationalization

### 🌐 spark-i18n (Internationalization)
- Provides multilingual support
- Automated translation toolchain
- Supports Chinese-English switching

## ⚡ Quick Start

### Run from Source

Before starting the web frontend service, please ensure the following environment is ready:
- [Node.js](https://nodejs.org) >= v20


First, install the required dependencies in the root directory:

```bash
npm install rimraf copyfiles cross-env --save-dev
```

Then, install all project dependencies:

```bash
npm run re-install
```

Then, configure environment variables. Create a file named `.env` in the current directory and copy contents from `.env.example`. Modify these environment variable values according to your needs:

```bash
cd packages/main
```
On macOS/Linux:
```bash
cp .env.example .env
```
On Windows (PowerShell):
```bash
Copy-Item .env.example .env
```


```
# Web server URL
WEB_SERVER="http://127.0.0.1:8080"

# Backend type, e.g., java, python
BACK_END="java"

# Default username for application login
DEFAULT_USERNAME=saa

# Default password for application login
DEFAULT_PASSWORD=123456
```

Finally, run the development server:

```bash
cd main
npm run dev
```

Open [http://localhost:8000](http://localhost:8000) in your browser to view the result.

You can start editing files in the `main/src` folder. The page will automatically update when you edit files.

## 🚢 Deployment

```bash
# If backend is java
npm run build:subtree:java
```

After the command completes, it will generate a `./packages/main/dist` directory, which you can then deploy to your server.

## 📦 Additional Dependencies
- @spark-ai/design
  - [https://www.npmjs.com/package/@spark-ai/design](https://www.npmjs.com/package/@spark-ai/design)
- @spark-ai/chat
  - [https://www.npmjs.com/package/@spark-ai/chat](https://www.npmjs.com/package/@spark-ai/chat)
