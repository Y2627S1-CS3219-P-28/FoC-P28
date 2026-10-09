"use client"

import { useState } from "react"
import { toast } from "sonner"

import { QuarterHourDateTimePicker } from "@/components/orders/quarter-hour-date-time-picker"
import { useAuth } from "@/components/providers/auth-provider"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { useApi } from "@/hooks/use-api"
import { invalidateCreditBalance } from "@/lib/credit-balance-events"
import { ApiError } from "@/lib/api"
import { isQuarterHourDateTime, minOrderExpiryDateTimeLocal, quarterHourDateTimeLocal, type Order } from "@/lib/orders"

const inputClass = "h-9 rounded-lg border bg-transparent px-3 text-sm"

export function RepostControls({ order, onUpdated }: { order: Order; onUpdated: (order: Order) => void }) {
  const api = useApi()
  const { user } = useAuth()
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState<{ message: string; version: number } | null>(null)
  const failureMessage = failure && order.version <= failure.version
    ? failure.message : order.repostFailureMessage
  const [creditAmount, setCreditAmount] = useState(String(order.offeredCredits))
  const [duration, setDuration] = useState(String(order.deliveryTimeLimitMinutes))
  const [description, setDescription] = useState(order.itemDescription)
  const [expiresAt, setExpiresAt] = useState(() => quarterHourDateTimeLocal(new Date(Math.max(new Date(order.expiresAt).getTime() + 2 * 60 * 60_000, Date.now() + 30 * 60_000))))

  async function manualRepost() {
    if (!user) return
    if (!isQuarterHourDateTime(expiresAt)) {
      toast.error("Choose expiry minutes of 00, 15, 30, or 45.")
      return
    }
    if (new Date(expiresAt).getTime() < Date.now() + 30 * 60_000) {
      toast.error("Order expiry must be at least 30 minutes from now. Choose a later time.")
      return
    }
    setBusy(true)
    setFailure(null)
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
      const code = error instanceof ApiError ? error.code : "UNKNOWN"
      const message = code === "INSUFFICIENT_CREDITS"
        ? "Repost failed: insufficient available credits."
        : code === "FORBIDDEN" || code === "UNAUTHENTICATED"
          ? "Repost could not be authorized. Please sign in again."
          : code === "VALIDATION_ERROR"
            ? "Repost failed: check the request details."
            : code === "CONFLICT"
              ? "This request could not be reposted. Refresh and check its status."
              : "Could not repost right now. Please try again later."
      setFailure({ message, version: order.version })
      // Failure recording changes the original's version. Refresh it before
      // another manual attempt; polling/reload can also read this durable result.
      if (code !== "UNAUTHENTICATED" && code !== "FORBIDDEN") {
        try {
          const original = await api<Order>(`/api/orders/${order.id}`)
          onUpdated(original)
        } catch {
          // Keep the short local message if this read also fails; polling recovers.
        }
      }
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
              <div className="grid gap-4 text-sm sm:grid-cols-2">
                <p><span className="text-muted-foreground">Repost time</span><br />{order.repostDueAt ? new Date(order.repostDueAt).toLocaleString() : "Not available"}</p>
                <p><span className="text-muted-foreground">Repost expiry</span><br />{order.repostExpiresAt ? new Date(order.repostExpiresAt).toLocaleString() : "Not available"}</p>
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
        {!busy && failureMessage && <p role="alert" className="text-xs text-destructive">{failureMessage}</p>}
        <label className="space-y-1 text-sm"><Label htmlFor={`description-${order.id}`}>Description</Label><Input id={`description-${order.id}`} className={inputClass} value={description} onChange={(event) => setDescription(event.target.value)} /></label>
        <div className="grid gap-4 sm:grid-cols-3">
          <label className="space-y-1 text-sm"><Label htmlFor={`manual-credits-${order.id}`}>Credits</Label><Input id={`manual-credits-${order.id}`} type="number" min="1" className={inputClass} value={creditAmount} onChange={(event) => setCreditAmount(event.target.value)} /></label>
          <label className="space-y-1 text-sm"><Label htmlFor={`manual-duration-${order.id}`}>Delivery minutes</Label><Input id={`manual-duration-${order.id}`} type="number" min="15" className={inputClass} value={duration} onChange={(event) => setDuration(event.target.value)} /></label>
          <div className="sm:col-span-3"><QuarterHourDateTimePicker id={"manual-expires-" + order.id} label="New expiry" min={minOrderExpiryDateTimeLocal()} value={expiresAt} onChange={setExpiresAt} /></div>
        </div>
        <Button size="sm" onClick={() => void manualRepost()} disabled={busy}>{busy ? "Reposting…" : "Create repost"}</Button>
      </CardContent>
    </Card>
  )
}
