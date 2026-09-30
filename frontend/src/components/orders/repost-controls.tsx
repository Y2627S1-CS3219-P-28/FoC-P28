"use client"

import { useState } from "react"
import { toast } from "sonner"

import { useAuth } from "@/components/providers/auth-provider"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { useApi } from "@/hooks/use-api"
import { invalidateCreditBalance } from "@/lib/credit-balance-events"
import { isoToDateTimeLocal, type Order } from "@/lib/orders"

const inputClass = "h-9 rounded-lg border bg-transparent px-3 text-sm"

export function RepostControls({ order, onUpdated }: { order: Order; onUpdated: (order: Order) => void }) {
  const api = useApi()
  const { user } = useAuth()
  const [busy, setBusy] = useState(false)
  const [creditAmount, setCreditAmount] = useState(String(order.offeredCredits))
  const [duration, setDuration] = useState(String(order.deliveryTimeLimitMinutes))
  const [description, setDescription] = useState(order.itemDescription)
  const [expiresAt, setExpiresAt] = useState(isoToDateTimeLocal(new Date(new Date(order.expiresAt).getTime() + 2 * 60 * 60_000).toISOString()))

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
      invalidateCreditBalance()
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
          <CardDescription>Automatic repost is selected when an order is created. It cannot be enabled or changed after posting.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {order.automaticRepostEnabled ? (
            <>
              <p className="text-sm font-medium">Automatic repost was configured when this order was created.</p>
              <div className="grid gap-4 text-sm sm:grid-cols-3">
                <p><span className="text-muted-foreground">Repost time</span><br />{order.repostDueAt ? new Date(order.repostDueAt).toLocaleString() : "Not available"}</p>
                <p><span className="text-muted-foreground">Repost credits</span><br />{order.repostCreditAmount ?? 0}</p>
                <p><span className="text-muted-foreground">Delivery minutes</span><br />{order.repostDeliveryDurationMinutes ?? 0}</p>
              </div>
              <p className="text-xs text-muted-foreground">These settings cannot be changed after posting.</p>
            </>
          ) : (
            <p className="text-sm text-muted-foreground">Automatic repost was not enabled when this order was created and cannot be enabled later.</p>
          )}
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
