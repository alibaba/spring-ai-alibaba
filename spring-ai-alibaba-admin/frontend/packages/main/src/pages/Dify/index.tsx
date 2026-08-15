import InnerLayout from '@/components/InnerLayout';
import $i18n from '@/i18n';
import { convertDifyToSpringAI } from '@/services/difyConverter';
import { Button, Upload, message } from 'antd';
import { InboxOutlined } from '@ant-design/icons';
import { useRequest } from 'ahooks';
import React, { useState } from 'react';
import styles from './index.module.less';

const { Dragger } = Upload;

const DifyConverter: React.FC = () => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [convertResult, setConvertResult] = useState<string[]>([]);

  const { loading: converting, runAsync } = useRequest(convertDifyToSpringAI, {
    manual: true,
  });

  const handleFileChange = (info: any) => {
    const { file } = info;

    // beforeUpload returns false, so we handle file selection here
    if (file) {
      setSelectedFile(file.originFileObj || file);
      message.success(
        $i18n.get(
          {
            id: 'main.pages.Dify.index.fileSelectedSuccess',
            dm: '{name} 文件选择成功',
          },
          { name: file.name },
        ),
      );
    }
  };

  const handleBeforeUpload = (file: File) => {
    const isYaml =
      file.type === 'application/x-yaml' ||
      file.type === 'text/yaml' ||
      file.name.endsWith('.yaml') ||
      file.name.endsWith('.yml');
    if (!isYaml) {
      message.error(
        $i18n.get({
          id: 'main.pages.Dify.index.onlyYamlSupported',
          dm: '只支持 YAML 格式的 Dify DSL 文件！',
        }),
      );
      return false;
    }

    setSelectedFile(file);
    message.success(
      $i18n.get(
        {
          id: 'main.pages.Dify.index.fileSelectedSuccess',
          dm: '{name} 文件选择成功',
        },
        { name: file.name },
      ),
    );

    // Prevent auto upload; selection only
    return false;
  };

  const handleConvert = async () => {
    if (!selectedFile) {
      message.warning(
        $i18n.get({
          id: 'main.pages.Dify.index.selectFileFirst',
          dm: '请先选择 Dify DSL 文件',
        }),
      );
      return;
    }

    try {
      const fileContent = await readFileContent(selectedFile);

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

      const response = await runAsync(params);

      const blob = response.data;
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'spring-ai-alibaba-demo.zip';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);

      message.success(
        $i18n.get({
          id: 'main.pages.Dify.index.convertSuccessDownload',
          dm: '转换成功！项目文件已开始下载',
        }),
      );
      setConvertResult([
        $i18n.get({
          id: 'main.pages.Dify.index.resultProjectGenerated',
          dm: 'Spring AI Alibaba 项目已生成',
        }),
        $i18n.get({
          id: 'main.pages.Dify.index.resultProjectType',
          dm: '项目类型: Maven 项目',
        }),
        $i18n.get({
          id: 'main.pages.Dify.index.resultLanguage',
          dm: '语言: Java 17',
        }),
        $i18n.get({
          id: 'main.pages.Dify.index.resultDependencies',
          dm: '包含依赖: spring-ai-alibaba-graph, web, spring-ai-alibaba-starter-dashscope',
        }),
        $i18n.get({
          id: 'main.pages.Dify.index.resultAppMode',
          dm: '应用模式: workflow',
        }),
      ]);
    } catch (error: any) {
      console.error('Conversion failed:', error);
      message.error(
        $i18n.get(
          {
            id: 'main.pages.Dify.index.convertFailed',
            dm: '转换失败：{message}',
          },
          {
            message:
              error?.message ||
              $i18n.get({
                id: 'main.pages.Dify.index.pleaseRetry',
                dm: '请重试',
              }),
          },
        ),
      );
    }
  };

  const readFileContent = (file: File): Promise<string> => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = (e) => {
        const content = e.target?.result as string;
        resolve(content);
      };
      reader.onerror = () => {
        reject(
          new Error(
            $i18n.get({
              id: 'main.pages.Dify.index.fileReadFailed',
              dm: '文件读取失败',
            }),
          ),
        );
      };
      reader.readAsText(file, 'utf-8');
    });
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
          title: $i18n.get({
            id: 'main.pages.Dify.index.breadcrumb',
            dm: 'DIFY 应用转换',
          }),
        },
      ]}
    >
      <div className={styles.container}>
        <div className={styles.header}>
          <h2>
            {$i18n.get({
              id: 'main.pages.Dify.index.heading',
              dm: 'DIFY 应用转换为 Spring AI Alibaba 工程',
            })}
          </h2>
        </div>

        <div className={styles.description}>
          <h3>
            {$i18n.get({
              id: 'main.pages.Dify.index.instructionsTitle',
              dm: '操作说明',
            })}
          </h3>
          <div className={styles.instructionList}>
            <div className={styles.instruction}>
              <span className={styles.step}>1.</span>
              <span>
                {$i18n.get({
                  id: 'main.pages.Dify.index.instruction1',
                  dm: '从 Dify 平台导出您的智能体应用的 DSL 配置文件（YAML 格式）',
                })}
              </span>
            </div>
            <div className={styles.instruction}>
              <span className={styles.step}>2.</span>
              <span>
                {$i18n.get({
                  id: 'main.pages.Dify.index.instruction2',
                  dm: '将 DSL 文件拖拽到下方文件选择区域，或点击选择文件',
                })}
              </span>
            </div>
            <div className={styles.instruction}>
              <span className={styles.step}>3.</span>
              <span>
                {$i18n.get({
                  id: 'main.pages.Dify.index.instruction3',
                  dm: '点击"开始转换"按钮，系统将自动解析 DSL 并生成 Spring AI Alibaba 项目',
                })}
              </span>
            </div>
            <div className={styles.instruction}>
              <span className={styles.step}>4.</span>
              <span>
                {$i18n.get({
                  id: 'main.pages.Dify.index.instruction4',
                  dm: '转换完成后，您可以下载生成的项目源码并导入 IDE 进行开发',
                })}
              </span>
            </div>
          </div>
        </div>

        <div className={styles.uploadSection}>
          <h3>
            {$i18n.get({
              id: 'main.pages.Dify.index.selectFileTitle',
              dm: '选择 Dify DSL 文件',
            })}
          </h3>
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
            <p className="ant-upload-text">
              {$i18n.get({
                id: 'main.pages.Dify.index.uploadText',
                dm: '点击或拖拽 Dify DSL 文件到此区域',
              })}
            </p>
            <p className="ant-upload-hint">
              {$i18n.get({
                id: 'main.pages.Dify.index.uploadHint',
                dm: '支持 YAML 格式的 Dify DSL 配置文件（.yaml 或 .yml）',
              })}
            </p>
          </Dragger>

          {selectedFile && (
            <div className={styles.selectedFile}>
              <span>
                {$i18n.get({
                  id: 'main.pages.Dify.index.selectedFileLabel',
                  dm: '已选择文件：',
                })}
              </span>
              <span className={styles.fileName}>{selectedFile.name}</span>
            </div>
          )}
        </div>

        <div className={styles.actionSection}>
          <Button
            type="primary"
            size="large"
            loading={converting}
            disabled={!selectedFile}
            onClick={handleConvert}
            className={styles.convertButton}
          >
            {converting
              ? $i18n.get({
                  id: 'main.pages.Dify.index.converting',
                  dm: '转换中...',
                })
              : $i18n.get({
                  id: 'main.pages.Dify.index.startConvert',
                  dm: '开始转换',
                })}
          </Button>
        </div>

        {convertResult.length > 0 && (
          <div className={styles.resultSection}>
            <h3>
              {$i18n.get({
                id: 'main.pages.Dify.index.resultTitle',
                dm: '转换结果',
              })}
            </h3>
            <div className={styles.resultContent}>
              <p className={styles.successText}>
                {$i18n.get({
                  id: 'main.pages.Dify.index.resultSuccessText',
                  dm: '✅ 转换成功！生成的文件如下：',
                })}
              </p>
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
