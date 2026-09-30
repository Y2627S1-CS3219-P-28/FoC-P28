"use client"

import { useCallback, useEffect, useState } from "react"
import { toast } from "sonner"

import { OrderCard } from "@/components/orders/order-card"
import { RequireAuth } from "@/components/require-auth"
import { useAuth } from "@/components/providers/auth-provider"
import { useOrderMode } from "@/components/providers/order-mode-provider"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { useApi } from "@/hooks/use-api"
import { buildOrderActionPayload, orderAvailablePath, type Order, type OrderPage } from "@/lib/orders"

export default function ErrandsPage() {
  const api = useApi()
  const { user } = useAuth()
  const { mode } = useOrderMode()
  const [orders, setOrders] = useState<Order[]>([])
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const page = await api<OrderPage>(orderAvailablePath())
      setOrders(page.content)
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Could not load available errands.")
    } finally {
      setLoading(false)
    }
  }, [api])

  // Fetching remote Order state is an external synchronization step.
  // eslint-disable-next-line react-hooks/set-state-in-effect
  useEffect(() => { void load() }, [load])

  async function accept(order: Order) {
    if (!user) return
    try {
      const updated = await api<Order>(`/api/orders/${order.id}/accept`, { method: "POST", body: buildOrderActionPayload(user.uid, order.version) })
      setOrders((current) => current.filter((item) => item.id !== updated.id))
      toast.success("Errand accepted")
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Could not accept this errand.")
    }
  }

  return (
    <RequireAuth>
      <div className="mx-auto flex w-full max-w-6xl flex-col gap-6 px-4 py-8">
        <div><p className="text-sm text-muted-foreground">Courier mode</p><h1 className="text-2xl font-semibold tracking-tight">Browse errands</h1><p className="mt-1 text-muted-foreground">Accept an open request, then manage it from My Errands.</p></div>
        {mode !== "courier" && <Card className="border-dashed"><CardContent className="p-4 text-sm text-muted-foreground">Switch to Courier mode from the sidebar to accept errands.</CardContent></Card>}
        {loading ? <div className="grid gap-4 md:grid-cols-2"><Skeleton className="h-52" /><Skeleton className="h-52" /></div> : orders.length === 0 ? <Card><CardContent className="p-8 text-center text-muted-foreground">No open errands are available right now.</CardContent></Card> : <div className="grid gap-4 md:grid-cols-2">{orders.map((order) => <OrderCard key={order.id} order={order} actions={<Button size="sm" onClick={() => void accept(order)} disabled={mode !== "courier"}>Accept errand</Button>} />)}</div>}
      </div>
    </RequireAuth>
  )
}
