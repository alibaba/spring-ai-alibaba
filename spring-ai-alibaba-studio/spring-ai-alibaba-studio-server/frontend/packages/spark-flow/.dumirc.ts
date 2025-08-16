import { defineConfig } from 'dumi';

export default defineConfig({
  plugins: ['@umijs/plugins/dist/tailwindcss'],
  outputPath: 'docs-dist',
  themeConfig: {
    name: 'spark-flow',
    hd: { rules: [] },
    logo: 'https://gw.alicdn.com/imgextra/i4/O1CN01vVn7g32134zNZEeAR_!!6000000006928-55-tps-24-24.svg',
    type: 'docs',
    prefersColor: { default: 'light', switch: false },
  },
  resolve: {
    atomDirs: [{ type: 'component', dir: 'src/demos' }],
    entryFile: './src/index.ts',
  },
  esm: { type: 'babel' },
  tailwindcss: {},
});
