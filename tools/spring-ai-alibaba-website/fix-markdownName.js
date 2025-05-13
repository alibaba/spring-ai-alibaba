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
import fs from "fs";
import path from "path";
import { fileURLToPath } from 'url';

const curFilename = fileURLToPath(import.meta.url);
const curDirname = path.dirname(curFilename);

function renameFiles(directory) {
    // 检查目录是否存在
    if (!fs.existsSync(directory)) {
        console.error(`目录不存在: ${directory}`);
        return;
    }

    // 读取目录中的所有文件和子目录
    fs.readdir(directory, { withFileTypes: true }, (err, files) => {
        if (err) {
            console.error('读取目录时出错:', err);
            return;
        }

        files.forEach((file) => {
            if (file.isFile() && file.name.startsWith('user-question-history')) {
                // 构建当前文件的完整路径
                const oldPath = path.join(directory, file.name);
                // 构建新文件名（添加前缀"SCA-"，更改后缀为".mdx"）
                const newName = `SCA-${file.name.replace(/\.([a-z]+)$/, '')}.mdx`; // 假设原文件有一个非空的扩展名，并将其替换为.mdx
                const newPath = path.join(directory, newName);

                // 重命名文件
                fs.rename(oldPath, newPath, (error) => {
                    if (error) {
                        console.error(`重命名文件失败: ${oldPath} -> ${newPath}`, error);
                    } else {
                        console.log(`文件已成功重命名为: ${newPath}`);
                    }
                });
            }
        });
    });
}

// 调用函数，传入目标目录路径
renameFiles(path.join(curDirname, 'src/content/blog/faq'));
