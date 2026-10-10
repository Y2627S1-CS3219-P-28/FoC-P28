"use client"

import { useState } from "react"
import { toast } from "sonner"

import { useAuth } from "@/components/providers/auth-provider"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog"
import { Button } from "@/components/ui/button"
import { useApi } from "@/hooks/use-api"
import { useOrderCommand } from "@/hooks/use-order-command"
import { OrderCommandNotice } from "@/components/orders/order-command-notice"
import { invalidateCreditBalance } from "@/lib/credit-balance-events"
import { buildOrderActionPayload, type Order, type OrderMode } from "@/lib/orders"

const ACTIONS = {
  ACCEPTED: { path: "start", label: "Start errand" },
  IN_PROGRESS: { path: "pickup", label: "Mark picked up" },
  PICKED_UP: { path: "deliver", label: "Mark delivered" },
} as const

export function OrderActions({ order, mode, onUpdated }: { order: Order; mode: OrderMode; onUpdated: (order: Order) => void }) {
  const api = useApi()
  const { user } = useAuth()
  const [busy, setBusy] = useState(false)
  const abort = useOrderCommand(`abort:${order.id}`, (updated) => {
    onUpdated(updated)
    toast.success(updated.status === "OPEN" ? "Errand reopened for other couriers" : "Expired errand aborted")
  }, (message) => toast.error(message))

  async function perform(path: string, success: string) {
    if (!user) return
    setBusy(true)
    try {
      if (path === "cancel-accepted") {
        await abort.run("ABORT", `/api/orders/${order.id}/${path}`, buildOrderActionPayload(user.uid, order.version))
        return
      }
      const updated = await api<Order>(`/api/orders/${order.id}/${path}`, {
        method: "POST",
        body: buildOrderActionPayload(user.uid, order.version),
      })
      onUpdated(updated)
      if (path === "complete" || path === "cancel") invalidateCreditBalance()
      if (path === "cancel-accepted") {
        toast.success(updated.status === "OPEN" ? "Errand reopened for other couriers" : "Expired errand aborted")
      } else {
        toast.success(success)
      }
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Could not update this order.")
    } finally {
      setBusy(false)
    }
  }

  if (mode === "courier") {
    const action = ACTIONS[order.status as keyof typeof ACTIONS]
    if (!action && order.status !== "ACCEPTED") return null

    return (
      <>
        {action && (
          <Button size="sm" onClick={() => void perform(action.path, action.label)} disabled={busy || abort.pending}>
            {busy ? "Saving…" : action.label}
          </Button>
        )}
        {order.status === "ACCEPTED" && (
          <AlertDialog>
            <AlertDialogTrigger render={<Button size="sm" variant="destructive" disabled={busy || abort.pending || !abort.ready} />}>
              Abort errand
            </AlertDialogTrigger>
            <AlertDialogContent>
              <AlertDialogHeader>
                <AlertDialogTitle>Abort this accepted errand?</AlertDialogTitle>
                <AlertDialogDescription>
                  If the errand has not expired, it will reopen for other couriers. If it has expired, the abort is final.
                </AlertDialogDescription>
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel>Keep errand</AlertDialogCancel>
                <AlertDialogAction
                  variant="destructive"
                  disabled={busy}
                  onClick={() => void perform("cancel-accepted", "Errand aborted")}
                >
                  Yes, abort errand
                </AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>
        )}
        <OrderCommandNotice {...abort} />
      </>
    )
  }

  if (mode === "requester" && order.status === "DELIVERED") {
    return <Button size="sm" onClick={() => void perform("complete", "Order completed")} disabled={busy}>{busy ? "Saving…" : "Confirm completion"}</Button>
  }

  if (mode === "requester" && order.status === "OPEN") {
    return <Button size="sm" variant="outline" onClick={() => void perform("cancel", "Order cancelled")} disabled={busy}>{busy ? "Saving…" : "Cancel order"}</Button>
  }

  return null
}
