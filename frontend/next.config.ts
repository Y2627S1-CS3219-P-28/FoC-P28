import type { NextConfig } from "next"

const nextConfig: NextConfig = {
  // Self-contained server bundle for the Docker image (see Dockerfile).
  output: "standalone",
  reactCompiler: true,
  poweredByHeader: false,
  // Optional local proxy for the isolated Order prototype. Deployed traffic uses the gateway.
  async rewrites() {
    const orderUrl = process.env.NODE_ENV === "development" ? process.env.FOC_ORDER_DEV_URL : undefined
    return orderUrl ? [{ source: "/api/orders/:path*", destination: `${orderUrl.replace(/\/+$/, "")}/api/orders/:path*` }] : []
  },
  images: {
    // Seed-data photos are several MB; serve them resized (NFR5.3.1). Only this host is optimised.
    remotePatterns: [{ protocol: "https", hostname: "raw.githubusercontent.com" }],
  },
}

export default nextConfig
