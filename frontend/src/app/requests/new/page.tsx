"use client"

import type { FormEvent } from "react"
import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { toast } from "sonner"

import { RequireAuth } from "@/components/require-auth"
import { useAuth } from "@/components/providers/auth-provider"
import { useOrderMode } from "@/components/providers/order-mode-provider"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { useApi } from "@/hooks/use-api"
import { buildCreateOrderPayload, buildRepostConfigPayload, type CreateOrderForm, type Order } from "@/lib/orders"
import { supplierOptionLabel, type Page, type Supplier } from "@/lib/suppliers"

const inputClass = "h-9 rounded-lg border bg-transparent px-3 text-sm"
const localDate = (hoursFromNow: number) => {
  const date = new Date(Date.now() + hoursFromNow * 60 * 60_000)
  const offset = date.getTimezoneOffset() * 60_000
  return new Date(date.getTime() - offset).toISOString().slice(0, 16)
}

const initialForm: CreateOrderForm = {
  itemDescription: "",
  pickupSupplierId: "",
  deliverySupplierId: "",
  offeredCredits: 1,
  deliveryTimeLimitMinutes: 15,
  expiresAt: localDate(1),
  automaticRepost: false,
  repostDueAt: localDate(2),
  repostCreditAmount: 1,
  repostDeliveryDurationMinutes: 15,
}

