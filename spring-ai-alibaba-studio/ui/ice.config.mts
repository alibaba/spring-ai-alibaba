import { defineConfig } from '@ice/app';
import antd from '@ice/plugin-antd';
import request from '@ice/plugin-request';

// The project config, see https://v3.ice.work/docs/guide/basic/config
const minify = process.env.NODE_ENV === 'production' ? 'swc' : false;
export default defineConfig(() => ({
  // Set your configs here.
  minify,
  server: {
    onDemand: true,
    format: 'esm',
  },
  plugins: [
    antd({
      importStyle: true,
    }),
    request(),
  ],
  routes: {
  },
}));
