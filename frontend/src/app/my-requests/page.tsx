"use client"

import { useCallback, useEffect, useMemo, useState } from "react"
import { toast } from "sonner"

import { OrderActions } from "@/components/orders/order-actions"
import { OrderCard } from "@/components/orders/order-card"
import { RepostControls } from "@/components/orders/repost-controls"
import { RequireAuth } from "@/components/require-auth"
import { useAuth } from "@/components/providers/auth-provider"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { useApi } from "@/hooks/use-api"
import { useSupplierNames } from "@/hooks/use-supplier-names"
import { orderMinePath, type Order, type OrderPage } from "@/lib/orders"

export default function MyRequestsPage() {
  const api = useApi()
  const { user } = useAuth()
  const [orders, setOrders] = useState<Order[]>([])
  const [loading, setLoading] = useState(true)
  const supplierIds = useMemo(() => orders.flatMap((order) => [order.pickupSupplierId, order.deliverySupplierId]), [orders])
  const { names: supplierNames, ready: supplierNamesReady } = useSupplierNames(supplierIds)

  const load = useCallback(async () => {
    if (!user) return
    setLoading(true)
    try {
      const page = await api<OrderPage>(orderMinePath("requester", user.uid))
      setOrders(page.items)
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Could not load your requests.")
    } finally {
      setLoading(false)
    }
  }, [api, user])

  // Fetching remote Order state is an external synchronization step.
  // eslint-disable-next-line react-hooks/set-state-in-effect
  useEffect(() => { void load() }, [load])

  function replace(updated: Order) { setOrders((current) => current.map((order) => order.id === updated.id ? updated : order)) }

  return (
    <RequireAuth>
      <div className="mx-auto flex w-full max-w-6xl flex-col gap-6 px-4 py-8">
        <div className="flex flex-wrap items-end justify-between gap-3"><div><p className="text-sm text-muted-foreground">Requests you posted</p><h1 className="text-2xl font-semibold tracking-tight">My requests</h1><p className="mt-1 text-muted-foreground">Track your orders, outcomes, and repost settings.</p></div><Button variant="outline" size="sm" onClick={() => void load()}>Refresh</Button></div>
        {loading || !supplierNamesReady ? <div className="grid gap-4 md:grid-cols-2"><Skeleton className="h-52" /><Skeleton className="h-52" /></div> : orders.length === 0 ? <Card><CardContent className="p-8 text-center text-muted-foreground">You have not posted a request yet.</CardContent></Card> : <div className="space-y-4">{orders.map((order) => <OrderCard key={order.id} order={order} supplierNames={supplierNames} actions={<OrderActions order={order} mode="requester" onUpdated={replace} />} footer={<RepostControls order={order} onUpdated={replace} />} />)}</div>}
      </div>
    </RequireAuth>
  )
}
