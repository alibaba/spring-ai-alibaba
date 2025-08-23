import React, { createContext, useContext, useState, ReactNode } from 'react';

interface DebugInfo {
  timestamp: Date;
  level: 'info' | 'warning' | 'error';
  message: string;
  data?: any;
}

interface ConnectionStatus {
  status: 'connected' | 'disconnected' | 'connecting';
  lastConnected?: Date;
  errorMessage?: string;
}

interface DebugState {
  debugLogs: DebugInfo[];
  connectionStatus: ConnectionStatus;
  metrics: {
    messagesCount: number;
    averageResponseTime: number;
    errorCount: number;
    lastActivity: Date | null;
  };
}

interface DebugContextValue {
  debugState: DebugState;
  addDebugLog: (log: Omit<DebugInfo, 'timestamp'>) => void;
  updateConnectionStatus: (status: Partial<ConnectionStatus>) => void;
  updateMetrics: (metrics: Partial<DebugState['metrics']>) => void;
  clearLogs: () => void;
}

const initialDebugState: DebugState = {
  debugLogs: [],
  connectionStatus: {
    status: 'connected',
    lastConnected: new Date(),
  },
  metrics: {
    messagesCount: 0,
    averageResponseTime: 0,
    errorCount: 0,
    lastActivity: null,
  },
};

const DebugContext = createContext<DebugContextValue | undefined>(undefined);

export const DebugProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [debugState, setDebugState] = useState<DebugState>(initialDebugState);

  const addDebugLog = (log: Omit<DebugInfo, 'timestamp'>) => {
    setDebugState(prev => ({
      ...prev,
      debugLogs: [
        { ...log, timestamp: new Date() },
        ...prev.debugLogs.slice(0, 99), // Keep only last 100 logs
      ],
    }));
  };

  const updateConnectionStatus = (status: Partial<ConnectionStatus>) => {
    setDebugState(prev => ({
      ...prev,
      connectionStatus: { ...prev.connectionStatus, ...status },
    }));
  };

  const updateMetrics = (metrics: Partial<DebugState['metrics']>) => {
    setDebugState(prev => ({
      ...prev,
      metrics: { ...prev.metrics, ...metrics, lastActivity: new Date() },
    }));
  };

  const clearLogs = () => {
    setDebugState(prev => ({ ...prev, debugLogs: [] }));
  };

  const value: DebugContextValue = {
    debugState,
    addDebugLog,
    updateConnectionStatus,
    updateMetrics,
    clearLogs,
  };

  return <DebugContext.Provider value={value}>{children}</DebugContext.Provider>;
};

export const useDebugContext = (): DebugContextValue => {
  const context = useContext(DebugContext);
  if (context === undefined) {
    throw new Error('useDebugContext must be used within a DebugProvider');
  }
  return context;
};
