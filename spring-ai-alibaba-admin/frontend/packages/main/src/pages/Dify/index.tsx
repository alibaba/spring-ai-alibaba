import InnerLayout from '@/components/InnerLayout';
import $i18n from '@/i18n';
import { convertDifyToSpringAI } from '@/services/difyConverter';
import { Button, Upload, message } from 'antd';
import { InboxOutlined } from '@ant-design/icons';
import { useRequest } from 'ahooks';
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styles from './index.module.less';

const { Dragger } = Upload;

const DifyConverter: React.FC = () => {
  const navigate = useNavigate();
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [convertResult, setConvertResult] = useState<string[]>([]);

  const { loading: converting, runAsync } = useRequest(convertDifyToSpringAI, {
    manual: true,
  });

  const handleFileChange = (info: any) => {
    const { status, file } = info;

    // 由于我们在 beforeUpload 中返回 false，文件不会真正上传
    // 所以我们需要在这里直接处理文件选择
    if (file) {
      setSelectedFile(file.originFileObj || file);
      message.success(`${file.name} 文件选择成功`);
    }
  };

  const handleBeforeUpload = (file: File) => {
    // 检查文件类型
    const isYaml = file.type === 'application/x-yaml' ||
                   file.type === 'text/yaml' ||
                   file.name.endsWith('.yaml') ||
                   file.name.endsWith('.yml');
    if (!isYaml) {
      message.error('只支持 YAML 格式的 Dify DSL 文件！');
      return false;
    }

    // 直接设置选中的文件，因为我们要阻止自动上传
    setSelectedFile(file);
    message.success(`${file.name} 文件选择成功`);

    // 阻止自动上传，只做文件选择
    return false;
  };

  const handleConvert = async () => {
    if (!selectedFile) {
      message.warning('请先选择 Dify DSL 文件');
      return;
    }

    try {
      // 读取用户上传文件的原始内容
      const fileContent = await readFileContent(selectedFile);

      // 准备请求参数
      const params = {
        dependencies: 'spring-ai-alibaba-graph,web,spring-ai-alibaba-starter-dashscope',
        appMode: 'workflow',
        dslDialectType: 'dify',
        type: 'maven-project',
        language: 'java',
        bootVersion: '3.5.0',
        baseDir: 'demo',
        groupId: 'com.example',
        artifactId: 'demo',
        name: 'demo',
        description: 'Demo project for Spring Boot',
        packageName: 'com.example.demo',
        packaging: 'jar',
        javaVersion: '17',
        dsl: fileContent,
      };

      // 调用转换服务
      const response = await runAsync(params);

      // 处理 zip 文件下载
      const blob = response.data;
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'spring-ai-alibaba-demo.zip';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);

      message.success('转换成功！项目文件已开始下载');
      setConvertResult([
        'Spring AI Alibaba 项目已生成',
        '项目类型: Maven 项目',
        '语言: Java 17',
        '包含依赖: spring-ai-alibaba-graph, web, spring-ai-alibaba-starter-dashscope',
        '应用模式: workflow'
      ]);

    } catch (error) {
      console.error('转换失败:', error);
      message.error(`转换失败：${error.message || '请重试'}`);
    }
  };

  // 读取文件内容的辅助函数
  const readFileContent = (file: File): Promise<string> => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = (e) => {
        const content = e.target?.result as string;
        resolve(content);
      };
      reader.onerror = () => {
        reject(new Error('文件读取失败'));
      };
      reader.readAsText(file, 'utf-8');
    });
  };

  const handleGoBack = () => {
    navigate('/');
  };

  return (
    <InnerLayout
      breadcrumbLinks={[
        {
          title: $i18n.get({
            id: 'main.pages.App.index.home',
            dm: '首页',
          }),
          path: '/',
        },
        {
          title: 'DIFY 应用转换',
        },
      ]}
    >
      <div className={styles.container}>
        <div className={styles.header}>
          <h2>DIFY 应用转换为 Spring AI Alibaba 工程</h2>
        </div>

        {/* 说明区域 */}
        <div className={styles.description}>
          <h3>操作说明</h3>
          <div className={styles.instructionList}>
            <div className={styles.instruction}>
              <span className={styles.step}>1.</span>
              <span>从 Dify 平台导出您的智能体应用的 DSL 配置文件（YAML 格式）</span>
            </div>
            <div className={styles.instruction}>
              <span className={styles.step}>2.</span>
              <span>将 DSL 文件拖拽到下方文件选择区域，或点击选择文件</span>
            </div>
            <div className={styles.instruction}>
              <span className={styles.step}>3.</span>
              <span>点击"开始转换"按钮，系统将自动解析析 DSL 并生成 Spring AI Alibaba 项目</span>
            </div>
            <div className={styles.instruction}>
              <span className={styles.step}>4.</span>
              <span>转换完成后，您可以下载生成的项目源码并导入 IDE 进行开发</span>
            </div>
          </div>
        </div>

        {/* 文件选择区域 */}
        <div className={styles.uploadSection}>
          <h3>选择 Dify DSL 文件</h3>
          <Dragger
            name="file"
            multiple={false}
            beforeUpload={handleBeforeUpload}
            onChange={handleFileChange}
            className={styles.uploader}
            accept=".yaml,.yml"
          >
            <p className="ant-upload-drag-icon">
              <InboxOutlined />
            </p>
            <p className="ant-upload-text">点击或拖拽 Dify DSL 文件到此区域</p>
            <p className="ant-upload-hint">
              支持 YAML 格式的 Dify DSL 配置文件（.yaml 或 .yml）
            </p>
          </Dragger>

          {selectedFile && (
            <div className={styles.selectedFile}>
              <span>已选择文件：</span>
              <span className={styles.fileName}>{selectedFile.name}</span>
            </div>
          )}
        </div>

        {/* 转换按钮 */}
        <div className={styles.actionSection}>
          <Button
            type="primary"
            size="large"
            loading={converting}
            disabled={!selectedFile}
            onClick={handleConvert}
            className={styles.convertButton}
          >
            {converting ? '转换中...' : '开始转换'}
          </Button>
        </div>

        {/* 结果显示区域 */}
        {convertResult.length > 0 && (
          <div className={styles.resultSection}>
            <h3>转换结果</h3>
            <div className={styles.resultContent}>
              <p className={styles.successText}>✅ 转换成功！生成的文件如下：</p>
              <div className={styles.fileList}>
                {convertResult.map((filePath, index) => (
                  <div key={index} className={styles.fileItem}>
                    <span className={styles.filePath}>{filePath}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}
      </div>
    </InnerLayout>
  );
};

export default DifyConverter;
