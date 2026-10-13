"use client"

import { useMemo } from "react"

import { useApi } from "@/hooks/use-api"
import { useConfig } from "@/components/providers/config-provider"
import { createAdminApi } from "@/lib/admin-api"

export function useAdminApi() {
  const request = useApi()
  const config = useConfig()
  const mode = config.adminApiMode
  return useMemo(() => createAdminApi(request, mode), [request, mode])
}
