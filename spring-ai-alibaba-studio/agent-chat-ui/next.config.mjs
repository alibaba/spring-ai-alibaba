/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  // 如果设置了 STATIC_EXPORT=true，则启用静态导出
  ...(process.env.STATIC_EXPORT === 'true' && {
    output: 'export',
    distDir: 'out',
    basePath: '/chatui', // 设置基础路径为 /chatui
  }),
  experimental: {
    serverActions: {
      bodySizeLimit: "10mb",
    },
  },
  // Allow cross-origin requests in development
  allowedDevOrigins: process.env.NODE_ENV === 'development'
    ? ['http://30.222.16.107', 'http://30.222.16.107:3000']
    : undefined,
};

export default nextConfig;
