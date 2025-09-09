const fs = require('fs');
const path = require('path');

const sourceFile = path.join(__dirname, '../tailwind.css');
const destDir = path.join(__dirname, '../dist');
const destFile = path.join(destDir, 'tailwind.css');

// 确保目标目录存在
if (!fs.existsSync(destDir)) {
  fs.mkdirSync(destDir, { recursive: true });
}

// 复制文件
fs.copyFileSync(sourceFile, destFile);
console.log(`Copied ${sourceFile} to ${destFile}`);