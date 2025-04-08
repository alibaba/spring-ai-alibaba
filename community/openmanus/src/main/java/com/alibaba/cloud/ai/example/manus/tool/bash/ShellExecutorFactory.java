/**
 * Copyright (C) 2024 AIDC-AI
 */
package com.alibaba.cloud.ai.example.manus.tool.bash;

/**
 * Shell执行器工厂类
 * 负责创建对应操作系统的Shell执行器
 */
public class ShellExecutorFactory {
    
    /**
     * 创建对应当前操作系统的Shell执行器
     * @return ShellCommandExecutor实现
     */
    public static ShellCommandExecutor createExecutor() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return new WindowsShellExecutor();
        } else if (os.contains("mac")) {
            return new MacShellExecutor();
        } else {
            return new LinuxShellExecutor();
        }
    }
    
    /**
     * 创建指定操作系统类型的Shell执行器
     * @param osType 操作系统类型：windows/mac/linux
     * @return ShellCommandExecutor实现
     */
    public static ShellCommandExecutor createExecutor(String osType) {
        switch (osType.toLowerCase()) {
            case "windows":
                return new WindowsShellExecutor();
            case "mac":
                return new MacShellExecutor();
            case "linux":
                return new LinuxShellExecutor();
            default:
                throw new IllegalArgumentException("Unsupported OS type: " + osType);
        }
    }
}
