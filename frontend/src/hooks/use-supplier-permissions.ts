"use client"

import { useEffect, useState } from "react"

import { useAuth } from "@/components/providers/auth-provider"
import { useApi } from "@/hooks/use-api"
import type { SupplierPermissions } from "@/lib/suppliers"

/**
 * What the signed-in user may do in the Supplier Service. Only used to show or hide
 * controls; the service enforces the same rules on every request.
 */
export function useSupplierPermissions() {
  const api = useApi()
  const { user, loading: authLoading } = useAuth()
  const [permissionState, setPermissionState] = useState<{
    userId: string
    permissions: SupplierPermissions | null
  } | null>(null)

  useEffect(() => {
    if (authLoading || !user) return

    const controller = new AbortController()
    api<SupplierPermissions>("/api/suppliers/permissions", { signal: controller.signal })
      .then((nextPermissions) => {
        setPermissionState({ userId: user.uid, permissions: nextPermissions })
      })
      .catch(() => setPermissionState({ userId: user.uid, permissions: null }))
    return () => controller.abort()
  }, [api, authLoading, user])

  // Associate the result with the Firebase user that was queried. This prevents
  // a previous user's permissions from being shown while auth is changing or
  // while the current user's request is still in flight.
  const visiblePermissions =
    authLoading || !user || permissionState?.userId !== user.uid
      ? null
      : permissionState.permissions

  return { permissions: visiblePermissions, canManage: visiblePermissions?.canManageSuppliers ?? false }
}
