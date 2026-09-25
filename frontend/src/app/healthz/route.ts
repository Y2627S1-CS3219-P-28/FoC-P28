// Liveness endpoint for Docker/Cloud Run health checks. Not proxied under /api, which the
// gateway reserves for backend services.
export function GET() {
  return Response.json({ status: "UP" })
}
