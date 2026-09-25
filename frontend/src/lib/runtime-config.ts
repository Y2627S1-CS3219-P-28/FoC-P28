// Browser-safe configuration, read from the server environment at request time.
// NEXT_PUBLIC_* variables are frozen at build time, which would force a rebuild per
// environment; reading plain env vars here lets CI promote one image staging -> prod.
export type PublicConfig = {
  environment: string
  // Origin of the API gateway. Empty string = same origin (the gateway also serves this UI).
  apiBaseUrl: string
  authEmulatorUrl: string | null
  firebase: {
    apiKey: string
    authDomain: string
    projectId: string
    appId?: string
  }
}

export function readPublicConfig(env: NodeJS.ProcessEnv = process.env): PublicConfig {
  const projectId = env.FOC_FIREBASE_PROJECT_ID || "demo-foc"
  return {
    environment: env.FOC_ENVIRONMENT || "local",
    apiBaseUrl: (env.FOC_API_BASE_URL || "").replace(/\/+$/, ""),
    authEmulatorUrl: env.FOC_FIREBASE_AUTH_EMULATOR_URL || null,
    firebase: {
      // The Firebase web API key is a public identifier, not a secret.
      apiKey: env.FOC_FIREBASE_API_KEY || "demo-api-key",
      authDomain: env.FOC_FIREBASE_AUTH_DOMAIN || `${projectId}.firebaseapp.com`,
      projectId,
      appId: env.FOC_FIREBASE_APP_ID || undefined,
    },
  }
}