export default function NewRequestPage() {
  const api = useApi()
  const router = useRouter()
  const { user } = useAuth()
  const { mode } = useOrderMode()
  const [form, setForm] = useState(initialForm)
  const [busy, setBusy] = useState(false)
  const [suppliers, setSuppliers] = useState<Supplier[]>([])
  const [suppliersLoading, setSuppliersLoading] = useState(true)
  const [suppliersError, setSuppliersError] = useState<string | null>(null)

  useEffect(() => {
    if (!user || mode !== "requester") {
      setSuppliersLoading(false)
      return
    }

    const controller = new AbortController()
    setSuppliersLoading(true)
    setSuppliersError(null)
    void api<Page<Supplier>>("/api/suppliers?status=active&page=1&size=100&sort=name&order=asc", {
      signal: controller.signal,
    })
      .then((page) => setSuppliers(page.items))
      .catch((error) => {
        if (error instanceof DOMException && error.name === "AbortError") return
        setSuppliersError(error instanceof Error ? error.message : "Could not load suppliers.")
      })
      .finally(() => setSuppliersLoading(false))
    return () => controller.abort()
  }, [api, mode, user])

  function update<K extends keyof CreateOrderForm>(key: K, value: CreateOrderForm[K]) {
    setForm((current) => ({ ...current, [key]: value }))
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!user) return
    setBusy(true)
    try {
      const created = await api<Order>("/api/orders", { method: "POST", body: buildCreateOrderPayload(form, user.uid) })
      if (form.automaticRepost) {
        await api<Order>(`/api/orders/${created.id}/repost/configure`, {
          method: "POST",
          body: buildRepostConfigPayload(form, user.uid, created.version),
        })
      }
      toast.success("Order posted")
      router.push("/my-requests")
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Could not post this request.")
    } finally {
      setBusy(false)
    }
  }

  return (
    <RequireAuth>
      <div className="mx-auto flex w-full max-w-3xl flex-col gap-6 px-4 py-8">
        <div><p className="text-sm text-muted-foreground">Requester mode</p><h1 className="text-2xl font-semibold tracking-tight">Post a campus request</h1><p className="mt-1 text-muted-foreground">Describe the errand and reserve credits before it becomes available.</p></div>
        {mode !== "requester" && <Card className="border-dashed"><CardContent className="p-4 text-sm text-muted-foreground">Switch to Requester mode from the sidebar to post a request.</CardContent></Card>}
        <form onSubmit={(event) => void submit(event)} className="space-y-6" aria-label="Post request form">
          <Card><CardHeader><CardTitle>Request details</CardTitle><CardDescription>Select suppliers from the active Supplier Service catalogue.</CardDescription></CardHeader><CardContent className="grid gap-4 sm:grid-cols-2">
            <label className="space-y-1 text-sm sm:col-span-2"><Label htmlFor="item-description">What do you need?</Label><Input id="item-description" required maxLength={100} className={inputClass} value={form.itemDescription} onChange={(event) => update("itemDescription", event.target.value)} placeholder="Pick up a parcel from the campus store" /></label>
            {suppliersError && <Alert variant="destructive" className="sm:col-span-2"><AlertTitle>Suppliers unavailable</AlertTitle><AlertDescription>{suppliersError} Refresh and try again.</AlertDescription></Alert>}
            <label className="space-y-1 text-sm"><Label htmlFor="pickup-supplier">Pickup supplier</Label><Select items={Object.fromEntries(suppliers.map((supplier) => [supplier.id, supplierOptionLabel(supplier)]))} value={form.pickupSupplierId} onValueChange={(value) => update("pickupSupplierId", String(value ?? ""))} disabled={suppliersLoading || suppliers.length === 0}><SelectTrigger id="pickup-supplier" aria-label="Pickup supplier" className="h-9 w-full"><SelectValue placeholder={suppliersLoading ? "Loading suppliers…" : "Select a pickup supplier"} /></SelectTrigger><SelectContent>{suppliers.map((supplier) => <SelectItem key={supplier.id} value={supplier.id}>{supplierOptionLabel(supplier)}</SelectItem>)}</SelectContent></Select></label>
            <label className="space-y-1 text-sm"><Label htmlFor="delivery-supplier">Delivery supplier</Label><Select items={Object.fromEntries(suppliers.map((supplier) => [supplier.id, supplierOptionLabel(supplier)]))} value={form.deliverySupplierId} onValueChange={(value) => update("deliverySupplierId", String(value ?? ""))} disabled={suppliersLoading || suppliers.length === 0}><SelectTrigger id="delivery-supplier" aria-label="Delivery supplier" className="h-9 w-full"><SelectValue placeholder={suppliersLoading ? "Loading suppliers…" : "Select a delivery supplier"} /></SelectTrigger><SelectContent>{suppliers.map((supplier) => <SelectItem key={supplier.id} value={supplier.id}>{supplierOptionLabel(supplier)}</SelectItem>)}</SelectContent></Select></label>
            <label className="space-y-1 text-sm"><Label htmlFor="credits">Offered credits</Label><Input id="credits" required type="number" min="1" className={inputClass} value={form.offeredCredits} onChange={(event) => update("offeredCredits", Number(event.target.value))} /></label>
            <label className="space-y-1 text-sm"><Label htmlFor="delivery-limit">Delivery time limit (minutes)</Label><Input id="delivery-limit" required type="number" min="15" className={inputClass} value={form.deliveryTimeLimitMinutes} onChange={(event) => update("deliveryTimeLimitMinutes", Number(event.target.value))} /></label>
            <label className="space-y-1 text-sm sm:col-span-2"><Label htmlFor="expires-at">Order expiry</Label><Input id="expires-at" required type="datetime-local" className={inputClass} value={form.expiresAt} onChange={(event) => update("expiresAt", event.target.value)} /></label>
          </CardContent></Card>
          <Card><CardHeader><CardTitle>Automatic repost</CardTitle><CardDescription>Optional NTH4 behavior. The new order is created and credited only when the repost is due.</CardDescription></CardHeader><CardContent className="space-y-4">
            <label className="flex items-center gap-2 text-sm font-medium"><input type="checkbox" checked={form.automaticRepost} onChange={(event) => update("automaticRepost", event.target.checked)} />Enable automatic repost if no courier accepts</label>
            <div className="grid gap-4 sm:grid-cols-3">
              <label className="space-y-1 text-sm"><Label htmlFor="repost-due">Repost time</Label><Input id="repost-due" type="datetime-local" className={inputClass} value={form.repostDueAt} onChange={(event) => update("repostDueAt", event.target.value)} /></label>
              <label className="space-y-1 text-sm"><Label htmlFor="repost-credits">Repost credits</Label><Input id="repost-credits" type="number" min="1" className={inputClass} value={form.repostCreditAmount} onChange={(event) => update("repostCreditAmount", Number(event.target.value))} /></label>
              <label className="space-y-1 text-sm"><Label htmlFor="repost-duration">Repost delivery minutes</Label><Input id="repost-duration" type="number" min="15" className={inputClass} value={form.repostDeliveryDurationMinutes} onChange={(event) => update("repostDeliveryDurationMinutes", Number(event.target.value))} /></label>
            </div>
          </CardContent></Card>
          <Button type="submit" disabled={busy || mode !== "requester" || suppliersLoading || suppliers.length === 0 || !form.pickupSupplierId || !form.deliverySupplierId}>{busy ? "Posting…" : "Post request"}</Button>
        </form>
      </div>
    </RequireAuth>
  )
}
