const babylon = require('@babel/parser');
const traverse = require('@babel/traverse').default;
const generate = require('@babel/generator').default;
const t = require('@babel/types');
const fs = require('fs-extra');

// Chinese text recognition regex
const chnRegExp = new RegExp(/[^\x00-\xff]/);

// Check if text contains only Chinese punctuation
const onlyContainsChinesePunctuation = (text) => {
    // Chinese punctuation regex
    const chinesePunctuationRegex = /[，。！？；：""''（）【】《》、]/g;
    // Remove all Chinese punctuation
    const textWithoutPunctuation = text.replace(chinesePunctuationRegex, '');
    // If empty after removal, means only contains punctuation
    return textWithoutPunctuation.trim() === '';
};

function isI18nCall(node) {
    // Check if it's $i18n.get('xxx') or $i18n.get({id: 'xxx'})
    return (
        node &&
        node.type === 'CallExpression' &&
        node.callee &&
        node.callee.type === 'MemberExpression' &&
        node.callee.object &&
        node.callee.object.name === '$i18n' &&
        node.callee.property &&
        node.callee.property.name === 'get'
    );
}

const parseJSX = (filePath, params = {}) => {
    const i18n = {};
    const contentKeyMap = new Map(); // Stores content to key mapping
    let uniqueCounter = 0; // Used to generate unique numeric suffix
    
    if (!params.keyPrefix) {
        throw new Error('keyPrefix is required in params');
    }
    
    let code = fs.readFileSync(filePath, {
        encoding: 'utf8'
    });
    
    // Modify file path key generation logic
    // 1. First remove src directory from path
    let filePathKey = filePath.split('src/')[1] || filePath;
    // 2. Remove file extension
    filePathKey = filePathKey.replace(/\.(tsx|jsx|js|ts)$/, '');
    // 3. Replace path separators with dots
    filePathKey = filePathKey.replace(/\//g, '.');
    
    console.log(`    # Parsing ${filePath} file`);

    // Helper function to get or create key
    const getOrCreateKey = (content) => {
        if (contentKeyMap.has(content)) {
            return contentKeyMap.get(content);
        }
        uniqueCounter++;
        const i18nKey = `${params.keyPrefix}.${filePathKey}.${uniqueCounter}`;
        contentKeyMap.set(content, i18nKey);
        i18n[i18nKey] = content;
        return i18nKey;
    };

    // File parsing
    const ast = babylon.parse(code, {
        sourceType: 'module',
        allowImportExportEverywhere: true,
        plugins: ['tsx', 'ts', 'js', 'jsx', 'typescript', 'functionBind', 'objectRestSpread', 'classProperties', 'transform-decorators', 'decorators-legacy'],
    });

    traverse(ast, {
        StringLiteral(path) {
            const { value } = path.node;
            if (chnRegExp.test(value) && !isI18nCall(path.parent) && !path.findParent(p => isI18nCall(p.node))) {
                if (onlyContainsChinesePunctuation(value)) {
                    return;
                }
                try {
                    const rawValue = (path.node.extra?.raw || JSON.stringify(value)).replace(/^['"]|['"]$/g, '');
                    const trimmedValue = rawValue.replace(/^\s+|\s+$/g, '');
                    
                    const i18nKey = getOrCreateKey(trimmedValue);
                    
                    // Create i18n.get call node
                    const i18nCallExpression = t.callExpression(
                        t.memberExpression(t.identifier('$i18n'), t.identifier('get')),
                        [
                            t.objectExpression([
                                t.objectProperty(t.identifier('id'), t.stringLiteral(i18nKey)),
                                t.objectProperty(t.identifier('dm'), t.stringLiteral(trimmedValue))
                            ])
                        ]
                    );
                    
                    // Check parent node type
                    const parentType = path.parent.type;
                    if (parentType === 'JSXAttribute') {
                        // String in JSX attribute
                        path.replaceWith(t.jsxExpressionContainer(i18nCallExpression));
                    } else if (parentType === 'ObjectProperty' && path.parentKey === 'value') {
                        // Object property value
                        path.replaceWith(i18nCallExpression);
                    } else {
                        // Other cases
                        path.replaceWith(i18nCallExpression);
                    }
                    
                    path.skip();
                } catch (e) {
                    console.error('Error processing StringLiteral:', e);
                    console.error('Node:', path.node);
                    console.error('Parent:', path.parent);
                }
            }
        },
        TemplateLiteral(path) {
            if (!isI18nCall(path.parent) && !path.findParent(p => isI18nCall(p.node))) {
                const val = [];
                const expressions = [];
                path.node.quasis.forEach(i => {
                    if (i.value.raw) {
                        val.push(i.value.raw);
                    }
                });

                // Use generate to handle complex expressions
                path.node.expressions.forEach(exp => {
                    try {
                        // Generate expression code while maintaining original form
                        const generatedCode = generate(exp, {
                            compact: true,
                            jsescOption: { minimal: true }
                        }).code;
                        
                        // Remove possible semicolons
                        const cleanCode = generatedCode.replace(/;$/, '');
                        expressions.push(cleanCode);
                    } catch (error) {
                        console.error('Error processing expression:', error);
                        expressions.push('');
                    }
                });

                const joinedVal = val.join('');
                if (chnRegExp.test(joinedVal)) {
                    if (onlyContainsChinesePunctuation(joinedVal)) {
                        return;
                    }                    
                    let key = '';
                    let defaultMessage = '';
                    let variables = new Map();
                    
                    path.node.quasis.forEach((quasi, index) => {
                        const rawValue = quasi.value.raw.replace(/^['"]|['"]$/g, '');
                        const trimmedValue = rawValue.replace(/^\s+|\s+$/g, '');
                        key += trimmedValue;
                        defaultMessage += trimmedValue;
                        
                        if (index < expressions.length) {
                            const expr = expressions[index];
                            if (expr) {
                                // Generate a unique variable name for complex expressions
                                const varName = `var${index + 1}`;
                                key += `{${varName}}`;
                                defaultMessage += `{${varName}}`;
                                variables.set(varName, expr);
                            }
                        }
                    });
                    
                    // Use new key generation logic
                    const i18nKey = getOrCreateKey(key);
                    
                    // Build variable object
                    const varProperties = Array.from(variables.entries()).map(([name, expr]) => {
                        // Parse expression string back to AST node
                        const ast = babylon.parse(`(${expr})`);
                        const exprNode = ast.program.body[0].expression;
                        return t.objectProperty(t.identifier(name), exprNode);
                    });
                    
                    const newNode = t.callExpression(
                        t.memberExpression(t.identifier('$i18n'), t.identifier('get')),
                        [
                            t.objectExpression([
                                t.objectProperty(t.identifier('id'), t.stringLiteral(i18nKey)),
                                t.objectProperty(t.identifier('dm'), t.stringLiteral(defaultMessage))
                            ]),
                            t.objectExpression(varProperties)
                        ]
                    );
                    
                    path.replaceWith(newNode);
                    path.skip();
                }
            }
        },
        JSXText(path) {
            const { value } = path.node;
            if (chnRegExp.test(value) && !isI18nCall(path.parent) && !path.findParent(p => isI18nCall(p.node))) {
                if (onlyContainsChinesePunctuation(value)) {
                    return;
                }
                const rawValue = (path.node.extra?.raw || JSON.stringify(value)).replace(/^['"]|['"]$/g, '');
                const trimmedValue = rawValue.replace(/^\s+|\s+$/g, '');
                
                // Use new key generation logic
                const i18nKey = getOrCreateKey(trimmedValue);
                
                const newNode = t.jsxExpressionContainer(
                    t.callExpression(
                        t.memberExpression(t.identifier('$i18n'), t.identifier('get')),
                        [
                            t.objectExpression([
                                t.objectProperty(t.identifier('id'), t.stringLiteral(i18nKey)),
                                t.objectProperty(t.identifier('dm'), t.stringLiteral(trimmedValue))
                            ])
                        ]
                    )
                );
                
                path.replaceWith(newNode);
                path.skip();
            }
        }
    });

    return new Promise((resolve) => {
        if (Object.keys(i18n).length === 0) {
            return resolve(i18n);
        }
        
        // Save extracted texts
        console.log(`    Extracted ${Object.keys(i18n).length} texts`);
        
        // Write back modified code while preserving original format
        const newCode = generate(ast, {
            jsescOption: { 
                minimal: true,
                quotes: 'single'
            },
            retainLines: true,
            compact: false
        }, code).code;
        fs.writeFileSync(filePath, newCode);
        
        resolve(i18n);
    });
}

module.exports = parseJSX;