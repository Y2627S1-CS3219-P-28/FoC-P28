"use client"

import { useMemo, useState } from "react"
import { toast } from "sonner"

import { useAuth } from "@/components/providers/auth-provider"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { useApi } from "@/hooks/use-api"
import { buildRepostConfigPayload, isoToDateTimeLocal, type Order } from "@/lib/orders"

const inputClass = "h-9 rounded-lg border bg-transparent px-3 text-sm"

export function RepostControls({ order, onUpdated }: { order: Order; onUpdated: (order: Order) => void }) {
  const api = useApi()
  const { user } = useAuth()
  const [busy, setBusy] = useState(false)
  const defaultDueAt = useMemo(() => isoToDateTimeLocal(new Date(new Date(order.expiresAt).getTime() + 60 * 60_000).toISOString()), [order.expiresAt])
  const [enabled, setEnabled] = useState(false)
  const [dueAt, setDueAt] = useState(defaultDueAt)
  const [creditAmount, setCreditAmount] = useState(String(order.offeredCredits))
  const [duration, setDuration] = useState(String(order.deliveryTimeLimitMinutes))
  const [description, setDescription] = useState(order.itemDescription)
  const [expiresAt, setExpiresAt] = useState(isoToDateTimeLocal(new Date(new Date(order.expiresAt).getTime() + 2 * 60 * 60_000).toISOString()))

  async function configure() {
    if (!user) return
    setBusy(true)
    try {
      const updated = await api<Order>(`/api/orders/${order.id}/repost/configure`, {
        method: "POST",
        body: buildRepostConfigPayload({
          itemDescription: order.itemDescription,
          pickupSupplierId: order.pickupSupplierId,
          deliverySupplierId: order.deliverySupplierId,
          offeredCredits: order.offeredCredits,
          deliveryTimeLimitMinutes: order.deliveryTimeLimitMinutes,
          expiresAt: order.expiresAt,
          automaticRepost: enabled,
          repostDueAt: dueAt,
          repostCreditAmount: Number(creditAmount),
          repostDeliveryDurationMinutes: Number(duration),
        }, user.uid, order.version),
      })
      onUpdated(updated)
      toast.success(enabled ? "Automatic repost enabled" : "Automatic repost disabled")
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Could not save repost settings.")
    } finally {
      setBusy(false)
    }
  }

  async function manualRepost() {
    if (!user) return
    setBusy(true)
    try {
      const updated = await api<Order>(`/api/orders/${order.id}/repost`, {
        method: "POST",
        body: {
          commandId: crypto.randomUUID(),
          actorId: user.uid,
          expectedVersion: order.version,
          itemDescription: description.trim(),
          offeredCredits: Number(creditAmount),
          deliveryTimeLimitMinutes: Number(duration),
          expiresAt: new Date(expiresAt).toISOString(),
        },
      })
      onUpdated(updated)
      toast.success("Order reposted")
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Could not repost this order.")
    } finally {
      setBusy(false)
    }
  }

  if (order.status === "OPEN") {
    return (
      <Card className="border-dashed bg-muted/30">
        <CardHeader className="pb-3">
          <CardTitle className="text-sm">Automatic repost</CardTitle>
          <CardDescription>Prepare one linked repost if this order expires unaccepted. Credits are reserved only when the repost is created.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <label className="flex items-center gap-2 text-sm font-medium">
            <input type="checkbox" checked={enabled} onChange={(event) => setEnabled(event.target.checked)} />
            Enable automatic repost
          </label>
          <div className="grid gap-4 sm:grid-cols-3">
            <label className="space-y-1 text-sm"><Label htmlFor={`due-${order.id}`}>Repost time</Label><Input id={`due-${order.id}`} type="datetime-local" className={inputClass} value={dueAt} onChange={(event) => setDueAt(event.target.value)} /></label>
            <label className="space-y-1 text-sm"><Label htmlFor={`credits-${order.id}`}>Repost credits</Label><Input id={`credits-${order.id}`} type="number" min="1" className={inputClass} value={creditAmount} onChange={(event) => setCreditAmount(event.target.value)} /></label>
            <label className="space-y-1 text-sm"><Label htmlFor={`duration-${order.id}`}>Delivery minutes</Label><Input id={`duration-${order.id}`} type="number" min="15" className={inputClass} value={duration} onChange={(event) => setDuration(event.target.value)} /></label>
          </div>
          <Button size="sm" variant="outline" onClick={() => void configure()} disabled={busy}>{busy ? "Saving…" : "Save repost settings"}</Button>
        </CardContent>
      </Card>
    )
  }

  if (order.status !== "EXPIRED" || order.repostedOrderId) return null

  return (
    <Card className="border-dashed bg-muted/30">
      <CardHeader className="pb-3">
        <CardTitle className="text-sm">Review and repost</CardTitle>
        <CardDescription>This expired order has not been reposted. Review the details before creating one linked repost.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <label className="space-y-1 text-sm"><Label htmlFor={`description-${order.id}`}>Description</Label><Input id={`description-${order.id}`} className={inputClass} value={description} onChange={(event) => setDescription(event.target.value)} /></label>
        <div className="grid gap-4 sm:grid-cols-3">
          <label className="space-y-1 text-sm"><Label htmlFor={`manual-credits-${order.id}`}>Credits</Label><Input id={`manual-credits-${order.id}`} type="number" min="1" className={inputClass} value={creditAmount} onChange={(event) => setCreditAmount(event.target.value)} /></label>
          <label className="space-y-1 text-sm"><Label htmlFor={`manual-duration-${order.id}`}>Delivery minutes</Label><Input id={`manual-duration-${order.id}`} type="number" min="15" className={inputClass} value={duration} onChange={(event) => setDuration(event.target.value)} /></label>
          <label className="space-y-1 text-sm"><Label htmlFor={`manual-expires-${order.id}`}>New expiry</Label><Input id={`manual-expires-${order.id}`} type="datetime-local" className={inputClass} value={expiresAt} onChange={(event) => setExpiresAt(event.target.value)} /></label>
        </div>
        <Button size="sm" onClick={() => void manualRepost()} disabled={busy}>{busy ? "Reposting…" : "Create repost"}</Button>
      </CardContent>
    </Card>
  )
}
