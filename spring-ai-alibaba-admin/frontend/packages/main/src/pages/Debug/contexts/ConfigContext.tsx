import React, { createContext, useContext, useState, ReactNode } from 'react';

interface ModelConfig {
  model: string;
  temperature: number;
  maxTokens: number;
  topP: number;
  frequencyPenalty: number;
  presencePenalty: number;
}

interface ConfigState {
  modelConfig: ModelConfig;
  showToolCalls: boolean;
  showDebugInfo: boolean;
  autoScroll: boolean;
  theme: 'light' | 'dark';
}

interface ConfigContextValue {
  config: ConfigState;
  updateModelConfig: (config: Partial<ModelConfig>) => void;
  toggleToolCalls: () => void;
  toggleDebugInfo: () => void;
  toggleAutoScroll: () => void;
  setTheme: (theme: 'light' | 'dark') => void;
}

const defaultConfig: ConfigState = {
  modelConfig: {
    model: 'qwen-plus',
    temperature: 0.7,
    maxTokens: 2048,
    topP: 1,
    frequencyPenalty: 0,
    presencePenalty: 0,
  },
  showToolCalls: true,
  showDebugInfo: false,
  autoScroll: true,
  theme: 'light',
};

const ConfigContext = createContext<ConfigContextValue | undefined>(undefined);

export const ConfigProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [config, setConfig] = useState<ConfigState>(defaultConfig);

  const updateModelConfig = (newConfig: Partial<ModelConfig>) => {
    setConfig(prev => ({
      ...prev,
      modelConfig: { ...prev.modelConfig, ...newConfig },
    }));
  };

  const toggleToolCalls = () => {
    setConfig(prev => ({ ...prev, showToolCalls: !prev.showToolCalls }));
  };

  const toggleDebugInfo = () => {
    setConfig(prev => ({ ...prev, showDebugInfo: !prev.showDebugInfo }));
  };

  const toggleAutoScroll = () => {
    setConfig(prev => ({ ...prev, autoScroll: !prev.autoScroll }));
  };

  const setTheme = (theme: 'light' | 'dark') => {
    setConfig(prev => ({ ...prev, theme }));
  };

  const value: ConfigContextValue = {
    config,
    updateModelConfig,
    toggleToolCalls,
    toggleDebugInfo,
    toggleAutoScroll,
    setTheme,
  };

  return <ConfigContext.Provider value={value}>{children}</ConfigContext.Provider>;
};

export const useConfigContext = () => {
  const context = useContext(ConfigContext);
  if (context === undefined) {
    throw new Error('useConfigContext must be used within a ConfigProvider');
  }
  return context;
};
