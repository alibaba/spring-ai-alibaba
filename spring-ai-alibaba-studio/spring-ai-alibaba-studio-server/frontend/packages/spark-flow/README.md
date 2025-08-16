# Spark Flow

## Project Introduction
Spark Flow is a Flow editing basic business component provided for the main package `packages/main`. The main technology stack includes React, React Flow, Antd, zustand, etc.

### Main Features
+ Node checklist;
+ Node manager;
+ Flow execution state management;
+ Support undo and redo;
+ Support rapid customization of business nodes throughout the lifecycle;
+ Support complex node interactions;

### Project Structure
```plain
spark-flow/
├── docs/
├── public/                # Static resource files
├── src/               
│   ├── components/        # Basic components
│   ├── constant/          # Visual workflow editor
│   ├── demos/             # Internationalization support
│   ├── flow/              # Flow component main entry
│   ├── hooks/             # Flow operation utility library
│   ├── i18n/              # Internationalization
│   ├── store/             # Global state management
│   ├── types/             # Type definitions
|		├── utils/             # Function utility library 
|		├── index.less/        # Less variable definitions
|		└── index.ts/          # Project main entry file
└── package.json           # Project configuration
```

#### Node Structure
```plain
[Business Node Name]
├── node                 # Flow node rendering
├── panel                # Node configuration panel
└── schema               # Node Schema protocol configuration
```

## How to Develop
### Quick Start
```shell
npm run re-install && cd packages/spark-flow && npm start
```

+ **Install Dependencies** Execute `npm run re-install` in the root directory
+ **Run** cd `packages/spark-flow` and execute `npm start`

### Development
> [!NOTE]  
> **Note:** Prerequisite is that you have completed the **Install Dependencies** operation from Quick Start

+ After development, execute `npm run fresh:flow` in the root directory to quickly clear the dependencies of the main package `packages/main`;
+ Enter the main package `packages/main` and execute `npm start`
+ Perform testing & verification;