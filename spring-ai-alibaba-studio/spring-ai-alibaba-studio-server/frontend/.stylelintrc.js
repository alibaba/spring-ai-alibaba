module.exports = {
  extends: require.resolve('umi/stylelint'),
  customSyntax: 'postcss-less',
  rules: {
    'shorthand-property-no-redundant-values': null,
    'alpha-value-notation': null,
    'rule-empty-line-before': null,
    'value-no-vendor-prefix': null,
    'length-zero-no-unit': null,
    'keyframes-name-pattern': null,
    'declaration-empty-line-before': null,
    'value-keyword-case': null,
    'at-rule-no-unknown': [
      true,
      {
        ignoreAtRules: [
          'tailwind',
          'apply',
          'variants',
          'responsive',
          'screen',
          'layer',
        ],
      },
    ],
    'selector-class-pattern': null,
    'function-no-unknown': [
      true,
      {
        ignoreFunctions: ['e'],
      },
    ],
  },
};
