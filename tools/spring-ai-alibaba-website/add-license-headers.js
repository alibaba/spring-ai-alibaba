/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

// 获取当前文件的目录路径
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// 许可证头部模板
const licenseHeader = `/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
`;

// 文件列表 - 这些是需要添加许可证头部的文件
const files = [
  '.astro/icon.d.ts',
  '.astro/types.d.ts',
  'animateSupport.js',
  'astro.config.mjs',
  'build-ebook.js',
  'fix-markdownName.js',
  'goat.config.ts',
  'postcss.config.cjs'
];

// 为每个文件添加许可证头部
for (const file of files) {
  const filePath = path.join(__dirname, file);
  
  try {
    // 检查文件是否存在
    if (fs.existsSync(filePath)) {
      // 读取文件内容
      const content = await fs.promises.readFile(filePath, 'utf8');
      
      // 检查文件是否已经有许可证头部
      if (!content.includes('Copyright') && !content.includes('Licensed under')) {
        // 添加许可证头部
        const newContent = licenseHeader + content;
        
        // 写入文件
        await fs.promises.writeFile(filePath, newContent, 'utf8');
        console.log(`已添加许可证头部到: ${file}`);
      } else {
        console.log(`文件已有许可证头部: ${file}`);
      }
    } else {
      console.log(`文件不存在: ${file}`);
    }
  } catch (error) {
    console.error(`处理文件时出错 ${file}:`, error);
  }
}

// 为图片文件创建许可证文件
const imageDirectories = [
  'public/assets',
  'public/img/blog'
];

// 为每个图片目录创建LICENSE.txt文件
for (const dir of imageDirectories) {
  const dirPath = path.join(__dirname, dir);
  const licensePath = path.join(dirPath, 'LICENSE.txt');
  
  try {
    // 检查目录是否存在
    if (fs.existsSync(dirPath)) {
      // 创建许可证文件
      const licenseText = `Copyright 2024-2025 the original author or authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
`;
      
      // 写入许可证文件
      await fs.promises.writeFile(licensePath, licenseText, 'utf8');
      console.log(`已创建许可证文件: ${licensePath}`);
    } else {
      console.log(`目录不存在: ${dirPath}`);
    }
  } catch (error) {
    console.error(`创建许可证文件时出错 ${licensePath}:`, error);
  }
}

console.log('许可证头部添加完成！');