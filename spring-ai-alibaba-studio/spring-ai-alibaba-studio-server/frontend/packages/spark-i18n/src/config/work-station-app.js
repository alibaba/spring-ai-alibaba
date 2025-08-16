const doNotTranslateFiles = [
    // Ignore test files
    /\.test\./,
    /\.spec\./,
    // Ignore type definition files
    /\.d\.ts$/,
    // Ignore specific directories
    'src/pages/App/Workflow/components/ScriptCodeMirror',
]

const config = {
    path: '../main/src',
    fileExtensions: ['.tsx', '.js', '.ts', '.jsx'],
    targetFilePath: '../main/src/i18n/locales/',
    keyPrefix: 'main',
    functionImportPath: '@/i18n'
}

module.exports = {
    doNotTranslateFiles,
    config
}