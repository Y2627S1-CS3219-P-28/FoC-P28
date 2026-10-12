"use client"

import { useCallback, useState } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"

import { Badge } from "@/components/ui/badge"
import { Button, buttonVariants } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { useAdminApi } from "@/hooks/use-admin-api"
import { useAdminPage } from "@/hooks/use-admin-page"
import { ADMIN_ORDER_STATUSES, type AdminOrder, type AdminOrderQuery } from "@/lib/admin-api"
import { formatOrderStatus, type OrderStatus } from "@/lib/orders"
import { AdminDetails, AdminHeading, AdminLoadState, AdminPagination, CopyId, adminDate } from "./admin-ui"

function OrderDetails({ order }: { order: AdminOrder }) {
  const entries = Object.entries(order)
  const values = new Map(entries)
  const ordered = (keys: string[]): [string, unknown][] => keys.filter((key) => values.has(key)).map((key) => [key, values.get(key)])
  const people = ["requesterName", "requesterId", "courierName", "courierId"]
  const locations = ["pickupSupplierName", "pickupSupplierId", "deliverySupplierName", "deliverySupplierId"]
  const timing = ["status", "createdAt", "expiresAt", "deliveryTimeLimitMinutes", "deliveredAt", "completedAt", "overdue", "overdueAt"]
  const information = ["id", "attemptId", "version", "offeredCredits", "itemDescription"]
  const groups: { title: string; fields: [string, unknown][] }[] = [
    { title: "Order information", fields: ordered(information) },
    { title: "People", fields: ordered(people) },
    { title: "Locations", fields: entries.filter(([key]) => locations.includes(key)) },
    { title: "Status and timing", fields: entries.filter(([key]) => timing.includes(key)) },
    { title: "Repost information", fields: entries.filter(([key]) => key.toLowerCase().includes("repost") || key === "originalOrderId") },
  ]
  const grouped = new Set(groups.flatMap((group) => group.fields.map(([key]) => key)))
  const other = entries.filter(([key]) => !grouped.has(key))
  if (other.length) groups.push({ title: "Other information", fields: other })
  function field([key, value]: [string, unknown]): [string, React.ReactNode] {
    const label = key.replace(/([A-Z])/g, " $1").replace(/^./, (letter) => letter.toUpperCase())
    const nameId = ({ requesterName: order.requesterId, courierName: order.courierId,
      pickupSupplierName: order.pickupSupplierId, deliverySupplierName: order.deliverySupplierId } as Record<string, string | null>)[key]
    if (key.endsWith("Name") && (people.includes(key) || locations.includes(key))) {
      return [label, <RecordLink key={key} kind={people.includes(key) ? "users" : "suppliers"} id={nameId} name={typeof value === "string" ? value : null} />]
    }
    const display = value == null ? "Not provided" : typeof value === "boolean" ? (value ? "Yes" : "No")
      : key.endsWith("At") && typeof value === "string" ? adminDate(value)
        : typeof value === "object" ? JSON.stringify(value) : String(value)
    return [label, key.endsWith("Id") || key === "id"
      ? <CopyId key={key} id={value as string | null} /> : display]
  }
  return <div className="grid gap-6 lg:grid-cols-2">
    {groups.map((group) => <Card key={group.title} className={group.title === "Repost information" ? "lg:col-span-2" : undefined}>
      <CardHeader><CardTitle><h2 className="text-xl">{group.title}</h2></CardTitle></CardHeader>
      <CardContent>{group.fields.length ? <AdminDetails fields={group.fields.map(field)} /> : <p className="text-sm text-muted-foreground">No information provided.</p>}</CardContent>
    </Card>)}
  </div>
}

function RecordLink({ id, name, kind }: { id: string | null; name: string | null; kind: "users" | "suppliers" }) {
  if (!id) return <span className="text-muted-foreground">Unassigned</span>
  return <Link href={"/admin/" + kind + "/" + encodeURIComponent(id)} className="font-medium underline-offset-4 hover:underline">
    {name || id}
  </Link>
}

function AdminOrderDetails({ orderId, onBack }: { orderId: string; onBack: () => void }) {
  const api = useAdminApi()
  const load = useCallback((signal: AbortSignal) => api.order(orderId, signal), [api, orderId])
  const { data, loading, error, refresh } = useAdminPage(load)
  return <section className="mx-auto w-full max-w-6xl space-y-6 px-4 py-8 sm:px-6 lg:py-10">
    <Button variant="outline" onClick={onBack}>Back to orders</Button>
    <AdminHeading title="Order details" description="Review all available information for the selected order." />
    <AdminLoadState loading={loading} error={error} empty={false} retry={refresh} />
    {data && <OrderDetails order={data} />}
  </section>
}

