#!/usr/bin/env node

const program = require('commander');
const mcms = require('./index');
const path = require('path');

/**
 * Usage.
 */
program
    .version('0.2.0')
    .option('-t, --type <string>', "类型：'init'、'patch' 、'check' 或 'update'")
    .option('-c, --config <string>', "配置文件名称，如：'main'")
    .action(async function (cmd) {
        const { type, config: configName } = cmd;
        
        if (!type || !configName) {
            console.error('请提供 type 和 config 参数');
            process.exit(1);
        }

        if (!['init', 'patch', 'check', 'update'].includes(type)) {
            console.error('类型错误，请输入：init、patch 、check 或 update');
            process.exit(1);
        }

        try {
            const configModule = await import(`../config/${configName}.js`);
            const { config, doNotTranslateFiles } = configModule;

            if (!config) {
                console.error(`配置文件 ${configName}.js 中未找到 config 对象`);
                process.exit(1);
            }

            const { path: dir, fileExtensions, targetFilePath, functionImportPath, keyPrefix } = config;

            const cmdOptions = {
                fileType: fileExtensions.join(','),
                targetFilePath,
                type,
                functionImportPath,
                doNotTranslateFiles,
                keyPrefix
            };

            if (type === 'init') {
                mcms.init(dir, cmdOptions);
            } else if (type === 'patch') {
                mcms.patch(dir, cmdOptions);
            } else if (type === 'check') {
                mcms.check(dir, cmdOptions);
            } else if (type === 'update') {
                mcms.update(dir, cmdOptions);
            }
        } catch (error) {
            console.error(`加载配置文件失败: ${error.message}`);
            process.exit(1);
        }
    });

program.on('--help', function () {
    console.log('  Examples:');
    console.log('');
    console.log('    $ must -t init -c main');
    console.log('    $ must -t check -c main');
    console.log('    $ must -t update -c main');
    console.log('');
});

program.parse(process.argv);