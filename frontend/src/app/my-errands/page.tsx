"use client"

import { useMemo } from "react"

import { OrderActions } from "@/components/orders/order-actions"
import { OrderCard } from "@/components/orders/order-card"
import { RequireAuth } from "@/components/require-auth"
import { useAuth } from "@/components/providers/auth-provider"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Skeleton } from "@/components/ui/skeleton"
import { useOrderList } from "@/hooks/use-order-list"
import { useSupplierNames } from "@/hooks/use-supplier-names"
import { orderMinePath, updateCourierOrderList, type Order } from "@/lib/orders"

export default function MyErrandsPage() {
  const { user } = useAuth()
  const { orders, loading, error, refresh: load, updateOrders } = useOrderList(user ? orderMinePath("courier", user.uid) : null)
  const supplierIds = useMemo(() => orders.flatMap((order) => [order.pickupSupplierId, order.deliverySupplierId]), [orders])
  const { names: supplierNames, ready: supplierNamesReady } = useSupplierNames(supplierIds)

  function replace(updated: Order) {
    updateOrders((current) => updateCourierOrderList(current, updated))
    void load()
  }

  return (
    <RequireAuth>
      <div className="mx-auto flex w-full max-w-6xl flex-col gap-6 px-4 py-8">
        <div className="flex flex-wrap items-end justify-between gap-3"><div><p className="text-sm text-muted-foreground">Delivery work</p><h1 className="text-2xl font-semibold tracking-tight">My errands</h1><p className="mt-1 text-muted-foreground">Move accepted orders through pickup and delivery.</p></div><Button variant="outline" size="sm" onClick={() => void load()}>Refresh</Button></div>
        {error && <p role="status" className="text-xs text-muted-foreground">Could not refresh your errands. We will try again.</p>}
        {loading || !supplierNamesReady ? <div className="grid gap-4 md:grid-cols-2"><Skeleton className="h-52" /><Skeleton className="h-52" /></div> : orders.length === 0 ? <Card><CardContent className="p-8 text-center text-muted-foreground">You have not accepted an errand yet.</CardContent></Card> : <div className="grid gap-4 md:grid-cols-2">{orders.map((order) => <OrderCard key={order.attemptId ?? order.id} order={order} supplierNames={supplierNames} actions={<OrderActions order={order} mode="courier" onUpdated={replace} />} />)}</div>}
      </div>
    </RequireAuth>
  )
}
