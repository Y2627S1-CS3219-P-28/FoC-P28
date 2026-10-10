"use client"

import { useCallback, useEffect, useRef, useState } from "react"
import { useAuth } from "@/components/providers/auth-provider"
import { OrderCommandNotice } from "@/components/orders/order-command-notice"
import { OrderPagination } from "@/components/orders/order-pagination"
import { useApi } from "@/hooks/use-api"
import { ORDER_POLL_INTERVAL_MS, useVisiblePolling } from "@/hooks/use-visible-polling"
import type { OrderCommandStatus } from "@/lib/order-commands"

/** Server-owned discovery survives navigation, lost local responses and missing order cards. */
export function PendingOrderCommands({ onResolved }: { onResolved: () => void }) {
  const api = useApi()
  const { user, loading } = useAuth()
  const [saved, setSaved] = useState<{ account: string; items: OrderCommandStatus[]; pages: number; enabled: boolean } | null>(null)
  const [page, setPage] = useState(1)
  const [busy, setBusy] = useState<string | null>(null)
  const active = useRef<string | null>(null)
  const resolved = useRef(onResolved)
  useEffect(() => { resolved.current = onResolved }, [onResolved])
  const account = user?.uid
  const refresh = useCallback(async (signal?: AbortSignal) => {
    if (!account || loading) return
    const capability = await api<{ enabled: boolean }>("/api/orders/commands/capabilities", { signal })
    const result = await api<{ items: OrderCommandStatus[]; totalPages: number }>(
      `/api/orders/commands?userId=${encodeURIComponent(account)}&page=${page}&size=20`, { signal })
    if (!signal?.aborted) setSaved({ account, items: result.items ?? [], pages: result.totalPages ?? 0, enabled: capability.enabled === true })
  }, [account, loading, api, page])
  useVisiblePolling(refresh, !!account && !loading, ORDER_POLL_INTERVAL_MS)

  async function authorize(command: OrderCommandStatus) {
    if (!account || !saved?.enabled || active.current) return
    active.current = command.commandId; setBusy(command.commandId)
    try {
      const result = await api<OrderCommandStatus>(`/api/orders/commands/${encodeURIComponent(command.commandId)}/resume?userId=${encodeURIComponent(account)}`, { method: "POST" })
      if (result.status === "COMPLETED") resolved.current()
      await refresh()
    } catch { /* Keep the owned action pending; a read failure is not a financial rejection. */ }
    finally { active.current = null; setBusy(null) }
  }

  if (loading || !account || saved?.account !== account || (!saved.items.length && saved.pages <= 1)) return null
  const labels = { CREATE: "Posting request", ACCEPT: "Accepting errand", ABORT: "Aborting errand" }
  return <section aria-label="Pending requests" className="space-y-3 rounded-xl border p-4">
    <h2 className="text-sm font-medium">Requests in progress</h2>
    {saved.items.map((command) => <div key={command.commandId} className="space-y-1">
      <p className="text-sm font-medium">{command.kind ? labels[command.kind] : "Processing request"}</p>
      <OrderCommandNotice message={`${command.message ?? "Your request is pending."}${command.attemptCount ? ` Attempt ${command.attemptCount}.` : ""}`}
        needsAuthorization={saved.enabled && !busy && command.reason === "AUTHORIZATION_REQUIRED"}
        continueRecovery={() => authorize(command)} />
    </div>)}
    <OrderPagination page={page} totalPages={saved.pages} disabled={!!busy} onChange={setPage} />
  </section>
}
