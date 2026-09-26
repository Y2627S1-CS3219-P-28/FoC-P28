"use client"

import { useEffect, useState } from "react"

import { useApi } from "@/hooks/use-api"
import type { SupplierPermissions } from "@/lib/suppliers"

/**
 * What the signed-in user may do in the Supplier Service. Only used to show or hide
 * controls; the service enforces the same rules on every request.
 */
export function useSupplierPermissions() {
  const api = useApi()
  const [permissions, setPermissions] = useState<SupplierPermissions | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    api<SupplierPermissions>("/api/suppliers/permissions", { signal: controller.signal })
      .then(setPermissions)
      .catch(() => setPermissions(null))
    return () => controller.abort()
  }, [api])

  return { permissions, canManage: permissions?.canManageSuppliers ?? false }
}
