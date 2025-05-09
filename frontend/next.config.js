const path = require("path");

/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  output: 'export',

  images: {
    unoptimized: true,
    dangerouslyAllowSVG: true, // SVG 허용 (주의: CSP 설정 필요)
    contentSecurityPolicy: "default-src 'self'; script-src 'none'; sandbox;",
    remotePatterns: [
      { protocol: "https", hostname: "k.kakaocdn.net" },
      { protocol: "https", hostname: "ipfs.io" },
      { protocol: "https", hostname: "static.megamart.com" },
      { protocol: "https", hostname: "example.com" },
    ],
    unoptimized: true,
  },

  webpack: (config) => {
    config.resolve.alias["@"] = path.resolve(__dirname);
    return config;
  },
};

module.exports = nextConfig;
