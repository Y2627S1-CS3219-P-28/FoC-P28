"use client"

import { useCallback } from "react"

import { useAuth } from "@/components/providers/auth-provider"
import { useConfig } from "@/components/providers/config-provider"
import { apiRequest, type RequestOptions } from "@/lib/api"

// Returns a request function that always sends the signed-in user's Firebase ID token.
export function useApi() {
  const { apiBaseUrl } = useConfig()
  const { getIdToken } = useAuth()

  return useCallback(
    async <T,>(path: string, options: Omit<RequestOptions, "token"> = {}) =>
      apiRequest<T>(apiBaseUrl, path, { ...options, token: await getIdToken() }),
    [apiBaseUrl, getIdToken],
  )
}
