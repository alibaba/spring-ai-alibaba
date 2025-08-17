const doNotTranslateFiles = [
    // Ignore test files
    /\.test\./,
    /\.spec\./,
    // Ignore type definition files
    /\.d\.ts$/,
    // Ignore specific directories
    // 'src/pages/App/Workflow/components/ScriptCodeMirror',
]

const config = {
    path: '../spark-flow/src',
    fileExtensions: ['.tsx', '.js', '.ts', '.jsx'],
    targetFilePath: '../spark-flow/src/i18n/locales/',
    keyPrefix: 'spark-flow',
    functionImportPath: '@/i18n'
}

module.exports = {
    doNotTranslateFiles,
    config
}