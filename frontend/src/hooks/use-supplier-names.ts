"use client"

import { useEffect, useMemo, useState } from "react"

import { useApi } from "@/hooks/use-api"
import { supplierLookupPath, supplierLookupPayload, supplierOptionLabel, type SupplierLookupResponse } from "@/lib/suppliers"

export function useSupplierNames(ids: string[]) {
  const api = useApi()
  const uniqueIds = useMemo(() => Array.from(new Set(ids.filter(Boolean))), [ids])
  const requestKey = uniqueIds.join("\u001f")
  const [names, setNames] = useState<Record<string, string>>({})
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (uniqueIds.length === 0) {
      setNames({})
      setLoading(false)
      setError(null)
      return
    }

    const controller = new AbortController()
    setLoading(true)
    setError(null)
    void api<SupplierLookupResponse>(supplierLookupPath(), {
      method: "POST",
      body: supplierLookupPayload(uniqueIds),
      signal: controller.signal,
    })
      .then((response) => {
        setNames(Object.fromEntries(response.items.map((supplier) => [supplier.id, supplierOptionLabel(supplier)])))
      })
      .catch((requestError) => {
        if (requestError instanceof DOMException && requestError.name === "AbortError") return
        setError(requestError instanceof Error ? requestError.message : "Could not resolve supplier names.")
      })
      .finally(() => setLoading(false))

    return () => controller.abort()
    // requestKey intentionally represents the de-duplicated ID list.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [api, requestKey])

  return { names, loading, error }
}
