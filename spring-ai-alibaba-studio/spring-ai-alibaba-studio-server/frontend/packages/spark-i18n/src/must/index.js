const fs = require('fs-extra');
const find = require('find');
const parseJSX = require('./parse-jsx');
const dotenv = require('dotenv');
const config = dotenv.config();
const { DASH_SCOPE_API_KEY, DASH_SCOPE_MUST_VALUE_TRANSLATE_APP_ID, DASH_SCOPE_MUST_KEY_TRANSLATE_APP_ID } = config.parsed;
const axios = require('axios');
const babylon = require('@babel/parser');
const traverse = require('@babel/traverse').default;
const generate = require('@babel/generator').default;
const t = require('@babel/types');
const readline = require('readline');

// Add import statement to file
const injectImportStatement = (filePath, importPath) => {
    try {
        const content = fs.readFileSync(filePath, 'utf8');
        const importStatement = `import $i18n from '${importPath}';\n`;
        
        // Check if the file already has this import statement
        if (!content.includes(importStatement.trim())) {
            // Check if the file has other import statements
            const importRegex = /^import\s+.*?from\s+['"].*?['"];?\n/gm;
            const imports = content.match(importRegex);
            
            if (imports) {
                // Add after the last import statement
                const lastImport = imports[imports.length - 1];
                const newContent = content.replace(lastImport, `${lastImport}${importStatement}`);
                fs.writeFileSync(filePath, newContent, 'utf8');
            } else {
                // If no import statements, add at the beginning
                fs.writeFileSync(filePath, importStatement + content, 'utf8');
            }
            console.log(`    # Added import statement to file: ${filePath}`);
        }
    } catch (error) {
        console.error(`    # Failed to add import statement: ${filePath}`, error);
    }
};

// Check if file should be ignored
const shouldIgnoreFile = (file, doNotTranslateFiles) => {
    if (!doNotTranslateFiles || !doNotTranslateFiles.length) return false;
    
    return doNotTranslateFiles.some(pattern => {
        if (typeof pattern === 'string') {
            return file.includes(pattern);
        } else if (pattern instanceof RegExp) {
            return pattern.test(file);
        }
        return false;
    });
};

const callDashScope = async (fileContent, tokenName, appId, retryCount = 3) => {
    const url = `https://dashscope.aliyuncs.com/api/v1/apps/${appId}/completion`;
    const data = {
        input: {
            prompt: fileContent,
            tokenName
        }
    };

    for (let i = 0; i < retryCount; i++) {
        try {
            const response = await axios.post(url, data, {
                headers: {
                    'Authorization': `Bearer ${DASH_SCOPE_API_KEY}`,
                    'Content-Type': 'application/json'
                }
            });

            if (response.status === 200) {
                return response.data.output.text;
            } else {
                console.log(`request_id=${response.headers['request_id']}`);
                console.log(`code=${response.status}`);
                console.log(`message=${response.data.message}`);
                
                if (i < retryCount - 1) {
                    console.log(`    # 第${i + 1}次请求失败，10 秒后重试...`);
                    await new Promise(resolve => setTimeout(resolve, 10000));
                }
            }
        } catch (error) {
            console.error(`Error calling DashScope: ${error.message}`);
            if (error.response) {
                console.error(`Response status: ${error.response.status}`);
                console.error(`Response data: ${JSON.stringify(error.response.data, null, 2)}`);
            }
            
            if (i < retryCount - 1) {
                console.log(`    # 第${i + 1}次请求失败，10 秒后重试...`);
                await new Promise(resolve => setTimeout(resolve, 10000));
            }
        }
    }
    
    throw new Error(`Failed after ${retryCount} retries`);
}

// Batch process key translation
const batchKeyTranslate = async (i18n) => {
    const entries = Object.entries(i18n);
    const batchSize = 100;
    const batches = [];
    
    // Split content into batches of 100
    for (let i = 0; i < entries.length; i += batchSize) {
        batches.push(entries.slice(i, i + batchSize));
    }
    
    console.log(`    # Total ${batches.length} batches to translate`);
    const keyMap = {};
    const zhCN = {};
    
    // Process each batch
    for (let i = 0; i < batches.length; i++) {
        const batch = batches[i];
        const batchContent = {
            type: 'i18n',
            content: Object.fromEntries(batch)
        };
        
        console.log(`    # Translating batch ${i + 1}/${batches.length}...`);
        const translatedText = await callDashScope(JSON.stringify(batchContent, null, 2), 'i18n', DASH_SCOPE_MUST_KEY_TRANSLATE_APP_ID);
        const translatedJson = extractJsonContent(translatedText);
        
        if (translatedJson.keyMap && translatedJson['zh-cn']){
            Object.assign(keyMap, translatedJson.keyMap);
            Object.assign(zhCN, translatedJson['zh-cn']);
        }
        // Pause 1 second between batches to avoid rate limiting
        if (i < batches.length - 1) {
            await new Promise(resolve => setTimeout(resolve, 1000));
        }
    }
    
    return { keyMap, zhCN };
}

// Batch process translation
const batchTranslate = async (i18n) => {
    const entries = Object.entries(i18n);
    const batchSize = 100;
    const batches = [];
    
    // Split content into batches of 100
    for (let i = 0; i < entries.length; i += batchSize) {
        batches.push(entries.slice(i, i + batchSize));
    }
    
    console.log(`    # Total ${batches.length} batches to translate`);
    const translatedI18n = {};
    
    // Process each batch
    for (let i = 0; i < batches.length; i++) {
        const batch = batches[i];
        const batchContent = Object.fromEntries(batch);
        
        let retryCount = 3;
        let success = false;
        
        while (retryCount > 0 && !success) {
            try {
                console.log(`    # Translating batch ${i + 1}/${batches.length}...${retryCount > 2 ? '' : `(Retry ${4-retryCount})`}`);
                const translatedText = await callDashScope(JSON.stringify(batchContent, null, 2), 'i18n', DASH_SCOPE_MUST_VALUE_TRANSLATE_APP_ID);
                
                const translatedJson = extractJsonContent(translatedText);
                if (translatedJson) {
                    Object.assign(translatedI18n, translatedJson);
                    success = true;
                } else {
                    throw new Error('Failed to parse translation result');
                }
            } catch (error) {
                retryCount--;
                if (retryCount > 0) {
                    console.log(`    # Translation failed, retrying in 10 seconds... Remaining attempts: ${retryCount}`);
                    await new Promise(resolve => setTimeout(resolve, 10000));
                } else {
                    console.error(`    # Batch ${i + 1} translation failed, skipping this batch`);
                    console.error('    # Error:', error.message);
                }
            }
        }
        
        // Pause 1 second between batches to avoid rate limiting
        if (i < batches.length - 1) {
            await new Promise(resolve => setTimeout(resolve, 1000));
        }
    }
    
    return translatedI18n;
}

// Replace file keys
const replaceKeysInFiles = async (path, keyMap, fileType) => {
    const fileTypes = fileType.split(',').map(s => s.trim());
    const re = new RegExp(`(${fileTypes.map(ft => ft.replace('.', '\\.')).join('|')})$`);
    const files = find.fileSync(re, path);
    
    console.log(`    # Starting to replace keys in ${files.length} files...`);
    
    for (const file of files) {
        try {
            const code = fs.readFileSync(file, 'utf8');
            
            // Parse file
            const ast = babylon.parse(code, {
                sourceType: 'module',
                allowImportExportEverywhere: true,
                plugins: [
                    // Basic plugins
                    'jsx',
                    'objectRestSpread',
                    'classProperties',
                    'functionBind',
                    
                    // TypeScript related plugins
                    'typescript',
                    
                    // Decorator plugins (legacy version only)
                    'decorators-legacy',
                    
                    // Dynamic import
                    'dynamicImport',
                    
                    // Class private properties
                    'classPrivateProperties',
                    'classPrivateMethods',
                    
                    // Optional chaining and nullish coalescing
                    'optionalChaining',
                    'nullishCoalescingOperator',
                ].filter(Boolean),
                tokens: true,
                errorRecovery: true,
            });
            
            let hasChanges = false;
            
            // Traverse and replace keys
            traverse(ast, {
                CallExpression(path) {
                    // Check if it's a $i18n.get call
                    if (
                        path.node.callee.type === 'MemberExpression' &&
                        path.node.callee.object &&
                        path.node.callee.object.name === '$i18n' &&
                        path.node.callee.property &&
                        path.node.callee.property.name === 'get'
                    ) {
                        const args = path.node.arguments;
                        // Check if first argument is an object expression
                        if (args.length > 0 && args[0].type === 'ObjectExpression') {
                            // Find id property
                            const idProperty = args[0].properties.find(
                                prop => prop.key && prop.key.name === 'id'
                            );
                            
                            // If id property found and is a string literal
                            if (idProperty && idProperty.value && idProperty.value.type === 'StringLiteral') {
                                const oldKey = idProperty.value.value;
                                const newKey = keyMap[oldKey];
                                
                                // If new key found in keyMap and different from old key, replace it
                                if (newKey && newKey !== oldKey) {
                                    idProperty.value = t.stringLiteral(newKey);
                                    hasChanges = true;
                                }
                            }
                        }
                    }
                }
            });
            
            // If changes made, write back to file
            if (hasChanges) {
                const newCode = generate(ast, {
                    jsescOption: { 
                        minimal: true,
                        quotes: 'single'
                    },
                    retainLines: true,
                    compact: false
                }, code).code;
                
                fs.writeFileSync(file, newCode);
                console.log(`    # Updated file: ${file}`);
            }
        } catch (error) {
            console.error(`    # Failed to process file: ${file}`, error);
            if (error.loc) {
                console.error(`    # Error location: Line ${error.loc.line}, Column ${error.loc.column}`);
            }
        }
    }
    
    console.log('    # Finished replacing all keys');
};

const askQuestion = (query) => {
    const rl = readline.createInterface({
        input: process.stdin,
        output: process.stdout,
    });

    return new Promise(resolve => rl.question(query, ans => {
        rl.close();
        resolve(ans.toLowerCase());
    }));
};

const extractJsonContent = (text, retryCount = 3) => {
    if (typeof text !== 'string') {
        console.error('    # Error: Input text is not a string');
        console.error('    # Actual type:', typeof text);
        console.error('    # Content:', text);
        return null;
    }

    // Clean text to remove characters that may cause parsing failures
    const cleanText = text
        .replace(/[\u0000-\u001F\u007F-\u009F]/g, '') // Remove control characters
        .replace(/\n\s*\n/g, '\n') // Remove extra empty lines
        .trim(); // Trim whitespace

    try {
        // First try to parse directly as JSON
        return JSON.parse(cleanText);
    } catch (e) {
        try {
            // If direct parsing fails, try extracting content between ```json```
            const regex = /```json\n([\s\S]*?)```/;
            const match = cleanText.match(regex);
            if (match && match[1]) {
                return JSON.parse(match[1].trim());
            }
        } catch (innerError) {
            console.error('    # JSON parsing error, preparing to retry translation');
            console.error('    # Original text:', text);
            console.error('    # Error message:', innerError.message);
        }
        return null;
    }
};

// Shared file scanning function
const scanFiles = (path, fileType, doNotTranslateFiles = []) => {
    const fileTypes = fileType.split(',').map(s => s.trim());
    const re = new RegExp(`(${fileTypes.map(ft => ft.replace('.', '\\.')).join('|')})$`);
    const files = find.fileSync(re, path);
    console.log(`    # Found ${files.length} ${fileType} files`);
    
    return files.filter(file => !shouldIgnoreFile(file, doNotTranslateFiles));
};

// Shared text processing function
const processFiles = async (files, params) => {
    const { keyPrefix, functionImportPath } = params;
    const promises = files.map(file => {
        if (/(\.js|\.ts|\.tsx|\.jsx)$/i.test(file)) {
            return parseJSX(file, { ...params, keyPrefix }).then(langObj => {
                if (Object.keys(langObj).length > 0 && functionImportPath) {
                    injectImportStatement(file, functionImportPath);
                }
                return langObj;
            });
        }
        return Promise.resolve({});
    });

    const results = await Promise.all(promises);
    const i18n = {};
    results.forEach(langObj => {
        Object.keys(langObj).forEach(lang => {
            i18n[lang] = langObj[lang];
        });
    });
    
    return i18n;
};

// Shared translation and file update function
const translateAndUpdateFiles = async (i18n, path, targetFilePath, fileType, isUpdate = false) => {
    if (Object.keys(i18n).length === 0) {
        console.log('    No text found to extract');
        return;
    }

    // Generate temporary key.json file
    const keyJsonPath = `${targetFilePath}/key.json`;
    fs.ensureDirSync(targetFilePath);
    fs.writeJsonSync(keyJsonPath, i18n, { spaces: 2 });
    console.log(`    # Generated temporary key.json file with ${Object.keys(i18n).length} text entries`);

    // Call key translation
    console.log(`    # AI is translating keys... Please wait`);
    const { keyMap, zhCN } = await batchKeyTranslate(i18n);
    
    // Replace file keys
    console.log(`    # Starting to replace keys in ${files.length} files...`);
    
    for (const file of files) {
        try {
            const code = fs.readFileSync(file, 'utf8');
            
            // Parse file
            const ast = babylon.parse(code, {
                sourceType: 'module',
                allowImportExportEverywhere: true,
                plugins: [
                    // Basic plugins
                    'jsx',
                    'objectRestSpread',
                    'classProperties',
                    'functionBind',
                    
                    // TypeScript related plugins
                    'typescript',
                    
                    // Decorator plugins (legacy version only)
                    'decorators-legacy',
                    
                    // Dynamic import
                    'dynamicImport',
                    
                    // Class private properties
                    'classPrivateProperties',
                    'classPrivateMethods',
                    
                    // Optional chaining and nullish coalescing
                    'optionalChaining',
                    'nullishCoalescingOperator',
                ].filter(Boolean),
                tokens: true,
                errorRecovery: true,
            });
            
            let hasChanges = false;
            
            // Traverse and replace keys
            traverse(ast, {
                CallExpression(path) {
                    // Check if it's a $i18n.get call
                    if (
                        path.node.callee.type === 'MemberExpression' &&
                        path.node.callee.object &&
                        path.node.callee.object.name === '$i18n' &&
                        path.node.callee.property &&
                        path.node.callee.property.name === 'get'
                    ) {
                        const args = path.node.arguments;
                        // Check if first argument is an object expression
                        if (args.length > 0 && args[0].type === 'ObjectExpression') {
                            // Find id property
                            const idProperty = args[0].properties.find(
                                prop => prop.key && prop.key.name === 'id'
                            );
                            
                            // If id property found and is a string literal
                            if (idProperty && idProperty.value && idProperty.value.type === 'StringLiteral') {
                                const oldKey = idProperty.value.value;
                                const newKey = keyMap[oldKey];
                                
                                // If new key found in keyMap and different from old key, replace it
                                if (newKey && newKey !== oldKey) {
                                    idProperty.value = t.stringLiteral(newKey);
                                    hasChanges = true;
                                }
                            }
                        }
                    }
                }
            });
            
            // If changes made, write back to file
            if (hasChanges) {
                const newCode = generate(ast, {
                    jsescOption: { 
                        minimal: true,
                        quotes: 'single'
                    },
                    retainLines: true,
                    compact: false
                }, code).code;
                
                fs.writeFileSync(file, newCode);
                console.log(`    # Updated file: ${file}`);
            }
        } catch (error) {
            console.error(`    # Failed to process file: ${file}`, error);
            if (error.loc) {
                console.error(`    # Error location: Line ${error.loc.line}, Column ${error.loc.column}`);
            }
        }
    }
    
    console.log('    # Finished replacing all keys');
};

// Check if i18n call has matching id and dm
const checkI18nCall = (node) => {
    if (
        node.type === 'CallExpression' &&
        node.callee.type === 'MemberExpression' &&
        node.callee.object &&
        node.callee.object.name === '$i18n' &&
        node.callee.property &&
        node.callee.property.name === 'get'
    ) {
        const args = node.arguments;
        if (args.length > 0 && args[0].type === 'ObjectExpression') {
            const idProperty = args[0].properties.find(
                prop => prop.key && prop.key.name === 'id'
            );
            const dmProperty = args[0].properties.find(
                prop => prop.key && prop.key.name === 'dm'
            );
            
            if (idProperty?.value?.type === 'StringLiteral' && dmProperty?.value?.type === 'StringLiteral') {
                const id = idProperty.value.value;
                const dm = dmProperty.value.value;
                return { id, dm };
            }
        }
    }
    return null;
};

// Refactored main function
exports.init = async function (path, params) {
    const { fileType = '.tsx,.js,.ts,.jsx', targetFilePath, keyPrefix, doNotTranslateFiles } = params;
    
    if (!keyPrefix) {
        throw new Error('keyPrefix is required in params');
    }

    const files = scanFiles(path, fileType, doNotTranslateFiles);
    console.log(`    # 解析${files.length}个${fileType}文件`);
    
    const i18n = await processFiles(files, params);
    await translateAndUpdateFiles(i18n, path, targetFilePath, fileType, false);
    
    console.log('    文案提取和翻译成功！');
};

exports.update = async function (path, params) {
    const { fileType = '.tsx,.js,.ts,.jsx', targetFilePath, keyPrefix, doNotTranslateFiles } = params;
    
    if (!keyPrefix) {
        throw new Error('keyPrefix is required in params');
    }

    // 获取文件修改时间
    const i18nFileMTime = getFileMTime(`${targetFilePath}/zh-cn.json`);
    const files = scanFiles(path, fileType, doNotTranslateFiles);
    
    // 过滤出新增或修改的文件
    const newFiles = files.filter(file => getFileMTime(file) > i18nFileMTime);
    console.log(`    # 其中${newFiles.length}个文件需要更新`);

    if (newFiles.length === 0) {
        console.log('    没有需要更新的文件');
        return;
    }

    const i18n = await processFiles(newFiles, params);
    const newKeysCount = await translateAndUpdateFiles(i18n, path, targetFilePath, fileType, true);
    
    console.log(`    文案更新成功！新增${newKeysCount}条文案`);
};

exports.patch = async function (path, params) {
    const { fileType = '.tsx,.js,.ts,.jsx', targetFilePath, keyPrefix, doNotTranslateFiles } = params;
    
    if (!keyPrefix) {
        throw new Error('keyPrefix is required in params');
    }

    const files = scanFiles(path, fileType, doNotTranslateFiles);
    console.log(`    # 检查${files.length}个${fileType}文件`);
    
    const i18n = await processFiles(files, params);
    const newKeysCount = await translateAndUpdateFiles(i18n, path, targetFilePath, fileType, true);
    
    console.log(`    文案补充完成！新增${newKeysCount}条文案`);
};

exports.check = async function (path, params) {
    const { fileType = '.tsx,.js,.ts,.jsx', targetFilePath } = params;

    // 读取现有的翻译文件
    let zhCN = {};
    let enUS = {};
    try {
        zhCN = fs.readJsonSync(`${targetFilePath}/zh-cn.json`);
    } catch (error) {
        console.log('    未找到zh-cn.json文件');
    }
    try {
        enUS = fs.readJsonSync(`${targetFilePath}/en-us.json`);
    } catch (error) {
        console.log('    未找到en-us.json文件');
    }

    const fileTypes = fileType.split(',').map(s => s.trim());
    const re = new RegExp(`(${fileTypes.map(ft => ft.replace('.', '\\.')).join('|')})$`);

    // 检索文件
    const files = find.fileSync(re, path);
    console.log(`    # 检查${files.length}个${fileType}文件`);

    // 收集所有使用的key
    const usedKeys = new Set();

    // 遍历所有文件
    files.forEach(file => {
        try {
            const code = fs.readFileSync(file, 'utf8');
            
            // 解析文件
            const ast = babylon.parse(code, {
                sourceType: 'module',
                allowImportExportEverywhere: true,
                plugins: [
                    'jsx',
                    'objectRestSpread',
                    'classProperties',
                    'functionBind',
                    'typescript',
                    'decorators-legacy',
                    'dynamicImport',
                    'classPrivateProperties',
                    'classPrivateMethods',
                    'optionalChaining',
                    'nullishCoalescingOperator',
                ].filter(Boolean),
                tokens: true,
                errorRecovery: true,
            });
            
            // 遍历AST查找$i18n.get调用
            traverse(ast, {
                CallExpression(path) {
                    if (
                        path.node.callee.type === 'MemberExpression' &&
                        path.node.callee.object &&
                        path.node.callee.object.name === '$i18n' &&
                        path.node.callee.property &&
                        path.node.callee.property.name === 'get'
                    ) {
                        const args = path.node.arguments;
                        if (args.length > 0 && args[0].type === 'ObjectExpression') {
                            const idProperty = args[0].properties.find(
                                prop => prop.key && prop.key.name === 'id'
                            );
                            
                            if (idProperty && idProperty.value && idProperty.value.type === 'StringLiteral') {
                                usedKeys.add(idProperty.value.value);
                            }
                        }
                    }
                }
            });
        } catch (error) {
            console.error(`    # 处理文件失败: ${file}`, error);
            if (error.loc) {
                console.error(`    # 错误位置: 第${error.loc.line}行, 第${error.loc.column}列`);
            }
        }
    });

    // 获取所有翻译文件中的key
    const zhCNKeys = new Set(Object.keys(zhCN));
    const enUSKeys = new Set(Object.keys(enUS));

    // 找出缺失的key
    const missingKeys = {
        zhCN: Array.from(usedKeys).filter(key => !zhCNKeys.has(key)),
        enUS: Array.from(usedKeys).filter(key => !enUSKeys.has(key))
    };

    // 找出多余的key
    const unusedKeys = {
        zhCN: Array.from(zhCNKeys).filter(key => !usedKeys.has(key)),
        enUS: Array.from(enUSKeys).filter(key => !usedKeys.has(key))
    };

    // 输出结果
    console.log('\n=== 检查结果 ===');
    
    console.log('\n1. 缺失的翻译key:');
    if (missingKeys.zhCN.length > 0) {
        console.log('\nzh-cn.json 缺失的key:');
        missingKeys.zhCN.forEach(key => console.log(`    ${key}`));
    } else {
        console.log('\nzh-cn.json 没有缺失的key');
    }
    
    if (missingKeys.enUS.length > 0) {
        console.log('\nen-us.json 缺失的key:');
        missingKeys.enUS.forEach(key => console.log(`    ${key}`));
    } else {
        console.log('\nen-us.json 没有缺失的key');
    }

    console.log('\n2. 未使用的翻译key:');
    if (unusedKeys.zhCN.length > 0) {
        console.log('\nzh-cn.json 中未使用的key:');
        unusedKeys.zhCN.forEach(key => console.log(`    ${key}`));
    } else {
        console.log('\nzh-cn.json 没有未使用的key');
    }
    
    if (unusedKeys.enUS.length > 0) {
        console.log('\nen-us.json 中未使用的key:');
        unusedKeys.enUS.forEach(key => console.log(`    ${key}`));
    } else {
        console.log('\nen-us.json 没有未使用的key');
    }

    console.log('\n=== 检查完成 ===\n');

    // 如果有未使用的key，询问是否删除
    if (unusedKeys.zhCN.length > 0 || unusedKeys.enUS.length > 0) {
        const answer = await askQuestion('是否删除未使用的key？(y/n): ');
        if (answer === 'y' || answer === 'yes') {
            // 删除zh-cn.json中未使用的key
            if (unusedKeys.zhCN.length > 0) {
                unusedKeys.zhCN.forEach(key => {
                    delete zhCN[key];
                });
                fs.writeJsonSync(`${targetFilePath}/zh-cn.json`, zhCN, { spaces: 2 });
                console.log(`    # 已从zh-cn.json中删除${unusedKeys.zhCN.length}个未使用的key`);
            }

            // 删除en-us.json中未使用的key
            if (unusedKeys.enUS.length > 0) {
                unusedKeys.enUS.forEach(key => {
                    delete enUS[key];
                });
                fs.writeJsonSync(`${targetFilePath}/en-us.json`, enUS, { spaces: 2 });
                console.log(`    # 已从en-us.json中删除${unusedKeys.enUS.length}个未使用的key`);
            }
        } else {
            console.log('    # 已取消删除操作');
        }
    }

    // 返回结果，以便其他程序可能需要使用
    return {
        missingKeys,
        unusedKeys
    };
}