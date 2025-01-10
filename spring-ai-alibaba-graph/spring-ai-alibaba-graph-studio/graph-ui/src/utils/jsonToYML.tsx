import { stringify } from 'yaml';

export function jsonToYML(data: any, filename = 'data.yml') {
  // 将数据转换为 YAML 格式
  const yamlContent = stringify(data);

  // 创建一个 Blob 对象
  const blob = new Blob([yamlContent], { type: 'text/yaml;charset=utf-8' });

  // 创建一个临时的链接用于下载文件
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = filename;

  // 触发下载
  document.body.appendChild(link);
  link.click();

  // 移除链接
  document.body.removeChild(link);
}
