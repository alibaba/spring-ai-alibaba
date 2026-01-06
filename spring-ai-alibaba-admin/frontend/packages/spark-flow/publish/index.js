const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const innerPackage = require('../package.json');
const publicPackage = require('./package.json');

const innerPackageBakPath = path.join(__dirname, '../package.json.bk');
const publicPackageTempPath = path.join(__dirname, '../package.json');

// 备份原始 package.json
fs.writeFileSync(
  innerPackageBakPath,
  JSON.stringify(innerPackage, null, 2)
);

// 创建新的 package.json，正确替换版本号
const updatedPublicPackage = {
  ...publicPackage,
  version: innerPackage.version
};

fs.writeFileSync(
  publicPackageTempPath,
  JSON.stringify(updatedPublicPackage, null, 2)
);

try {
  execSync('npm publish', { stdio: 'inherit' });
  console.log('Publish completed successfully');
} catch (error) {
  console.error('Publish failed:', error);
}

// 清理临时文件并恢复原始文件
fs.unlinkSync(publicPackageTempPath);
fs.renameSync(innerPackageBakPath, path.join(__dirname, '../package.json'));