export function AdminOrders({ initialOrderId }: { initialOrderId?: string }) {
  const router = useRouter()
  return initialOrderId
    ? <AdminOrderDetails key={initialOrderId} orderId={initialOrderId} onBack={() => router.push("/admin/orders")} />
    : <AdminOrderList />
}

function AdminOrderList() {
  const api = useAdminApi()
  const [query, setQuery] = useState<AdminOrderQuery>({ page: 1, status: null, orderId: "" })
  const [orderId, setOrderId] = useState("")
  const load = useCallback((signal: AbortSignal) => api.orders(query, signal), [api, query])
  const { data, loading, error, refresh } = useAdminPage(load)
  const statuses = { all: "All statuses", ...Object.fromEntries(ADMIN_ORDER_STATUSES.map((status) => [status, formatOrderStatus(status)])) }

  return <section className="mx-auto w-full max-w-7xl space-y-6 p-4 md:p-8">
    <AdminHeading title="Orders" description="View every order, across all statuses. Latest orders appear first." />
    <div className="flex flex-wrap items-end gap-4">
      <div className="flex flex-col gap-2"><Label>Status</Label>
        <Select items={statuses} value={query.status ?? "all"} onValueChange={(value) =>
          setQuery((current) => ({ ...current, page: 1, status: !value || value === "all" ? null : value as OrderStatus }))}>
          <SelectTrigger aria-label="Order status" className="min-w-40 data-[size=default]:h-10"><SelectValue /></SelectTrigger>
          <SelectContent>{Object.entries(statuses).map(([value, label]) =>
            <SelectItem key={value} value={value}>{label}</SelectItem>)}</SelectContent>
        </Select>
      </div>
      <form className="flex flex-1 flex-wrap items-end gap-2" onSubmit={(event) => {
        event.preventDefault(); setQuery((current) => ({ ...current, page: 1, orderId: orderId.trim() }))
      }}>
        <div className="flex min-w-40 flex-1 flex-col gap-2"><Label htmlFor="admin-order-id">Order ID</Label>
          <Input className="h-10" id="admin-order-id" value={orderId} onChange={(event) => setOrderId(event.target.value)} placeholder="Enter an exact Order ID" />
        </div>
        <Button className="h-10" type="submit">Search</Button>
        <Button className="h-10" type="button" variant="outline" onClick={() => {
          setOrderId(""); setQuery({ page: 1, status: null, orderId: "" })
        }}>Clear</Button>
      </form>
    </div>
    <AdminLoadState loading={loading} error={error} empty={data?.items.length === 0} retry={refresh} />
    {data && data.items.length > 0 && <>
      <div className="min-w-0 overflow-x-auto rounded-xl border" tabIndex={0} aria-label="Orders table, scroll horizontally for more columns">
        <Table className="min-w-[1400px]">
          <TableHeader><TableRow>{["Order ID", "Status", "Requester", "Courier", "Pickup", "Delivery", "Credits", "Created", "Expires", ""].map((title, index) =>
            <TableHead key={index}>{title || <span className="sr-only">Details</span>}</TableHead>)}</TableRow></TableHeader>
          <TableBody>{data.items.map((order) => <TableRow key={order.id}>
            <TableCell className="max-w-64 whitespace-normal"><CopyId id={order.id} /></TableCell>
            <TableCell><Badge variant="outline">{formatOrderStatus(order.status)}</Badge></TableCell>
            <TableCell><RecordLink kind="users" id={order.requesterId} name={order.requesterName} /></TableCell>
            <TableCell><RecordLink kind="users" id={order.courierId} name={order.courierName} /></TableCell>
            <TableCell><RecordLink kind="suppliers" id={order.pickupSupplierId} name={order.pickupSupplierName} /></TableCell>
            <TableCell><RecordLink kind="suppliers" id={order.deliverySupplierId} name={order.deliverySupplierName} /></TableCell>
            <TableCell>{order.offeredCredits}</TableCell>
            <TableCell className="text-xs">{adminDate(order.createdAt)}</TableCell>
            <TableCell className="text-xs">{adminDate(order.expiresAt)}</TableCell>
            <TableCell><Link className={buttonVariants({ size: "sm", variant: "outline" })}
              href={"/admin/orders/" + encodeURIComponent(order.id)} aria-label={"View order " + order.id}>View</Link></TableCell>
          </TableRow>)}</TableBody>
        </Table>
      </div>
      <AdminPagination {...data} onPage={(page) => setQuery((current) => ({ ...current, page }))} />
    </>}
  </section>
}
