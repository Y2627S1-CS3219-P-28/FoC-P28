"use client"
import { useRef, useState, type FormEvent } from "react"
import { useRouter } from "next/navigation"
import { useApi } from "@/hooks/use-api"
import { type Errand, type Supplier, orderPath } from "@/lib/orders"
import { OrderLayout, ErrorMessage, QueryState, useOrderQuery } from "./order-shared"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"

export function PostRequest() {
  const api = useApi()
  const router = useRouter()
  const suppliers = useOrderQuery<Supplier[]>(orderPath + "/prototype/suppliers")
  const [pickup, setPickup] = useState<string | null>(null)
  const [delivery, setDelivery] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<unknown>(null)
  const pending = useRef<{ fingerprint: string; commandId: string } | null>(null)
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (busy) return
    const form = new FormData(event.currentTarget)
    const expiry = String(form.get("expiry"))
    const payload = {
      requesterId: String(form.get("requesterId")).trim(), description: String(form.get("description")).trim(),
      pickupSupplierId: pickup, deliverySupplierId: delivery,
      creditAmount: Number(form.get("creditAmount")), deliveryDurationMinutes: Number(form.get("duration")),
      expiresAt: new Date(expiry + ":00+08:00").toISOString(),
    }
    const fingerprint = JSON.stringify(payload)
    if (pending.current?.fingerprint !== fingerprint) pending.current = { fingerprint, commandId: crypto.randomUUID() }
    setBusy(true); setError(null)
    try {
      const result = await api<Errand>(orderPath + "/errands", { method: "POST", body: { ...payload, commandId: pending.current.commandId } })
      router.push("/requests/" + encodeURIComponent(result.id))
    } catch (cause) { setError(cause); setBusy(false) }
  }
  return <OrderLayout title="Post a request" description="Tell a campus courier what to collect and where to bring it.">
    <QueryState {...suppliers} />
    <form onSubmit={submit} className="max-w-2xl space-y-5">
      <fieldset disabled={busy} className="space-y-5">
        <div className="space-y-2"><Label htmlFor="requesterId">Demo requester ID</Label>
          <Input id="requesterId" name="requesterId" defaultValue="demo-requester" required maxLength={128} /></div>
        <div className="space-y-2"><Label htmlFor="description">What do you need?</Label>
          <Textarea id="description" name="description" placeholder="Collect my lunch from the campus canteen" required minLength={10} maxLength={100} />
          <p className="text-xs text-muted-foreground">10–100 characters.</p></div>
        <div className="grid gap-5 sm:grid-cols-2">
          {[{ id: "pickup", label: "Pickup", value: pickup, change: setPickup }, { id: "delivery", label: "Delivery", value: delivery, change: setDelivery }].map(field =>
            <div className="space-y-2" key={field.id}><Label htmlFor={field.id}>{field.label}</Label>
              <Select value={field.value} onValueChange={field.change}>
                <SelectTrigger id={field.id} className="w-full"><SelectValue placeholder="Choose a sample location" /></SelectTrigger>
                <SelectContent>{suppliers.data?.map(s => <SelectItem key={s.id} value={s.id}>{s.name}</SelectItem>)}</SelectContent>
              </Select>
            </div>)}
        </div>
        <div className="grid gap-5 sm:grid-cols-2">
          <div className="space-y-2"><Label htmlFor="creditAmount">Reward (credits)</Label><Input id="creditAmount" name="creditAmount" type="number" min={1} step={1} defaultValue={5} required /></div>
          <div className="space-y-2"><Label htmlFor="duration">Delivery duration (minutes)</Label><Input id="duration" name="duration" type="number" min={15} max={1440} step={1} defaultValue={30} required /></div>
        </div>
        <div className="space-y-2"><Label htmlFor="expiry">Accept before (Singapore time)</Label>
          <Input id="expiry" name="expiry" type="datetime-local" required />
          <p className="text-xs text-muted-foreground">At least 30 minutes from now. Delivery duration begins when the courier starts.</p></div>
      </fieldset>
      <ErrorMessage error={error} />
      <Button type="submit" size="lg" disabled={busy || !pickup || !delivery || suppliers.loading || !!suppliers.error}>
        {busy ? "Posting…" : "Post request"}
      </Button>
    </form>
  </OrderLayout>
}
